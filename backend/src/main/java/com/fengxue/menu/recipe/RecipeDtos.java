package com.fengxue.menu.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜谱 DTO，字段与 docs/api-contract.md 保持一致。
 */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record IngredientDto(Long id, @NotBlank(message = "配料名不能为空") String name,
                                String amount, String unit, String category, Integer sort) {
    }

    public record StepDto(Long id, Integer stepNo, @NotBlank(message = "步骤内容不能为空") String content,
                          Integer durationSec) {
    }

    public record RecipeUpsertRequest(
            @NotBlank(message = "菜谱名不能为空")
            @Size(max = 100, message = "菜谱名最多 100 字")
            String title,
            @Size(max = 500, message = "描述最多 500 字") String description,
            Long categoryId,
            String coverUrl,
            String sourcePlatform,
            @Size(max = 500) String sourceUrl,
            @Size(max = 100) String sourceAuthor,
            Integer durationMin,
            Integer difficulty,
            Integer servings,
            String meatType,
            Integer spiceLevel,
            List<String> tags,
            @Size(max = 40, message = "配料最多 40 项") List<IngredientDto> ingredients,
            @Size(max = 40, message = "步骤最多 40 步") List<StepDto> steps) {
    }

    public record RecipeCardVO(
            Long id, String title, String description, String coverUrl,
            Long categoryId, String categoryName, String meatType, Integer difficulty,
            Integer servings, Integer durationMin, Integer spiceLevel, List<String> tags,
            String aiSummary, String sourcePlatform, String sourceUrl, String sourceAuthor,
            String owner, LocalDateTime createdAt) {
    }

    public record RecipeDetailVO(
            Long id, String title, String description, String coverUrl,
            Long categoryId, String categoryName, String meatType, Integer difficulty,
            Integer servings, Integer durationMin, Integer spiceLevel, List<String> tags,
            String aiSummary, String sourcePlatform, String sourceUrl, String sourceAuthor,
            String owner, LocalDateTime createdAt,
            List<IngredientDto> ingredients, List<StepDto> steps) {
    }
}
