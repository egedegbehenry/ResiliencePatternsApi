package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.service.CircuitBreakerDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class CircuitBreakerDemoController {

    private final CircuitBreakerDemoService service;

    public CircuitBreakerDemoController(CircuitBreakerDemoService service) {
        this.service = service;
    }

    @GetMapping("/circuit-breaker")
    public Map<String, String> call(@RequestParam(defaultValue = "false") boolean fail) {
        return Map.of("message", service.call(fail));
    }
}
