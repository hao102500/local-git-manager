package com.localgit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("code", 200);
        result.put("message", "服务运行正常");
        result.put("data", Map.of(
                "status", "UP",
                "application", "local-git-manager-server",
                "timestamp", LocalDateTime.now().toString()
        ));

        return result;
    }
}