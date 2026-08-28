package com.shopsphere.api_gateway.service;

public record ServiceInstanceDto(String name, String host, int port, String status) {

    public String url() {
        return "http://" + host + ":" + port;
    }
}