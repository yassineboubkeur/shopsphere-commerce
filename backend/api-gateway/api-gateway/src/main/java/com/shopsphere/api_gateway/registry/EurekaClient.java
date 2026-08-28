package com.shopsphere.api_gateway.registry;

import com.shopsphere.api_gateway.config.EurekaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EurekaClient {

    private final RestTemplate restTemplate;
    private final EurekaProperties eurekaProperties;

    public void register() {
        if (!eurekaProperties.isEnabled()) {
            return;
        }
        Map<String, Object> body = Map.of(
                "name", eurekaProperties.getName(),
                "host", eurekaProperties.getHost(),
                "port", eurekaProperties.getPort()
        );
        try {
            restTemplate.postForEntity(eurekaProperties.getUrl() + "/register", json(body), String.class);
            log.info("Registered with Eureka: {}@{}:{}", eurekaProperties.getName(),
                    eurekaProperties.getHost(), eurekaProperties.getPort());
        } catch (Exception e) {
            log.warn("Eureka registration failed: {}", e.getMessage());
        }
    }

    public void renew() {
        if (!eurekaProperties.isEnabled()) {
            return;
        }
        try {
            restTemplate.exchange(eurekaProperties.getUrl() + "/renew/" + eurekaProperties.getName(),
                    HttpMethod.PUT, null, String.class);
        } catch (Exception e) {
            log.warn("Eureka heartbeat failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private HttpEntity<String> json(Object body) {
        String payload;
        if (body instanceof Map map) {
            payload = "{\"name\":\"" + map.get("name") + "\",\"host\":\"" + map.get("host")
                    + "\",\"port\":" + map.get("port") + "}";
        } else {
            payload = body.toString();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }
}