package com.shopsphere.api_gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "eureka")
public class EurekaProperties {

    private String url;
    private boolean enabled = true;
    private String host = "localhost";
    private String name;
    private int port;
}