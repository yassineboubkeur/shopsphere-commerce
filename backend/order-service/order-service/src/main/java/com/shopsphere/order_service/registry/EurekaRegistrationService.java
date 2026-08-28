package com.shopsphere.order_service.registry;

import com.shopsphere.order_service.config.EurekaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EurekaRegistrationService implements ApplicationRunner {

    private final EurekaClient eurekaClient;
    private final EurekaProperties eurekaProperties;

    @Override
    public void run(ApplicationArguments args) {
        eurekaClient.register();
    }

    @Scheduled(fixedDelayString = "${eureka.heartbeat-interval-seconds:30}000",
            initialDelayString = "${eureka.heartbeat-interval-seconds:30}000")
    public void sendHeartbeat() {
        eurekaClient.renew();
    }
}