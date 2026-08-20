package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<?> home() {
        return ApiResponse.of(java.util.Map.of("service", "carpool-backend", "status", "ok"));
    }
}
