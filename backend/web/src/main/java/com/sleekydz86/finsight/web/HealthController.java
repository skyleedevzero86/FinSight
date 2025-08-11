package com.sleekydz86.finsight.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "up";
    }

    @GetMapping("/exception")
    public String exception() {
        throw new IllegalArgumentException("예외 발생!");
    }
}
