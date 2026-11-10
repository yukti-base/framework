package org.yuktisetu.core.recovery;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds new requests back until the service has picked up what a crash left
 * behind (decided 2026-11-01: "the server should acknowledge the ongoing tasks
 * first after startup").
 *
 * Tomcat accepts connections before Spring's ApplicationRunners run, so the
 * runner alone cannot keep traffic out. This filter answers 503 with Retry-After
 * until every StartupRecovery bean has run; the portals retry on 503, so a user
 * mid-drive sees a short pause rather than an error. Health checks and CORS
 * preflights always pass.
 *
 * In-flight requests at the moment of a crash need no replay: each one was a
 * single database transaction, so it either committed (and recovery sees it) or
 * rolled back (and the client's retry starts it cleanly).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RecoveryGate extends OncePerRequestFilter implements ApplicationRunner {

    private final AtomicBoolean open = new AtomicBoolean(false);
    private final ObjectProvider<StartupRecovery> recoveries;

    public RecoveryGate(ObjectProvider<StartupRecovery> recoveries) {
        this.recoveries = recoveries;
    }

    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void run(ApplicationArguments args) {
        List<StartupRecovery> all = recoveries.orderedStream().toList();
        long started = System.currentTimeMillis();
        log.info("Startup recovery: {} step(s) before taking requests", all.size());
        for (StartupRecovery r : all) {
            long t = System.currentTimeMillis();
            try {
                int n = r.recover();
                if (n > 0) {
                    log.warn("Startup recovery [{}]: picked up {} item(s) left by the previous run ({} ms)",
                            r.name(), n, System.currentTimeMillis() - t);
                } else {
                    log.info("Startup recovery [{}]: nothing left behind ({} ms)", r.name(), System.currentTimeMillis() - t);
                }
            } catch (Exception e) {
                // Never keep the service shut over one failed step: the periodic
                // reconcilers retry the same work, and the log says what failed.
                log.error("Startup recovery [{}] failed; continuing so the service can serve: {}",
                        r.name(), e.getMessage(), e);
            }
        }
        open.set(true);
        log.info("Startup recovery complete in {} ms; accepting requests", System.currentTimeMillis() - started);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (open.get() || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "2");
        response.setContentType("application/json");
        String message = "The service is finishing work from before a restart. Please retry in a moment.";
        response.getWriter().write("{\"success\":false,\"status\":503,\"message\":\"" + message
                + "\",\"data\":null,\"error\":{\"code\":\"SERVICE_RECOVERING\",\"message\":\"" + message + "\"}}");
    }
}
