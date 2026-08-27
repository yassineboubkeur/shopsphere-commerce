package com.shopsphere.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyService {

    private final WebClient webClient;

    @Value("${gateway.retry.max-attempts:2}")
    private int maxRetries;

    @Value("${gateway.retry.delay-seconds:1}")
    private long retryDelaySeconds;

    private static final Map<String, String> SERVICE_MAP = Map.of(
            "/api/auth", "http://localhost:8081",
            "/api/products", "http://localhost:8082",
            "/api/orders", "http://localhost:8083",
            "/api/cart", "http://localhost:8083",
            "/api/payments", "http://localhost:8084",
            "/api/inventory", "http://localhost:8085",
            "/api/notifications", "http://localhost:8086",
            "/api/analytics", "http://localhost:8087"
    );

    private static final Map<String, String> FALLBACK_MAP = Map.of(
            "/api/products", "{\"message\":\"Product service unavailable. Fallback response.\"}",
            "/api/orders", "{\"message\":\"Order service unavailable. Fallback response.\"}",
            "/api/cart", "{\"message\":\"Cart service unavailable. Fallback response.\"}",
            "/api/payments", "{\"message\":\"Payment service unavailable. Fallback response.\"}",
            "/api/inventory", "{\"message\":\"Inventory service unavailable. Fallback response.\"}",
            "/api/notifications", "{\"message\":\"Notification service unavailable. Fallback response.\"}",
            "/api/analytics", "{\"message\":\"Analytics service unavailable. Fallback response.\"}",
            "/api/auth", "{\"message\":\"Auth service unavailable. Fallback response.\"}"
    );

    public Mono<ServerResponse> proxy(ServerRequest request) {
        String path = request.path();
        String serviceUrl = getServiceUrl(path);

        if (serviceUrl == null) {
            return ServerResponse.notFound().build();
        }

        String query = request.uri().getRawQuery();
        String targetUrl = (query != null) ? serviceUrl + path + "?" + query : serviceUrl + path;
        HttpMethod method = HttpMethod.valueOf(request.method().name());

        log.info("Proxying: {} {} → {}", request.method(), path, targetUrl);

        HashMap<String, String> forwardHeaders = new HashMap<>();
        request.exchange().getRequest().getHeaders().forEach((key, values) -> {
            if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) {
                forwardHeaders.put(key, String.join(", ", values));
            }
        });

        return request.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> webClient
                        .method(method)
                        .uri(targetUrl)
                        .headers(h -> forwardHeaders.forEach(h::add))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchangeToMono(response -> response.bodyToMono(String.class)))
                .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(retryDelaySeconds))
                        .filter(throwable -> shouldRetry(throwable, method)))
                .map(responseBody -> {
                    log.info("Response from {}: {}", targetUrl, responseBody);
                    return responseBody;
                })
                .flatMap(responseBody -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responseBody))
                .onErrorResume(e -> {
                    log.error("Proxy error for {}: {}", targetUrl, e.getMessage());
                    String fallback = getFallbackResponse(path);
                    if (fallback != null && method == HttpMethod.GET) {
                        log.warn("Returning fallback for {}", path);
                        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(fallback);
                    }
                    return ServerResponse.status(HttpStatus.BAD_GATEWAY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "Bad Gateway", "message", e.getMessage()));
                });
    }

    private String getFallbackResponse(String path) {
        for (Map.Entry<String, String> entry : FALLBACK_MAP.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getServiceUrl(String path) {
        for (Map.Entry<String, String> entry : SERVICE_MAP.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean shouldRetry(Throwable throwable, HttpMethod method) {
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH
                || method == HttpMethod.DELETE) {
            return false;
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
