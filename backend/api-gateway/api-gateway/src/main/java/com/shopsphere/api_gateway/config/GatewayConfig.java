package com.shopsphere.api_gateway.config;

import com.shopsphere.api_gateway.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final ProxyService proxyService;

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return RouterFunctions.route()
                .route(req -> req.path().startsWith("/api/"), req -> proxyService.proxy(req))
                .build();
    }
}
