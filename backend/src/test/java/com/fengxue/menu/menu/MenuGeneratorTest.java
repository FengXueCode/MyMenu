package com.fengxue.menu.menu;

import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.preference.PreferenceDtos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuGeneratorTest {

    private final MenuGenerator generator = new MenuGenerator();

    private PreferenceDtos.PreferencesDto prefs(int mealsPerDay, int dishesPerMeal) {
        return new PreferenceDtos.PreferencesDto(
                4, mealsPerDay, dishesPerMeal, 2, 3, 90, 2,
                List.of("香菜"), List.of(10L));
    }

    private List<MenuCandidate> candidates() {
        List<MenuCandidate> list = new ArrayList<>();
        list.add(candidate(1, "红烧肉", "MEAT", 1, 60, 2, List.of("五花肉", "生姜")));
        list.add(candidate(2, "鱼香肉丝", "MEAT", 2, 30, 2, List.of("里脊肉", "木耳")));
        list.add(candidate(3, "宫保鸡丁", "MEAT", 3, 25, 2, List.of("鸡胸肉", "花生")));
        list.add(candidate(4, "香菜炒肉", "MEAT", 1, 20, 2, List.of("香菜", "肉")));
        list.add(candidate(5, "清炒时蔬", "VEG", 0, 10, 2, List.of("青菜")));
        list.add(candidate(6, "蒜蓉西兰花", "VEG", 0, 15, 2, List.of("西兰花")));
        list.add(candidate(7, "凉拌黄瓜", "VEG", 1, 10, 2, List.of("黄瓜")));
        list.add(candidate(8, "番茄蛋汤", "SOUP", 0, 15, 2, List.of("番茄", "鸡蛋")));
        list.add(candidate(9, "紫菜蛋花汤", "SOUP", 0, 10, 2, List.of("紫菜", "鸡蛋")));
        return list;
    }

    private MenuCandidate candidate(long id, String title, String meatType, int spice,
                                    int minutes, int servings, List<String> ingredients) {
        return new MenuCandidate(id, title, "/api/files/x.svg", meatType, 1, servings, minutes,
                spice, 10L, List.of("家常"), ingredients,
                List.of(new MenuPlan.IngredientLine("主料", "300", "g")), -1);
    }

    @Test
    void generatesStableMenuForSameSeed() {
        MenuPlan first = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());
        MenuPlan second = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());

        assertEquals(2, first.getMeals().size());
        assertEquals(3, first.getMeals().get(0).getDishes().size());
        assertEquals(JsonSupport.toJson(first), JsonSupport.toJson(second));
    }

    @Test
    void appliesMeatVegRatioAndSoup() {
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());
        for (MenuPlan.Meal meal : plan.getMeals()) {
            long meat = meal.getDishes().stream().filter(d -> "MEAT".equals(d.getMeatType())).count();
            long veg = meal.getDishes().stream().filter(d -> "VEG".equals(d.getMeatType())).count();
            long soup = meal.getDishes().stream().filter(d -> "SOUP".equals(d.getMeatType())).count();
            assertEquals(1, meat);
            assertEquals(1, veg);
            assertEquals(1, soup);
        }
    }

    @Test
    void filtersAvoidedIngredientsSpicyAndSlowDishes() {
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());
        for (MenuPlan.Meal meal : plan.getMeals()) {
            for (MenuPlan.Dish dish : meal.getDishes()) {
                assertFalse(dish.getTitle().contains("香菜"));
                assertNotEquals("宫保鸡丁", dish.getTitle());
                assertTrue(dish.getCookMinutes() <= 90);
            }
        }
    }

    @Test
    void scalesIngredientsToPeopleCount() {
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());
        MenuPlan.Dish dish = plan.getMeals().get(0).getDishes().get(0);
        assertEquals("600", dish.getIngredients().get(0).getAmount());
    }

    @Test
    void rerollReplacesDishAndIncrementsSeq() {
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(2, 3), candidates());
        MenuPlan before = JsonSupport.fromJson(JsonSupport.toJson(plan), MenuPlan.class);
        Long oldId = plan.getMeals().get(1).getDishes().get(0).getRecipeId();
        int oldSeq = plan.getSeq();

        MenuPlan rerolled = generator.reroll(plan, prefs(2, 3), candidates(), "dinner", 0);

        assertEquals(oldSeq + 1, rerolled.getSeq());
        assertNotEquals(oldId, rerolled.getMeals().get(1).getDishes().get(0).getRecipeId());
        assertEquals(before.getMeals().get(1).getDishes().size(),
                rerolled.getMeals().get(1).getDishes().size());
    }

    @Test
    void warnsWhenNotEnoughCandidates() {
        List<MenuCandidate> small = List.of(
                candidate(1, "可乐鸡翅", "MEAT", 1, 30, 2, List.of("鸡翅")));
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(1, 3), small);

        assertFalse(plan.getWarnings().isEmpty());
        assertTrue(plan.getMeals().stream().flatMap(m -> m.getDishes().stream()).count() <= 1);
    }

    @Test
    void emptyPoolReturnsFriendlyWarning() {
        MenuPlan plan = generator.generate(1L, "2026-11-06", 0, prefs(3, 3), List.of());

        assertTrue(plan.getMeals().isEmpty());
        assertFalse(plan.getWarnings().isEmpty());
    }
}
