package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.dto.FallbackDemoResponse;
import com.henry.resiliencepatternsapi.service.FallbackDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class FallbackDemoController {

    private final FallbackDemoService service;

    public FallbackDemoController(FallbackDemoService service) {
        this.service = service;
    }

    @GetMapping("/fallback")
    public FallbackDemoResponse call(@RequestParam(defaultValue = "false") boolean fail) {
        return service.call(fail);
    }
}
