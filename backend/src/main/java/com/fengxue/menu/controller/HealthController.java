package com.fengxue.menu.controller;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 健康检查：MySQL 为硬依赖（决定 HTTP 200/503），Redis 为可选缓存（只展示状态）。
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisUtil redisUtil;

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean mysqlUp = probeMysql(detail);
        boolean redisUp = probeRedis(detail);
        detail.put("status", mysqlUp ? "UP" : "DOWN");
        detail.put("mysql", mysqlUp ? "UP" : "DOWN");
        detail.put("redis", redisUp ? "UP" : "DOWN");
        detail.put("timestamp", System.currentTimeMillis());

        Result<Map<String, Object>> body = mysqlUp
                ? Result.success(detail)
                : Result.failure(ResultCode.SERVICE_UNAVAILABLE.getCode(), "数据库不可用");
        return ResponseEntity.status(mysqlUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping("/mysql")
    public ResponseEntity<Result<Map<String, Object>>> mysql() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean up = probeMysql(detail);
        detail.put("mysql", up ? "UP" : "DOWN");
        Result<Map<String, Object>> body = up
                ? Result.success(detail)
                : Result.failure(ResultCode.SERVICE_UNAVAILABLE.getCode(), "MySQL 不可用");
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping("/redis")
    public ResponseEntity<Result<Map<String, Object>>> redis() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean up = probeRedis(detail);
        detail.put("redis", up ? "UP" : "DOWN");
        Result<Map<String, Object>> body = up
                ? Result.success(detail)
                : Result.failure(ResultCode.SERVICE_UNAVAILABLE.getCode(), "Redis 不可用（可选依赖）");
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean probeMysql(Map<String, Object> detail) {
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean ok = value != null && value == 1;
            if (!ok) {
                detail.put("mysqlError", "SELECT 1 返回了非预期结果");
            }
            return ok;
        } catch (Exception e) {
            log.warn("MySQL 健康检查失败: {}", e.getMessage());
            detail.put("mysqlError", e.getClass().getSimpleName());
            return false;
        }
    }

    private boolean probeRedis(Map<String, Object> detail) {
        String key = "menu:health:" + UUID.randomUUID();
        try {
            redisUtil.set(key, "pong", Duration.ofSeconds(10));
            Object value = redisUtil.get(key);
            boolean ok = "pong".equals(value);
            if (!ok) {
                detail.put("redisError", "读取到的值与写入值不一致");
            }
            return ok;
        } catch (Exception e) {
            log.debug("Redis 健康检查失败（可选依赖）: {}", e.getMessage());
            detail.put("redisError", e.getClass().getSimpleName());
            return false;
        } finally {
            try {
                redisUtil.delete(key);
            } catch (Exception ignored) {
                // Redis 不可用时忽略
            }
        }
    }
}
