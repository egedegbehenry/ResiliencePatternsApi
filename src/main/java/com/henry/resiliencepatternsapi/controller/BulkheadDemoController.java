package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.service.BulkheadDemoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class BulkheadDemoController {

    private final BulkheadDemoService service;

    public BulkheadDemoController(BulkheadDemoService service) {
        this.service = service;
    }

    @GetMapping("/bulkhead")
    public Map<String, String> call(@RequestParam(defaultValue = "1000") @Min(0) @Max(3000) int delayMs) {
        return Map.of("message", service.call(delayMs));
    }
}
