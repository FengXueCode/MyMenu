package com.fengxue.menu.ai;

import java.util.List;

/**
 * AI 模块 DTO，字段与 docs/api-contract.md 对齐。
 */
public final class AiDtos {

    private AiDtos() {
    }

    public record ProviderInput(
            String name,
            String providerType,
            String baseUrl,
            String chatModel,
            String imageModel,
            String apiKey,
            Boolean enabled,
            Boolean isDefault) {
    }

    public record ProviderVO(
            Long id, String name, String providerType, String baseUrl,
            String chatModel, String imageModel, boolean hasKey, String keyMasked,
            boolean enabled, boolean isDefault, Boolean lastTestOk) {
    }

    public record TestResult(boolean ok, String message, long latencyMs) {
    }

    public record Preset(String providerType, String name, String baseUrl,
                         String defaultChatModel, String defaultImageModel) {
    }

    public record ExtractRequest(
            String text,
            String title,
            String platform,
            String extraText) {
    }

    public record IngredientDraft(String name, String amount, String unit) {
    }

    public record RecipeDraft(
            String title, String description, String categoryName, String meatType,
            Integer difficulty, Integer servings, Integer cookMinutes, Integer spiceLevel,
            List<String> tags, List<IngredientDraft> ingredients, List<String> steps,
            String tips) {
    }

    public record SummaryResponse(String aiSummary, List<String> ingredientsBrief, String pairing) {
    }

    /** 收藏夹视频的菜谱识别结果。 */
    public record VideoRecipeDecision(boolean isRecipe, String reason, RecipeDraft recipe) {
    }

    public record ImageResponse(String coverUrl, boolean aiGenerated, String warning) {
    }
}
