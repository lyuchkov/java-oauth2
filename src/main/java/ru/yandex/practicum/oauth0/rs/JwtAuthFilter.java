package ru.yandex.practicum.oauth0.rs;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final RsProperties rsProperties;

    public JwtAuthFilter(JwtDecoder jwtDecoder, RsProperties rsProperties) {
        this.jwtDecoder = jwtDecoder;
        this.rsProperties = rsProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtDecoder.decodeAndVerify(token);

            if (!rsProperties.getAud().equals(claims.getAudience())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid audience (aud)");
                return;
            }

            String jti = claims.getId();
            if (isTokenRevoked(jti)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked or replayed");
                return;
            }

            String scopeClaim = claims.get("scope", String.class);
            List<GrantedAuthority> authorities = Arrays.stream(scopeClaim != null ? scopeClaim.split(" ") : new String[0])
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenRevoked(String jti) {
        // заглушка
        return false;
    }
}