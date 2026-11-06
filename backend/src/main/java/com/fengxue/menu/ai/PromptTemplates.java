package com.fengxue.menu.ai;

/**
 * 提示词模板。
 */
public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static final String EXTRACT_SYSTEM = """
            你是一位专业的中餐菜谱编辑。请从用户提供的视频标题、简介、字幕或文案中，
            提取出一道完整、可复现的菜谱，并只输出一个 JSON 对象，不要输出任何解释或 Markdown 围栏。

            JSON 结构必须严格如下（字段全部必填）：
            {
              "title": "菜名，4-20字",
              "description": "20-60字的诱人描述",
              "categoryName": "从 家常菜/川菜/粤菜/快手菜/汤羹/主食/早餐/甜点 中选一个",
              "meatType": "MEAT 荤菜 / VEG 素菜 / SOUP 汤羹 / STAPLE 主食 / DESSERT 甜点 / OTHER 其他",
              "difficulty": 1到3的整数（1简单 2中等 3进阶）,
              "servings": 整数，默认2,
              "cookMinutes": 整数，烹饪总时长,
              "spiceLevel": 0到3的整数（0不辣）,
              "tags": ["2-4个标签"],
              "ingredients": [{"name": "食材名", "amount": "数字字符串", "unit": "g/ml/个/根/瓣/勺/片/只/把"}],
              "steps": ["步骤1（具体到火候和时间）", "步骤2"],
              "tips": "一句实用小贴士"
            }

            规则：
            1. 如果原文信息不足，请根据菜名和常识补全合理步骤，不要编造与菜名冲突的内容；
            2. ingredients 至少 4 项、steps 至少 4 步；
            3. 全部使用简体中文；
            4. 只输出 JSON 对象本身。
            """;

    public static final String SUMMARY_SYSTEM = """
            你是一位中餐配菜顾问。根据给定的菜谱，输出一个 JSON 对象，不要输出解释或 Markdown 围栏：
            {
              "summary": "一句话总结这道菜的卖点，不超过50字",
              "ingredientsBrief": ["主要食材1", "主要食材2", "主要食材3"],
              "pairing": "搭配建议（配什么主食/汤/饮品），不超过30字"
            }
            全部使用简体中文，只输出 JSON。
            """;

    public static String extractUserPrompt(String title, String platform, String text) {
        StringBuilder builder = new StringBuilder();
        if (title != null && !title.isBlank()) {
            builder.append("视频标题：").append(title.trim()).append('\n');
        }
        if (platform != null && !platform.isBlank()) {
            builder.append("来源平台：").append(platform).append('\n');
        }
        builder.append("视频文本：\n").append(text == null ? "" : text.trim());
        return builder.toString();
    }

    public static String summaryUserPrompt(String title, String description, String ingredients) {
        return "菜谱名：" + title + "\n描述：" + (description == null ? "" : description)
                + "\n配料：" + ingredients;
    }

    public static String imagePrompt(String title, String meatType) {
        return "为一款中式菜品《" + title + "》生成一张有食欲的美食摄影图，"
                + "暖色灯光、浅景深、家常餐桌氛围，画面干净，不要出现文字和水印，"
                + "构图适合作为菜谱封面（1:1）。菜品类型：" + (meatType == null ? "家常菜" : meatType);
    }
}
