package com.fengxue.menu.common.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 全局共享的 Jackson 3 序列化器（Spring Boot 4 使用 {@code tools.jackson}）。
 *
 * <p>只用于后端内部读写 JSON 字段（标签、偏好、菜单快照、第三方响应），
 * 不参与 MVC 的请求/响应序列化（那部分由 Boot 自动配置的 Mapper 处理）。</p>
 */
public final class JsonSupport {

    public static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private JsonSupport() {
    }

    public static String toJson(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    public static JsonNode readTree(String json) {
        return MAPPER.readTree(json);
    }
}
