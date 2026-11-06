package com.fengxue.menu.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortionScalerTest {

    private List<MenuPlan.IngredientLine> base() {
        return List.of(
                new MenuPlan.IngredientLine("五花肉", "300", "g"),
                new MenuPlan.IngredientLine("鸡蛋", "2", "个"),
                new MenuPlan.IngredientLine("生姜", "3", "片"),
                new MenuPlan.IngredientLine("盐", "适量", ""),
                new MenuPlan.IngredientLine("生抽", "1", "勺"));
    }

    @Test
    void scalesMassByPeopleCount() {
        List<MenuPlan.IngredientLine> scaled = PortionScaler.scale(base(), 2, 4);
        assertEquals("600", scaled.get(0).getAmount());
        assertEquals("g", scaled.get(0).getUnit());
    }

    @Test
    void scalesDiscreteUnitsUpwards() {
        List<MenuPlan.IngredientLine> scaled = PortionScaler.scale(base(), 2, 3);
        assertEquals("3", scaled.get(1).getAmount());
        assertEquals("个", scaled.get(1).getUnit());
        assertEquals("5", scaled.get(2).getAmount());
    }

    @Test
    void keepsAsYouLikeAmounts() {
        List<MenuPlan.IngredientLine> scaled = PortionScaler.scale(base(), 2, 5);
        assertEquals("适量", scaled.get(3).getAmount());
        assertEquals("", scaled.get(3).getUnit());
    }

    @Test
    void roundsMassToFiveGramSteps() {
        List<MenuPlan.IngredientLine> scaled = PortionScaler.scale(base(), 3, 4);
        assertEquals("400", scaled.get(0).getAmount());
    }

    @Test
    void handlesAmountWithEmbeddedUnit() {
        List<MenuPlan.IngredientLine> scaled = PortionScaler.scale(
                List.of(new MenuPlan.IngredientLine("面粉", "200g", "")), 2, 3);
        assertEquals("300", scaled.get(0).getAmount());
        assertEquals("g", scaled.get(0).getUnit());
    }
}
