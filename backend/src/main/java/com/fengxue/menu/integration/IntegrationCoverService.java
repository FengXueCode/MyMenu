package com.fengxue.menu.integration;

import com.fengxue.menu.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * 把收藏视频的远程封面转存到本地，避免 B 站/抖音图片防盗链。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationCoverService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final FileStorageService fileStorageService;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public String store(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        String url = rawUrl.startsWith("//") ? "https:" + rawUrl : rawUrl;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!allowedHost(host)) {
                return url;
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 Chrome/131.0 Safari/537.36")
                    .header("Referer", "https://www.bilibili.com/")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                return url;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > MAX_BYTES) {
                return url;
            }
            String contentType = response.headers().firstValue("content-type").orElse("image/jpeg");
            String extension = contentType.contains("png") ? "png"
                    : contentType.contains("webp") ? "webp"
                    : contentType.contains("gif") ? "gif" : "jpg";
            return fileStorageService.saveImage(body, extension);
        } catch (Exception e) {
            log.debug("视频封面转存失败，使用远程地址: {}", e.getMessage());
            return url;
        }
    }

    private boolean allowedHost(String host) {
        if (host.isBlank()) {
            return false;
        }
        return host.endsWith("hdslb.com")
                || host.endsWith("biliimg.com")
                || host.endsWith("douyinpic.com")
                || host.endsWith("byteimg.com")
                || host.endsWith("douyin.com");
    }
}
