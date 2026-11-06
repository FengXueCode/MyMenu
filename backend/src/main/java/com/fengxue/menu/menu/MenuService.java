package com.fengxue.menu.menu;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.mapper.DailyMenuMapper;
import com.fengxue.menu.preference.PreferenceDtos;
import com.fengxue.menu.preference.PreferenceService;
import com.fengxue.menu.recipe.Recipe;
import com.fengxue.menu.recipe.RecipeIngredient;
import com.fengxue.menu.recipe.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final PreferenceService preferenceService;
    private final RecipeService recipeService;
    private final DailyMenuMapper dailyMenuMapper;
    private final MenuGenerator menuGenerator;

    public MenuPlan today(Long userId, String date, boolean regenerate) {
        LocalDate day = parseDate(date);
        int seq = regenerate ? (int) (System.currentTimeMillis() / 1000 % 90_000) + 1 : 0;
        return build(userId, day, seq);
    }

    public MenuPlan reroll(Long userId, MenuDtos.RerollRequest request) {
        LocalDate day = parseDate(request.date());
        PreferenceDtos.PreferencesDto prefs = preferenceService.get(userId);
        MenuPlan plan = build(userId, day, request.seq());
        return menuGenerator.reroll(plan, prefs, loadCandidates(userId), request.mealKey(), request.index());
    }

    @Transactional
    public void save(Long userId, MenuDtos.SaveRequest request) {
        if (request.plan() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "菜单内容不能为空");
        }
        LocalDate day = parseDate(request.date());
        MenuPlan plan = request.plan();
        plan.setDate(day.toString());
        String payload = JsonSupport.toJson(plan);

        DailyMenu existing = dailyMenuMapper.selectOne(Wrappers.<DailyMenu>lambdaQuery()
                .eq(DailyMenu::getUserId, userId)
                .eq(DailyMenu::getMenuDate, day));
        if (existing == null) {
            DailyMenu entity = new DailyMenu();
            entity.setUserId(userId);
            entity.setMenuDate(day);
            entity.setRegenerateSeq(plan.getSeq());
            entity.setPayloadJson(payload);
            dailyMenuMapper.insert(entity);
        } else {
            existing.setRegenerateSeq(plan.getSeq());
            existing.setPayloadJson(payload);
            dailyMenuMapper.updateById(existing);
        }
    }

    private MenuPlan build(Long userId, LocalDate day, int seq) {
        PreferenceDtos.PreferencesDto prefs = preferenceService.get(userId);
        return menuGenerator.generate(userId, day.toString(), seq, prefs, loadCandidates(userId));
    }

    private List<MenuCandidate> loadCandidates(Long userId) {
        List<Recipe> recipes = recipeService.listVisibleRecipes(userId);
        if (recipes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = recipes.stream().map(Recipe::getId).toList();
        Map<Long, List<RecipeIngredient>> ingredientMap = recipeService.ingredientsByRecipeIds(ids);
        Map<Long, Integer> lastCooked = lastCookedDays(userId);

        List<MenuCandidate> candidates = new ArrayList<>();
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> ingredients = ingredientMap.getOrDefault(recipe.getId(), List.of());
            List<MenuPlan.IngredientLine> baseIngredients = ingredients.stream()
                    .map(i -> new MenuPlan.IngredientLine(i.getName(), i.getAmount(), i.getUnit()))
                    .toList();
            List<String> ingredientNames = ingredients.stream()
                    .map(RecipeIngredient::getName).toList();
            candidates.add(new MenuCandidate(
                    recipe.getId(), recipe.getTitle(), recipe.getCoverUrl(), recipe.getMeatType(),
                    recipe.getDifficulty() == null ? 1 : recipe.getDifficulty(),
                    recipe.getServings() == null ? 2 : recipe.getServings(),
                    recipe.getDurationMin() == null ? 30 : recipe.getDurationMin(),
                    recipe.getSpiceLevel() == null ? 0 : recipe.getSpiceLevel(),
                    recipe.getCategoryId(), List.of(), ingredientNames, baseIngredients,
                    lastCooked.getOrDefault(recipe.getId(), -1)));
        }
        return candidates;
    }

    private Map<Long, Integer> lastCookedDays(Long userId) {
        LocalDate today = LocalDate.now();
        List<DailyMenu> history = dailyMenuMapper.selectList(Wrappers.<DailyMenu>lambdaQuery()
                .eq(DailyMenu::getUserId, userId)
                .ge(DailyMenu::getMenuDate, today.minusDays(7)));
        Map<Long, Integer> result = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (DailyMenu menu : history) {
            try {
                MenuPlan plan = JsonSupport.fromJson(menu.getPayloadJson(), MenuPlan.class);
                int days = (int) java.time.temporal.ChronoUnit.DAYS.between(menu.getMenuDate(), today);
                for (MenuPlan.Meal meal : plan.getMeals()) {
                    for (MenuPlan.Dish dish : meal.getDishes()) {
                        if (dish.getRecipeId() != null && seen.add(dish.getRecipeId())) {
                            result.put(dish.getRecipeId(), Math.max(0, days));
                        }
                    }
                }
            } catch (Exception ignored) {
                // 历史数据损坏时跳过，不影响本次生成
            }
        }
        return result;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日期格式需为 yyyy-MM-dd");
        }
    }
}
