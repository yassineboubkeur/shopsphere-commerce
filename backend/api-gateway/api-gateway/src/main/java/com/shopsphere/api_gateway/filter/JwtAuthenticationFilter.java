package com.shopsphere.api_gateway.filter;

import com.shopsphere.api_gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    private static final String[] PUBLIC_PREFIXES = {
            "/api/auth/",
            "/api/products/"
    };

    private static final String[] PROTECTED_PREFIXES = {
            "/api/orders/",
            "/api/payments/",
            "/api/cart/",
            "/api/inventory/",
            "/api/notifications/",
            "/api/analytics/",
            "/api/user/",
            "/api/admin/"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            if (exchange.getRequest().getHeaders().getOrigin() != null) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.NO_CONTENT);
                response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
                response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type");
                response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, PATCH, OPTIONS");
                response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                return response.setComplete();
            }
            return chain.filter(exchange);
        }

        if (isPublic(path) || !isProtected(path)) {
            return chain.filter(withOptionalIdentity(exchange));
        }

        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Missing Authorization header for protected path {}", path);
            return unauthorized(exchange);
        }

        String token = authorization.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid JWT token for protected path {}", path);
            return unauthorized(exchange);
        }

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean matchesAny(String path, String[] prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublic(String path) {
        return matchesAny(path, PUBLIC_PREFIXES);
    }

    private ServerWebExchange withOptionalIdentity(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return exchange;
        }
        String token = authorization.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return exchange;
        }
        try {
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();
            return exchange.mutate().request(mutatedRequest).build();
        } catch (Exception e) {
            return exchange;
        }
    }

    private boolean isProtected(String path) {
        return matchesAny(path, PROTECTED_PREFIXES);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"error\":\"Unauthorized\",\"message\":\"Valid JWT token required\"}"
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}