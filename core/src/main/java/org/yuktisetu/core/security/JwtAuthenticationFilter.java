package org.yuktisetu.core.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Not a @Component -- constructed explicitly and registered by each
 * service's own SecurityConfig (matches the "explicit over auto-config"
 * decision for this codebase; see core's design notes). Every service does:
 *
 *   http.addFilterBefore(new JwtAuthenticationFilter(tokenVerifier),
 *                         UsernamePasswordAuthenticationFilter.class);
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenVerifier tokenVerifier;

    public JwtAuthenticationFilter(JwtTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = tokenVerifier.verify(token);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roleClaims = claims.get("roles", List.class);

                List<UserPrincipal.RoleClaim> roles = roleClaims.stream()
                        .map(m -> new UserPrincipal.RoleClaim(
                                (String) m.get("role"),
                                // collegeId/deptId come back from the JSON
                                // round-trip as a Number (Integer or Long
                                // depending on magnitude), never a String --
                                // read them as Number, do not cast to String.
                                // (This was a real bug in an earlier
                                // per-service copy of this filter: casting
                                // straight to (String) threw
                                // ClassCastException for every non-null
                                // value, which the catch-all below swallowed,
                                // silently deauthenticating anyone whose role
                                // wasn't trust-wide.)
                                toLongOrNull(m.get("collegeId")),
                                toLongOrNull(m.get("deptId"))
                        ))
                        .toList();

                UserPrincipal principal = new UserPrincipal(
                        Long.parseLong(claims.getSubject()),
                        claims.get("email", String.class),
                        roles
                );

                List<GrantedAuthority> authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.role()))
                        .map(GrantedAuthority.class::cast)
                        .toList();

                var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                // Invalid/expired token, or malformed claims: leave the
                // SecurityContext empty. RestAuthenticationEntryPoint then
                // produces a consistent 401 JSON body for any endpoint that
                // actually requires authentication -- this filter itself
                // never writes a response.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private Long toLongOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = v.toString();
        return s.isBlank() ? null : Long.parseLong(s);
    }
}
