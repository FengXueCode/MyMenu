package com.fengxue.menu.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareTextParserTest {

    @Test
    void parsesDouyinShareText() {
        String text = "7.85 复制打开抖音，看看【美食作家王刚的作品】红烧肉这样做才好吃 #美食教程 https://v.douyin.com/abc123/";
        ShareTextParser.ParsedShare parsed = ShareTextParser.parse(text);

        assertEquals("https://v.douyin.com/abc123/", parsed.url());
        assertEquals("美食作家王刚", parsed.author());
        assertNotNull(parsed.title());
        assertTrue(parsed.title().contains("红烧肉"));
    }

    @Test
    void parsesBilibiliShareText() {
        String text = "【保姆级红烧肉教程】 https://b23.tv/xyz789";
        ShareTextParser.ParsedShare parsed = ShareTextParser.parse(text);

        assertEquals("https://b23.tv/xyz789", parsed.url());
        assertTrue(parsed.title().contains("红烧肉"));
    }

    @Test
    void handlesPlainTextWithoutUrl() {
        ShareTextParser.ParsedShare parsed = ShareTextParser.parse("番茄炒蛋 10 分钟快手菜");
        assertEquals(null, parsed.url());
        assertNotNull(parsed.title());
    }
}
