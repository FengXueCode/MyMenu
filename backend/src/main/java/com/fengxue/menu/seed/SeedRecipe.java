package com.fengxue.menu.seed;

import java.util.List;

/**
 * 示例菜谱 JSON 结构，对应 classpath:seed/recipes.json。
 */
public record SeedRecipe(
        String title,
        String description,
        String category,
        String meatType,
        int difficulty,
        int servings,
        int durationMin,
        int spiceLevel,
        List<String> tags,
        List<Ingredient> ingredients,
        List<String> steps,
        String tips) {

    public record Ingredient(String name, String amount, String unit) {
    }
}
