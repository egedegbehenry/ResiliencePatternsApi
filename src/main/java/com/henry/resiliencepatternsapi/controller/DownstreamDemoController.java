package com.henry.resiliencepatternsapi.controller;

import com.henry.resiliencepatternsapi.dto.DownstreamDemoResponse;
import com.henry.resiliencepatternsapi.service.DownstreamCircuitBreakerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DownstreamDemoController {

    private final DownstreamCircuitBreakerService service;

    public DownstreamDemoController(DownstreamCircuitBreakerService service) {
        this.service = service;
    }

    @GetMapping("/inventory")
    public DownstreamDemoResponse inventory(@RequestParam(defaultValue = "false") boolean fail) {
        return new DownstreamDemoResponse("inventoryService", service.inventory(fail));
    }

    @GetMapping("/payment")
    public DownstreamDemoResponse payment(@RequestParam(defaultValue = "false") boolean fail) {
        return new DownstreamDemoResponse("paymentService", service.payment(fail));
    }
}
