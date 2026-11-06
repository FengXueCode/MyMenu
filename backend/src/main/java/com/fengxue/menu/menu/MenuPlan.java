package com.fengxue.menu.menu;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 今日菜单快照。字段与 docs/api-contract.md 对齐，会整体序列化进 daily_menu.payload_json。
 */
@Data
public class MenuPlan {

    private String date;
    private int seq;
    private int peopleCount;
    private int mealsPerDay;
    private List<Meal> meals = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    public static class Meal {
        private String key;
        private String label;
        private List<Dish> dishes = new ArrayList<>();

        public Meal() {
        }

        public Meal(String key, String label, List<Dish> dishes) {
            this.key = key;
            this.label = label;
            this.dishes = dishes;
        }
    }

    @Data
    public static class Dish {
        private Long recipeId;
        private String title;
        private String coverUrl;
        private String meatType;
        private int spiceLevel;
        private int cookMinutes;
        private int baseServings;
        private List<IngredientLine> ingredients = new ArrayList<>();

        public Dish() {
        }
    }

    @Data
    public static class IngredientLine {
        private String name;
        private String amount;
        private String unit;

        public IngredientLine() {
        }

        public IngredientLine(String name, String amount, String unit) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
        }
    }
}
