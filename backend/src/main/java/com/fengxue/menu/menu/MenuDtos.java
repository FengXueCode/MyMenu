package com.fengxue.menu.menu;

import jakarta.validation.constraints.NotBlank;

/**
 * 今日菜单请求 DTO。
 */
public final class MenuDtos {

    private MenuDtos() {
    }

    public record RerollRequest(@NotBlank String date, int seq,
                                @NotBlank String mealKey, int index) {
    }

    public record SaveRequest(@NotBlank String date, MenuPlan plan) {
    }
}
