package com.fengxue.menu.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 回归：手机/局域网访问时 Origin 需要被 CORS 白名单接受，
 * 否则 Spring 会直接返回 403 Invalid CORS request。
 */
class CorsConfigTest {

    private CorsConfiguration config(String... patterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(patterns));
        return configuration;
    }

    @Test
    void localhostAndPrivateNetworkOriginsAreAllowed() {
        CorsConfiguration configuration = config(
                "http://localhost:*", "http://127.0.0.1:*",
                "http://192.168.*:*", "http://10.*:*", "http://172.*:*");

        assertEquals("http://localhost:5173",
                configuration.checkOrigin("http://localhost:5173"));
        assertEquals("http://10.8.131.19:5173",
                configuration.checkOrigin("http://10.8.131.19:5173"));
        assertEquals("http://192.168.153.1:5173",
                configuration.checkOrigin("http://192.168.153.1:5173"));
        assertNull(configuration.checkOrigin("http://evil.example.com"));
    }
}
