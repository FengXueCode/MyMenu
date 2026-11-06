package com.fengxue.menu.ai;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import tools.jackson.databind.JsonNode;

/**
 * 大模型返回内容里的 JSON 提取与容错解析。
 */
public final class AiJsonParser {

    private AiJsonParser() {
    }

    public static JsonNode parse(String content) {
        String json = extractJson(content);
        if (json == null) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 返回内容不是合法 JSON，请重试");
        }
        try {
            return JsonSupport.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 返回 JSON 解析失败，请重试");
        }
    }

    /**
     * 去掉 ```json 围栏和前后解释文字，截取第一个完整 JSON 对象。
     */
    public static String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content.trim();
        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int lineEnd = text.indexOf('\n', fenceStart);
            if (lineEnd > 0) {
                text = text.substring(lineEnd + 1);
            }
            int fenceEnd = text.lastIndexOf("```");
            if (fenceEnd >= 0) {
                text = text.substring(0, fenceEnd);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
