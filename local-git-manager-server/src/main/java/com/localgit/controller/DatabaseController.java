package com.localgit.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/database")
public class DatabaseController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT sqlite_version()",
                    String.class
            );

            result.put("code", 200);
            result.put("message", "SQLite 连接成功");
            result.put("data", Map.of(
                    "status", "UP",
                    "database", "SQLite",
                    "version", version
            ));

        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "SQLite 连接失败");
            result.put("data", e.getMessage());
        }

        return result;
    }


    @GetMapping("/repositories/table")
    public Map<String, Object> getRepositoryTable() {

        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "PRAGMA table_info(repositories)"
        );

        return Map.of(
                "code", 200,
                "message", "仓库数据表查询成功",
                "data", columns
        );
    }
}