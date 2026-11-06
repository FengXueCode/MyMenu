package com.fengxue.menu.ai;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.recipe.RecipeDtos;
import com.fengxue.menu.recipe.RecipeIngredient;
import com.fengxue.menu.recipe.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final Set<String> MEAT_TYPES =
            Set.of("MEAT", "VEG", "SOUP", "STAPLE", "DESSERT", "OTHER");

    private final AiProviderService providerService;
    private final AiClient aiClient;
    private final RecipeService recipeService;

    public AiDtos.RecipeDraft extract(Long userId, AiDtos.ExtractRequest request) {
        String text = firstNonBlank(request.text(), request.extraText());
        if (text == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请提供视频文案或菜谱描述");
        }
        if (text.length() > 12000) {
            text = text.substring(0, 12000);
        }
        AiProvider provider = providerService.requireDefault(userId);
        String content = aiClient.chat(provider, providerService.apiKeyOf(provider),
                PromptTemplates.EXTRACT_SYSTEM,
                PromptTemplates.extractUserPrompt(request.title(), request.platform(), text), 90);
        JsonNode node = AiJsonParser.parse(content);
        return parseDraft(node, true);
    }

    /**
     * 收藏夹自动扫描：先判断视频是否菜谱，是则同时返回结构化菜谱。
     */
    public AiDtos.VideoRecipeDecision decideVideoRecipe(Long userId, String title, String description) {
        String text = firstNonBlank(title, description);
        if (text == null) {
            return new AiDtos.VideoRecipeDecision(false, "缺少视频标题与简介", null);
        }
        if (text.length() > 6000) {
            text = text.substring(0, 6000);
        }
        AiProvider provider = providerService.requireDefault(userId);
        String content = aiClient.chat(provider, providerService.apiKeyOf(provider),
                PromptTemplates.VIDEO_RECIPE_SYSTEM,
                PromptTemplates.videoRecipePrompt(title, description, "video"), 90);
        JsonNode node = AiJsonParser.parse(content);
        boolean isRecipe = node.path("isRecipe").asBoolean(false);
        String reason = clampText(node.path("reason").asString(""), 100, null);
        AiDtos.RecipeDraft draft = null;
        JsonNode recipeNode = node.path("recipe");
        if (isRecipe && recipeNode.isObject()) {
            draft = parseDraft(recipeNode, false);
        }
        return new AiDtos.VideoRecipeDecision(isRecipe, reason, draft);
    }

    private AiDtos.RecipeDraft parseDraft(JsonNode node, boolean requireComplete) {
        List<AiDtos.IngredientDraft> ingredients = parseIngredients(node.path("ingredients"));
        List<String> steps = parseSteps(node.path("steps"));
        if (requireComplete && (ingredients.isEmpty() || steps.isEmpty())) {
            throw new BusinessException(ResultCode.AI_CALL_FAILED,
                    "AI 未能提取到完整菜谱，请补充一段视频文案后重试");
        }
        return new AiDtos.RecipeDraft(
                clampText(node.path("title").asString(""), 100, "视频菜谱"),
                clampText(node.path("description").asString(""), 200, null),
                clampText(node.path("categoryName").asString(""), 16, "家常菜"),
                normalizeMeatType(node.path("meatType").asString("")),
                clampInt(node.path("difficulty").asInt(1), 1, 3),
                clampInt(node.path("servings").asInt(2), 1, 10),
                clampInt(node.path("cookMinutes").asInt(30), 5, 240),
                clampInt(node.path("spiceLevel").asInt(1), 0, 3),
                parseTags(node.path("tags")),
                ingredients,
                steps,
                clampText(node.path("tips").asString(""), 300, null));
    }

    public AiDtos.SummaryResponse summary(Long userId, Long recipeId) {
        RecipeDtos.RecipeDetailVO recipe = recipeService.detail(userId, recipeId);
        AiProvider provider = providerService.requireDefault(userId);
        String ingredientText = recipe.ingredients().stream()
                .map(i -> i.name() + " " + (i.amount() == null ? "" : i.amount())
                        + (i.unit() == null ? "" : i.unit()))
                .reduce((a, b) -> a + "、" + b).orElse("未知");
        String content = aiClient.chat(provider, providerService.apiKeyOf(provider),
                PromptTemplates.SUMMARY_SYSTEM,
                PromptTemplates.summaryUserPrompt(recipe.title(), recipe.description(), ingredientText), 60);
        JsonNode node = AiJsonParser.parse(content);
        String summary = clampText(node.path("summary").asString(""), 200, null);
        if (summary == null) {
            summary = (recipe.description() == null ? recipe.title() : recipe.description());
        }
        List<String> brief = new ArrayList<>();
        for (JsonNode item : node.path("ingredientsBrief")) {
            if (brief.size() >= 5) {
                break;
            }
            String value = item.asString("");
            if (!value.isBlank()) {
                brief.add(value.length() > 20 ? value.substring(0, 20) : value);
            }
        }
        String pairing = clampText(node.path("pairing").asString(""), 100, null);
        recipeService.updateSummary(recipeId, summary);
        return new AiDtos.SummaryResponse(summary, brief, pairing);
    }

    private List<AiDtos.IngredientDraft> parseIngredients(JsonNode array) {
        List<AiDtos.IngredientDraft> list = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return list;
        }
        for (JsonNode item : array) {
            if (list.size() >= 40) {
                break;
            }
            String name = item.path("name").asString("").trim();
            if (name.isBlank()) {
                continue;
            }
            list.add(new AiDtos.IngredientDraft(
                    name.length() > 40 ? name.substring(0, 40) : name,
                    clampText(item.path("amount").asString(""), 20, "适量"),
                    clampText(item.path("unit").asString(""), 10, "")));
        }
        return list;
    }

    private List<String> parseSteps(JsonNode array) {
        List<String> steps = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return steps;
        }
        for (JsonNode item : array) {
            if (steps.size() >= 40) {
                break;
            }
            String content = item.isObject() ? item.path("content").asString("") : item.asString("");
            content = content.trim();
            if (!content.isBlank()) {
                steps.add(content.length() > 300 ? content.substring(0, 300) : content);
            }
        }
        return steps;
    }

    private List<String> parseTags(JsonNode array) {
        List<String> tags = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return tags;
        }
        for (JsonNode item : array) {
            if (tags.size() >= 6) {
                break;
            }
            String tag = item.asString("").trim();
            if (!tag.isBlank()) {
                tags.add(tag.length() > 12 ? tag.substring(0, 12) : tag);
            }
        }
        return tags;
    }

    private String normalizeMeatType(String value) {
        String upper = value == null ? "" : value.trim().toUpperCase();
        if (MEAT_TYPES.contains(upper)) {
            return upper;
        }
        return switch (upper) {
            case "荤", "荤菜", "肉" -> "MEAT";
            case "素", "素菜" -> "VEG";
            case "汤", "汤羹" -> "SOUP";
            case "主食", "面", "饭" -> "STAPLE";
            case "甜点", "甜品" -> "DESSERT";
            default -> "OTHER";
        };
    }

    private String clampText(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }
}
