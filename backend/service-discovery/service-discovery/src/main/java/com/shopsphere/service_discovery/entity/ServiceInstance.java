package com.shopsphere.service_discovery.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance {

    private String name;
    private String host;
    private int port;
    private LocalDateTime registeredAt;
    private LocalDateTime lastHeartbeat;

    public String getStatus() {
        return "UP";
    }

    public String getUrl() {
        return "http://" + host + ":" + port;
    }
}