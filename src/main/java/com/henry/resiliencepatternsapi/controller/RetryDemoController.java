package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.dto.RetryDemoResponse;
import com.henry.resiliencepatternsapi.service.RetryDemoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class RetryDemoController {

    private final RetryDemoService service;

    public RetryDemoController(RetryDemoService service) {
        this.service = service;
    }

    @GetMapping("/retry")
    public RetryDemoResponse call(@RequestParam(defaultValue = "0") @Min(0) @Max(3) int failTimes) {
        return service.call(failTimes);
    }
}
