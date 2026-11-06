package com.fengxue.menu.importer;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 带安全约束的 HTTP 抓取器：手动跟随重定向、逐跳校验域名、限制响应大小与超时。
 */
@Component
public class HttpFetcher {

    private final HttpClient client;
    private final UrlGuard urlGuard;
    private final int timeoutSeconds;
    private final int maxBytes;
    private final int maxRedirects;

    public HttpFetcher(UrlGuard urlGuard,
                       @Value("${app.import.timeout-seconds:8}") int timeoutSeconds,
                       @Value("${app.import.max-bytes:2097152}") int maxBytes,
                       @Value("${app.import.max-redirects:3}") int maxRedirects) {
        this.urlGuard = urlGuard;
        this.timeoutSeconds = timeoutSeconds;
        this.maxBytes = maxBytes;
        this.maxRedirects = maxRedirects;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public FetchedPage get(String rawUrl, Map<String, String> headers) {
        String current = urlGuard.normalize(rawUrl);
        for (int hop = 0; hop <= maxRedirects; hop++) {
            URI uri = URI.create(current);
            urlGuard.assertSafe(uri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .GET();
            if (headers != null) {
                headers.forEach(builder::header);
            }
            try {
                HttpResponse<InputStream> response = client.send(builder.build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("location").orElse(null);
                    if (location == null) {
                        throw new BusinessException(ResultCode.IMPORT_FAILED, "视频页面重定向失败");
                    }
                    current = uri.resolve(location).toString();
                    continue;
                }
                String body = readLimited(response.body());
                return new FetchedPage(status, current, body,
                        response.headers().firstValue("content-type").orElse(""));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ResultCode.IMPORT_FAILED, "视频页面抓取失败：" + e.getClass().getSimpleName());
            }
        }
        throw new BusinessException(ResultCode.IMPORT_FAILED, "视频链接重定向次数过多");
    }

    private String readLimited(InputStream stream) throws Exception {
        try (stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = stream.read(chunk)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new BusinessException(ResultCode.IMPORT_FAILED, "视频页面内容过大，已停止解析");
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    public record FetchedPage(int status, String finalUrl, String body, String contentType) {
    }
}
