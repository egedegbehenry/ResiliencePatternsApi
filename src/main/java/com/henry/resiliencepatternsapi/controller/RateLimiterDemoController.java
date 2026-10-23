package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.service.RateLimiterDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class RateLimiterDemoController {

    private final RateLimiterDemoService service;

    public RateLimiterDemoController(RateLimiterDemoService service) {
        this.service = service;
    }

    @GetMapping("/rate-limit")
    public Map<String, String> call() {
        return Map.of("message", service.call());
    }
}
