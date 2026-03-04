package com.harness.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    private static final String VERSION = System.getenv().getOrDefault("APP_VERSION", "1.0.0");

    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of(
            "message", "Hello from Harness Demo App!",
            "version", VERSION
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
