package com.fengxue.menu.preference;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 用餐偏好 DTO。
 */
public record PreferenceDtos() {

    public record PreferencesDto(
            @NotNull @Min(1) @Max(10) Integer peopleCount,
            @NotNull @Min(1) @Max(3) Integer mealsPerDay,
            @NotNull @Min(1) @Max(6) Integer dishesPerMeal,
            @NotNull @Min(0) @Max(10) Integer meatRatio,
            @NotNull @Min(0) @Max(10) Integer vegRatio,
            @NotNull @Min(15) @Max(240) Integer maxCookMinutes,
            @NotNull @Min(0) @Max(3) Integer spiceLevel,
            List<String> avoidIngredients,
            List<Long> preferredCategories) {
    }
}
