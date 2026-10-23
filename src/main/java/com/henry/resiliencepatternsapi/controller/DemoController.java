package com.henry.resiliencepatternsapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "message",
                "Resilience API is running successfully"
        );
    }
}
