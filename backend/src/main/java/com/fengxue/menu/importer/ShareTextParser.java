package com.fengxue.menu.importer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从抖音/B站分享文案中提取标题、作者、链接。
 *
 * <p>抖音分享文案示例：{@code 7.85 复制打开抖音，看看【美食作家王刚的作品】红烧肉教程 #美食 https://v.douyin.com/xxxx/}</p>
 */
public final class ShareTextParser {

    private static final Pattern AUTHOR_BLOCK = Pattern.compile("看看【([^】]{1,80})】");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern HASHTAG = Pattern.compile("#[^\\s#]+");
    private static final Pattern EMOJI = Pattern.compile("[\\p{So}\\p{Sk}\\p{Cs}]+");

    private ShareTextParser() {
    }

    public record ParsedShare(String url, String title, String author) {
    }

    public static ParsedShare parse(String text) {
        if (text == null || text.isBlank()) {
            return new ParsedShare(null, null, null);
        }
        String raw = text.trim();
        String url = UrlGuard.extractUrl(raw);
        String author = null;
        Matcher authorMatcher = AUTHOR_BLOCK.matcher(raw);
        if (authorMatcher.find()) {
            author = authorMatcher.group(1)
                    .replace("的作品", "")
                    .replace("的图文", "")
                    .trim();
        }

        String title = raw;
        if (url != null) {
            title = title.replace(url, " ");
        }
        title = AUTHOR_BLOCK.matcher(title).replaceAll(" ");
        title = title.replaceAll("复制打开抖音|复制此链接|打开抖音|看看|的作品|https?://\\S*", " ");
        title = HASHTAG.matcher(title).replaceAll(" ");
        title = EMOJI.matcher(title).replaceAll(" ");
        title = title.replaceAll("[0-9.]+\\s*[A-Za-z@:：]*", " ");
        title = WHITESPACE.matcher(title).replaceAll(" ").trim();
        title = title.replaceAll("^[，。、,;；:：\\-—]+|[，。、,;；:：\\-—]+$", "").trim();
        if (title.length() > 60) {
            title = title.substring(0, 60);
        }
        return new ParsedShare(url, title.isBlank() ? null : title, author);
    }
}
