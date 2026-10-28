package org.yuktisetu.core.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The fleet's async executors, configured once in core so every service shares
 * the same sizing rather than each defaulting to Spring's
 * SimpleAsyncTaskExecutor -- which creates an unbounded new thread per call and
 * will happily exhaust the machine during a drive-day burst.
 *
 * Three named pools rather than one, because the workloads have genuinely
 * different shapes and must not starve each other. A single shared pool means a
 * 600-student eligibility run occupies every thread and the notification that
 * tells those students about the drive queues behind it.
 *
 * Sized for this deployment: ~5000 daily users on one on-premise box, bursting to
 * a couple of thousand concurrent students when a drive is announced. The pools
 * are small on purpose -- the bottleneck is Postgres connections (Hikari is
 * capped at 10 per service), so more threads than that just queue inside the
 * driver and make latency harder to reason about.
 */
@Slf4j
@Configuration
public class YuktiSetuAsyncConfig implements AsyncConfigurer {

    public static final String DEFAULT_EXECUTOR = "yuktiSetuTaskExecutor";
    public static final String HEAVY_EXECUTOR = "yuktiSetuHeavyTaskExecutor";
    public static final String NOTIFICATION_EXECUTOR = "yuktiSetuNotificationExecutor";

    /**
     * Short, frequent fire-and-forget work: audit rows, cache warming, status
     * bookkeeping the caller does not wait on.
     */
    @Bean(name = DEFAULT_EXECUTOR)
    public Executor yuktiSetuTaskExecutor() {
        return build("ys-async-", 4, 8, 500);
    }

    /**
     * Long batch work: eligibility resolution over a whole cohort, exports,
     * and later the composite-scoring run.
     *
     * Deliberately narrow. Two of these running concurrently already saturate a
     * 10-connection Hikari pool, and a third would sit blocked holding a thread
     * for nothing. The queue absorbs the rest.
     */
    @Bean(name = HEAVY_EXECUTOR)
    public Executor yuktiSetuHeavyTaskExecutor() {
        return build("ys-heavy-", 2, 4, 200);
    }

    /**
     * Notification fan-out. Separate so a slow SMTP handshake can never delay an
     * eligibility run, and vice versa.
     */
    @Bean(name = NOTIFICATION_EXECUTOR)
    public Executor yuktiSetuNotificationExecutor() {
        return build("ys-notify-", 2, 6, 1000);
    }

    /** What a bare @Async with no qualifier resolves to. */
    @Override
    public Executor getAsyncExecutor() {
        return yuktiSetuTaskExecutor();
    }

    /**
     * Without this, an exception thrown from an @Async void method is swallowed
     * silently -- there is no caller left to receive it. That is the single worst
     * failure mode of going async: a background eligibility run dies and the only
     * evidence is that nobody became eligible.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("Uncaught exception in @Async {}.{}: {}",
                        method.getDeclaringClass().getSimpleName(), method.getName(),
                        throwable.getMessage(), throwable);
    }

    private ThreadPoolTaskExecutor build(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        // CallerRuns rather than Abort: when the queue is full the submitting
        // thread does the work itself. That degrades into back-pressure instead of
        // throwing away a task, which for a notification or an audit row would
        // mean losing it outright.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Let in-flight background work finish on shutdown rather than killing a
        // half-written eligibility run mid-transaction.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(securityContextPropagatingDecorator());
        executor.initialize();
        // Logged because a mis-wired @EnableAsync is otherwise invisible: the
        // annotation silently becomes a no-op and every "background" call runs
        // inline on the request thread. Seeing these three lines at startup is the
        // proof that the shared pools are actually in use.
        log.info("Async executor ready: prefix={} core={} max={} queue={}", prefix, core, max, queue);
        return executor;
    }

    /**
     * Carries the caller's identity onto the worker thread.
     *
     * Three properties, each fixing a way this used to go wrong:
     *
     *  1. A COPY, taken at submit time. The old version handed the worker the very
     *     SecurityContext object the request thread owns, so the worker and the
     *     request shared mutable state, and anything the request did to it after
     *     submitting (including clearing it) was visible to a task that had not
     *     started yet. The copy is a fresh context holding the same Authentication,
     *     which is immutable for our purposes (a JWT principal).
     *  2. The worker's previous context is RESTORED, not wiped. A rejected task
     *     under CallerRunsPolicy runs on the SUBMITTING thread; the old finally
     *     block cleared the context there, logging the request's own user out
     *     halfway through the request that was in progress.
     *  3. Nothing leaks between tasks on a pooled thread: if there was no context
     *     before the task, there is none after it.
     */
    private TaskDecorator securityContextPropagatingDecorator() {
        return runnable -> {
            SecurityContext captured = SecurityContextHolder.createEmptyContext();
            captured.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
            return () -> {
                SecurityContext previous = SecurityContextHolder.getContext();
                boolean hadPrevious = previous.getAuthentication() != null;
                try {
                    SecurityContextHolder.setContext(captured);
                    runnable.run();
                } finally {
                    if (hadPrevious) {
                        SecurityContextHolder.setContext(previous);
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                }
            };
        };
    }
}
