package com.fengxue.menu.menu;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.preference.PreferenceDtos;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 随机今日菜单生成器（纯逻辑，方便单测）。
 *
 * <p>同一天、同一用户、同一 seq 生成结果稳定；荤素配额、忌口、时长、辣度先过滤，
 * 再按偏好菜系与“近 7 天是否做过”加权抽样。</p>
 */
@Component
public class MenuGenerator {

    private static final Set<String> BREAKFAST_TYPES = Set.of("STAPLE", "DESSERT", "OTHER");
    private static final Set<String> LUNCH_FALLBACK_TYPES = Set.of("MEAT", "VEG", "SOUP", "STAPLE", "OTHER");

    public MenuPlan generate(long userId, String date, int seq,
                             PreferenceDtos.PreferencesDto prefs, List<MenuCandidate> candidates) {
        MenuPlan plan = new MenuPlan();
        plan.setDate(date);
        plan.setSeq(seq);
        plan.setPeopleCount(prefs.peopleCount());
        plan.setMealsPerDay(prefs.mealsPerDay());

        List<MenuCandidate> eligible = candidates.stream()
                .filter(candidate -> eligible(candidate, prefs))
                .toList();
        if (eligible.isEmpty()) {
            plan.getWarnings().add("菜谱库为空或没有符合偏好的菜谱，先去添加几道菜吧");
            return plan;
        }

        Random random = new Random(seedOf(userId, date, seq));
        Set<Long> used = new HashSet<>();
        boolean[] relaxed = {false};

        for (MealDef mealDef : mealDefs(prefs.mealsPerDay())) {
            List<MenuPlan.Dish> dishes = new ArrayList<>();
            int slots = Math.max(1, prefs.dishesPerMeal());
            if (mealDef.breakfast()) {
                for (int i = 0; i < slots; i++) {
                    MenuCandidate candidate = pickByTypes(eligible, BREAKFAST_TYPES, used, random, prefs);
                    if (candidate == null) {
                        candidate = pickByTypes(eligible, Set.of(), used, random, prefs);
                        if (candidate != null) {
                            relaxed[0] = true;
                        }
                    }
                    if (candidate == null) {
                        break;
                    }
                    used.add(candidate.id());
                    dishes.add(toDish(candidate, prefs.peopleCount()));
                }
            } else {
                int soupCount = slots >= 3 && hasType(eligible, "SOUP") ? 1 : 0;
                int remaining = slots - soupCount;
                int totalRatio = Math.max(1, prefs.meatRatio() + prefs.vegRatio());
                int meatCount = Math.max(0, Math.min(remaining,
                        (int) Math.round(remaining * (double) prefs.meatRatio() / totalRatio)));
                int vegCount = remaining - meatCount;

                for (int i = 0; i < meatCount; i++) {
                    MenuCandidate candidate = pickOrRelax(eligible, "MEAT", used, random, prefs, relaxed);
                    if (candidate == null) {
                        break;
                    }
                    used.add(candidate.id());
                    dishes.add(toDish(candidate, prefs.peopleCount()));
                }
                for (int i = 0; i < vegCount; i++) {
                    MenuCandidate candidate = pickOrRelax(eligible, "VEG", used, random, prefs, relaxed);
                    if (candidate == null) {
                        break;
                    }
                    used.add(candidate.id());
                    dishes.add(toDish(candidate, prefs.peopleCount()));
                }
                if (soupCount > 0) {
                    MenuCandidate candidate = pickOrRelax(eligible, "SOUP", used, random, prefs, relaxed);
                    if (candidate != null) {
                        used.add(candidate.id());
                        dishes.add(toDish(candidate, prefs.peopleCount()));
                    }
                }
                while (dishes.size() < slots) {
                    MenuCandidate candidate = pickByTypes(eligible, LUNCH_FALLBACK_TYPES, used, random, prefs);
                    if (candidate == null) {
                        candidate = pickByTypes(eligible, Set.of(), used, random, prefs);
                    }
                    if (candidate == null) {
                        break;
                    }
                    used.add(candidate.id());
                    dishes.add(toDish(candidate, prefs.peopleCount()));
                }
            }
            if (!dishes.isEmpty()) {
                plan.getMeals().add(new MenuPlan.Meal(mealDef.key(), mealDef.label(), dishes));
            }
        }
        if (relaxed[0]) {
            plan.getWarnings().add("符合偏好的菜谱不足，已自动放宽部分条件");
        }
        int expectedDishes = plan.getMeals().size() * Math.max(1, prefs.dishesPerMeal());
        int actualDishes = plan.getMeals().stream().mapToInt(m -> m.getDishes().size()).sum();
        if (plan.getMeals().isEmpty()) {
            plan.getWarnings().add("当前偏好下没有可用的菜品，试着放宽烹饪时长或辣度");
        } else if (actualDishes < expectedDishes) {
            plan.getWarnings().add("符合偏好的菜谱不足，已尽量安排现有菜品");
        }
        return plan;
    }

    public MenuPlan reroll(MenuPlan plan, PreferenceDtos.PreferencesDto prefs,
                           List<MenuCandidate> candidates, String mealKey, int index) {
        MenuPlan.Meal meal = plan.getMeals().stream()
                .filter(m -> m.getKey().equals(mealKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PARAM_ERROR, "餐次不存在"));
        if (index < 0 || index >= meal.getDishes().size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "要替换的菜品不存在");
        }
        List<MenuCandidate> pool = candidates.stream()
                .filter(candidate -> eligible(candidate, prefs))
                .toList();
        Set<Long> used = new HashSet<>();
        for (MenuPlan.Meal m : plan.getMeals()) {
            for (MenuPlan.Dish dish : m.getDishes()) {
                used.add(dish.getRecipeId());
            }
        }
        MenuPlan.Dish current = meal.getDishes().get(index);
        Random random = new Random();
        MenuCandidate replacement = pick(pool, current.getMeatType(), used, random, prefs);
        if (replacement == null) {
            replacement = pick(pool, null, used, random, prefs);
        }
        if (replacement == null) {
            if (!plan.getWarnings().contains("暂时没有可替换的菜谱")) {
                plan.getWarnings().add("暂时没有可替换的菜谱");
            }
            return plan;
        }
        meal.getDishes().set(index, toDish(replacement, prefs.peopleCount()));
        plan.setSeq(plan.getSeq() + 1);
        return plan;
    }

    private MenuCandidate pickOrRelax(List<MenuCandidate> pool, String type, Set<Long> used,
                                      Random random, PreferenceDtos.PreferencesDto prefs,
                                      boolean[] relaxed) {
        MenuCandidate candidate = pick(pool, type, used, random, prefs);
        if (candidate == null && type != null) {
            candidate = pick(pool, null, used, random, prefs);
            if (candidate != null) {
                relaxed[0] = true;
            }
        }
        return candidate;
    }

    private MenuCandidate pick(List<MenuCandidate> pool, String meatType, Set<Long> used,
                               Random random, PreferenceDtos.PreferencesDto prefs) {
        List<MenuCandidate> available = pool.stream()
                .filter(candidate -> !used.contains(candidate.id()))
                .filter(candidate -> meatType == null || meatType.equals(candidate.meatType()))
                .toList();
        return weightedPick(available, random, prefs);
    }

    private MenuCandidate pickByTypes(List<MenuCandidate> pool, Set<String> types, Set<Long> used,
                                      Random random, PreferenceDtos.PreferencesDto prefs) {
        List<MenuCandidate> available = pool.stream()
                .filter(candidate -> !used.contains(candidate.id()))
                .filter(candidate -> types.isEmpty() || types.contains(candidate.meatType()))
                .toList();
        return weightedPick(available, random, prefs);
    }

    private MenuCandidate weightedPick(List<MenuCandidate> available, Random random,
                                       PreferenceDtos.PreferencesDto prefs) {
        if (available.isEmpty()) {
            return null;
        }
        Set<Long> preferred = prefs.preferredCategories() == null
                ? Set.of() : new HashSet<>(prefs.preferredCategories());
        double[] weights = new double[available.size()];
        double total = 0;
        for (int i = 0; i < available.size(); i++) {
            MenuCandidate candidate = available.get(i);
            double weight = 1.0;
            if (candidate.categoryId() != null && preferred.contains(candidate.categoryId())) {
                weight += 2.0;
            }
            if (candidate.lastCookedDays() < 0) {
                weight += 0.5;
            } else if (candidate.lastCookedDays() >= 7) {
                weight += 1.0;
            } else {
                weight += 0.1;
            }
            weights[i] = weight;
            total += weight;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < available.size(); i++) {
            roll -= weights[i];
            if (roll <= 0) {
                return available.get(i);
            }
        }
        return available.get(available.size() - 1);
    }

    private boolean eligible(MenuCandidate candidate, PreferenceDtos.PreferencesDto prefs) {
        if (candidate.cookMinutes() > prefs.maxCookMinutes()) {
            return false;
        }
        if (candidate.spiceLevel() > prefs.spiceLevel()) {
            return false;
        }
        List<String> avoid = prefs.avoidIngredients();
        if (avoid == null || avoid.isEmpty()) {
            return true;
        }
        String haystack = (candidate.title() + " "
                + String.join(" ", candidate.ingredientNames())).toLowerCase();
        for (String item : avoid) {
            if (item != null && !item.isBlank() && haystack.contains(item.trim().toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasType(List<MenuCandidate> pool, String type) {
        return pool.stream().anyMatch(candidate -> type.equals(candidate.meatType()));
    }

    private MenuPlan.Dish toDish(MenuCandidate candidate, int peopleCount) {
        MenuPlan.Dish dish = new MenuPlan.Dish();
        dish.setRecipeId(candidate.id());
        dish.setTitle(candidate.title());
        dish.setCoverUrl(candidate.coverUrl());
        dish.setMeatType(candidate.meatType());
        dish.setSpiceLevel(candidate.spiceLevel());
        dish.setCookMinutes(candidate.cookMinutes());
        dish.setBaseServings(candidate.servings());
        dish.setIngredients(PortionScaler.scale(candidate.baseIngredients(),
                candidate.servings(), peopleCount));
        return dish;
    }

    private long seedOf(long userId, String date, int seq) {
        return 31L * userId + (date == null ? 0 : date.hashCode()) * 131L + seq * 977L;
    }

    private List<MealDef> mealDefs(int mealsPerDay) {
        return switch (Math.max(1, Math.min(3, mealsPerDay))) {
            case 1 -> List.of(new MealDef("dinner", "晚餐", false));
            case 2 -> List.of(
                    new MealDef("lunch", "午餐", false),
                    new MealDef("dinner", "晚餐", false));
            default -> List.of(
                    new MealDef("breakfast", "早餐", true),
                    new MealDef("lunch", "午餐", false),
                    new MealDef("dinner", "晚餐", false));
        };
    }

    private record MealDef(String key, String label, boolean breakfast) {
    }
}
