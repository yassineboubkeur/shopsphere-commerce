package com.shopsphere.service_discovery.controller;

import com.shopsphere.service_discovery.dto.RegisterRequest;
import com.shopsphere.service_discovery.entity.ServiceInstance;
import com.shopsphere.service_discovery.service.RegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/eureka")
@RequiredArgsConstructor
public class EurekaController {

    private final RegistryService registryService;

    @PostMapping("/register")
    public ResponseEntity<ServiceInstance> register(@RequestBody RegisterRequest request) {
        ServiceInstance instance = ServiceInstance.builder()
                .name(request.getName())
                .host(request.getHost())
                .port(request.getPort())
                .build();
        return new ResponseEntity<>(registryService.register(instance), HttpStatus.CREATED);
    }

    @PutMapping("/renew/{name}")
    public ResponseEntity<Void> renew(@PathVariable String name) {
        return registryService.renew(name)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/deregister/{name}")
    public ResponseEntity<Void> deregister(@PathVariable String name) {
        registryService.deregister(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/instances/{name}")
    public List<ServiceInstance> getInstances(@PathVariable String name) {
        return registryService.getInstances(name);
    }

    @GetMapping("/instances")
    public Map<String, ServiceInstance> getAllInstances() {
        return registryService.getAllInstances();
    }
}