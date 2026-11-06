package com.fengxue.menu.ai;

import com.fengxue.menu.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiJsonParserTest {

    @Test
    void extractsJsonFromCodeFence() {
        String content = """
                好的，下面是结果：
                ```json
                {"title":"红烧肉","ingredients":[{"name":"五花肉","amount":"500","unit":"g"}]}
                ```
                希望有帮助。
                """;
        JsonNode node = AiJsonParser.parse(content);

        assertEquals("红烧肉", node.path("title").asString());
        assertEquals("五花肉", node.path("ingredients").path(0).path("name").asString());
    }

    @Test
    void extractsJsonFromSurroundingText() {
        String content = "结果如下：{\"name\":\"番茄炒蛋\"} 请查收";
        JsonNode node = AiJsonParser.parse(content);

        assertEquals("番茄炒蛋", node.path("name").asString());
    }

    @Test
    void invalidContentThrowsBusinessException() {
        assertThrows(BusinessException.class, () -> AiJsonParser.parse("抱歉，我无法完成"));
    }
}
