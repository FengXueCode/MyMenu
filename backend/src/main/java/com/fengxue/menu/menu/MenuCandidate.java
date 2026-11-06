package com.fengxue.menu.menu;

import java.util.List;

/**
 * 随机菜单的候选菜谱（含用于过滤的配料名与用于缩放的基准用量）。
 */
public record MenuCandidate(
        Long id,
        String title,
        String coverUrl,
        String meatType,
        int difficulty,
        int servings,
        int cookMinutes,
        int spiceLevel,
        Long categoryId,
        List<String> tags,
        List<String> ingredientNames,
        List<MenuPlan.IngredientLine> baseIngredients,
        int lastCookedDays) {
}
