package com.fengxue.menu.integration;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * B 站 Web 接口客户端：二维码登录、导航信息、收藏夹列表。
 */
@Component
public class BilibiliApi {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36";
    private static final String REFERER = "https://www.bilibili.com/";
    private static final String QR_GENERATE =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
    private static final String QR_POLL =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public HttpResult generateQr() {
        return getJson(QR_GENERATE, null);
    }

    public HttpResult pollQr(String qrcodeKey, String cookieHeader) {
        return getJson(QR_POLL + urlEncode(qrcodeKey), cookieHeader);
    }

    public JsonNode nav(String cookieHeader) {
        return getJson("https://api.bilibili.com/x/web-interface/nav", cookieHeader).json();
    }

    public JsonNode folderInfo(String mediaId, String cookieHeader) {
        return getJson("https://api.bilibili.com/x/v3/fav/folder/info?media_id="
                + urlEncode(mediaId), cookieHeader).json();
    }

    public JsonNode favList(String mediaId, int page, int pageSize, String cookieHeader) {
        String url = "https://api.bilibili.com/x/v3/fav/resource/list"
                + "?media_id=" + urlEncode(mediaId)
                + "&pn=" + page
                + "&ps=" + pageSize
                + "&keyword=&order=mtime&type=0&tid=0&platform=web";
        return getJson(url, cookieHeader).json();
    }

    /**
     * 检查 B 站返回 code，-101 表示登录失效。
     */
    public static void assertCode(JsonNode root) {
        int code = root.path("code").asInt(-1);
        if (code == 0) {
            return;
        }
        String message = root.path("message").asString("B站接口返回错误");
        if (code == -101) {
            throw new BilibiliAuthException("B站登录已过期，请重新扫码登录");
        }
        throw new BusinessException(ResultCode.IMPORT_FAILED, "B站接口返回：" + message);
    }

    public static String cookieHeader(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        cookies.forEach((name, value) -> {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(name).append('=').append(value);
        });
        return builder.toString();
    }

    private HttpResult getJson(String url, String cookieHeader) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", UA)
                    .header("Referer", REFERER)
                    .header("Accept", "application/json, text/plain, */*")
                    .GET();
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                builder.header("Cookie", cookieHeader);
            }
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new BusinessException(ResultCode.IMPORT_FAILED,
                        "B站接口 HTTP " + response.statusCode());
            }
            JsonNode root = JsonSupport.readTree(response.body());
            return new HttpResult(root, parseCookies(response.headers().allValues("Set-Cookie")));
        } catch (BusinessException | BilibiliAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.IMPORT_FAILED,
                    "B站接口请求失败：" + e.getClass().getSimpleName());
        }
    }

    private Map<String, String> parseCookies(Iterable<String> setCookies) {
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String raw : setCookies) {
            String first = raw.split(";", 2)[0];
            int index = first.indexOf('=');
            if (index > 0) {
                cookies.put(first.substring(0, index).trim(), first.substring(index + 1).trim());
            }
        }
        return cookies;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public record HttpResult(JsonNode json, Map<String, String> cookies) {
    }
}
