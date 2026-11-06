package com.fengxue.menu.importer;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * 导入链接安全校验：只允许 B 站 / 抖音域名，禁止内网、回环、链路本地地址（防 SSRF）。
 */
@Component
public class UrlGuard {

    private static final List<String> ALLOWED_HOSTS = List.of(
            "bilibili.com", "b23.tv", "douyin.com", "iesdouyin.com");

    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "请提供视频分享链接");
        }
        String text = raw.trim();
        int start = text.indexOf("http://");
        if (start < 0) {
            start = text.indexOf("https://");
        }
        if (start >= 0) {
            text = text.substring(start);
        } else {
            text = "https://" + text;
        }
        try {
            URI uri = URI.create(text);
            assertSafe(uri);
            return uri.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "链接格式不正确");
        }
    }

    public void assertSafe(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "仅支持 http/https 链接");
        }
        if (uri.getUserInfo() != null) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "链接包含非法用户信息");
        }
        String host = uri.getHost();
        if (host == null || !isAllowedHost(host)) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "仅支持 B 站 / 抖音分享链接");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new BusinessException(ResultCode.IMPORT_FAILED, "链接指向了不安全的地址");
                }
            }
        } catch (UnknownHostException e) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "无法解析链接域名");
        }
    }

    public static boolean isAllowedHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return ALLOWED_HOSTS.stream()
                .anyMatch(allowed -> lower.equals(allowed) || lower.endsWith("." + allowed));
    }

    /**
     * 从任意分享文本中提取第一个链接。
     */
    public static String extractUrl(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf("http://");
        if (start < 0) {
            start = text.indexOf("https://");
        }
        if (start < 0) {
            return null;
        }
        int end = start;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        String url = text.substring(start, end);
        return url.replaceAll("[，。；、,;）)】\\]]+$", "");
    }
}
