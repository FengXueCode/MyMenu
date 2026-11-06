package com.fengxue.menu.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlGuardTest {

    private final UrlGuard guard = new UrlGuard();

    @Test
    void allowsOnlyKnownPlatformHosts() {
        assertTrue(UrlGuard.isAllowedHost("www.bilibili.com"));
        assertTrue(UrlGuard.isAllowedHost("b23.tv"));
        assertTrue(UrlGuard.isAllowedHost("v.douyin.com"));
        assertTrue(UrlGuard.isAllowedHost("www.iesdouyin.com"));
        assertFalse(UrlGuard.isAllowedHost("evil-bilibili.com"));
        assertFalse(UrlGuard.isAllowedHost("127.0.0.1"));
        assertFalse(UrlGuard.isAllowedHost("localhost"));
    }

    @Test
    void rejectsUnsafeSchemes() {
        assertThrows(RuntimeException.class, () -> guard.normalize("file:///etc/passwd"));
        assertThrows(RuntimeException.class, () -> guard.normalize("ftp://b23.tv/abc"));
    }

    @Test
    void rejectsDisallowedHost() {
        assertThrows(RuntimeException.class, () -> guard.normalize("https://evil.example.com/video/1"));
    }

    @Test
    void extractsFirstUrlFromShareText() {
        String text = "7.85 复制打开抖音，看看【小灶的作品】红烧肉 #美食 https://v.douyin.com/abc123/ 复制此链接";
        assertEquals("https://v.douyin.com/abc123/", UrlGuard.extractUrl(text));
    }

    @Test
    void normalizesUrlWithoutScheme() {
        assertEquals("https://b23.tv/abc", guard.normalize("b23.tv/abc"));
    }
}
