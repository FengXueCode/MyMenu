package com.fengxue.menu.ai;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议客户端。所有请求由后端发起，API Key 只在内存中使用、不落日志。
 */
@Component
public class AiClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String chat(AiProvider provider, String apiKey, String systemPrompt,
                       String userPrompt, int timeoutSeconds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.getChatModel());
        body.put("stream", false);
        body.put("temperature", 0.7);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        JsonNode response = postJson(endpoint(provider.getBaseUrl(), "/chat/completions"),
                apiKey, JsonSupport.toJson(body), timeoutSeconds);
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        String text = content.isMissingNode() || content.isNull() ? "" : content.asString("");
        if (text.isBlank() && content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : content) {
                builder.append(part.path("text").asString(""));
            }
            text = builder.toString();
        }
        if (text.isBlank()) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 返回了空内容，请重试");
        }
        return text;
    }

    public String test(AiProvider provider, String apiKey) {
        return chat(provider, apiKey, "你是一个连通性测试助手。", "请只回复两个字符：OK", 30);
    }

    public JsonNode images(AiProvider provider, String apiKey, String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.getImageModel());
        body.put("prompt", prompt);
        body.put("n", 1);
        body.put("size", "1024x1024");
        body.put("response_format", "b64_json");
        return postJson(endpoint(provider.getBaseUrl(), "/images/generations"),
                apiKey, JsonSupport.toJson(body), 120);
    }

    public byte[] download(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 图片地址不合法");
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 图片下载失败");
            }
            return response.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 图片下载失败");
        }
    }

    private JsonNode postJson(String url, String apiKey, String body, int timeoutSeconds) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String responseBody = response.body() == null ? "" : response.body();
            if (status / 100 != 2) {
                String shortBody = responseBody.length() > 240 ? responseBody.substring(0, 240) : responseBody;
                String message = switch (status) {
                    case 401, 403 -> "AI Key 无效或没有该模型权限";
                    case 404 -> "AI 接口地址或模型名不正确";
                    case 429 -> "AI 服务限流，请稍后重试";
                    default -> "AI 服务返回 HTTP " + status + "：" + shortBody;
                };
                throw new BusinessException(ResultCode.AI_CALL_FAILED, message);
            }
            return JsonSupport.readTree(responseBody);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED,
                    "AI 请求失败：" + e.getClass().getSimpleName());
        }
    }

    private String endpoint(String baseUrl, String suffix) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ResultCode.AI_NOT_CONFIGURED, "AI 接口地址为空，请检查配置");
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + suffix;
    }
}
