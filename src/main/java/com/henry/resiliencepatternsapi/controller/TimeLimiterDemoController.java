package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.service.TimeLimiterDemoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class TimeLimiterDemoController {

    private final TimeLimiterDemoService service;

    public TimeLimiterDemoController(TimeLimiterDemoService service) {
        this.service = service;
    }

    @GetMapping("/time-limit")
    public Map<String, String> call(@RequestParam(defaultValue = "2000") @Min(0) @Max(3000) int delayMs)
            throws Exception {
        return Map.of("message", service.call(delayMs));
    }
}
