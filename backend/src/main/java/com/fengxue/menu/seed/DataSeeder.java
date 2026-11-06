package com.fengxue.menu.seed;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.category.Category;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.mapper.CategoryMapper;
import com.fengxue.menu.mapper.RecipeIngredientMapper;
import com.fengxue.menu.mapper.RecipeMapper;
import com.fengxue.menu.mapper.RecipeStepMapper;
import com.fengxue.menu.mapper.UserMapper;
import com.fengxue.menu.preference.PreferenceService;
import com.fengxue.menu.recipe.CoverService;
import com.fengxue.menu.recipe.Recipe;
import com.fengxue.menu.recipe.RecipeIngredient;
import com.fengxue.menu.recipe.RecipeStep;
import com.fengxue.menu.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动播种：系统分类、演示账号、示例菜谱。幂等，重启不会重复插入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final List<CategorySeed> CATEGORIES = List.of(
            new CategorySeed("家常菜", "🍳", "#D8503C", 1),
            new CategorySeed("川菜", "🌶️", "#B03A2E", 2),
            new CategorySeed("粤菜", "🥢", "#4C7A4F", 3),
            new CategorySeed("快手菜", "⚡", "#E7B24A", 4),
            new CategorySeed("汤羹", "🍲", "#7B4B3A", 5),
            new CategorySeed("主食", "🍚", "#B5651D", 6),
            new CategorySeed("早餐", "🥟", "#5B6C8F", 7),
            new CategorySeed("甜点", "🍰", "#8C4A5A", 8));

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final PreferenceService preferenceService;
    private final PasswordEncoder passwordEncoder;
    private final CoverService coverService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Long> categoryIds = seedCategories();
        seedDemoUser();
        seedRecipes(categoryIds);
    }

    private Map<String, Long> seedCategories() {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (CategorySeed seed : CATEGORIES) {
            Category existing = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
                    .eq(Category::getUserId, 0L)
                    .eq(Category::getName, seed.name())
                    .last("limit 1"));
            if (existing == null) {
                Category category = new Category();
                category.setUserId(0L);
                category.setName(seed.name());
                category.setIcon(seed.icon());
                category.setColor(seed.color());
                category.setSort(seed.sort());
                category.setIsSystem(1);
                category.setCreatedAt(LocalDateTime.now());
                categoryMapper.insert(category);
                ids.put(seed.name(), category.getId());
                log.info("播种系统分类: {}", seed.name());
            } else {
                ids.put(seed.name(), existing.getId());
            }
        }
        return ids;
    }

    private void seedDemoUser() {
        User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, "demo").last("limit 1"));
        if (existing != null) {
            return;
        }
        User user = new User();
        user.setUsername("demo");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setNickname("小灶");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        preferenceService.createDefault(user.getId());
        log.info("播种演示账号: demo / 123456");
    }

    private void seedRecipes(Map<String, Long> categoryIds) {
        Long count = recipeMapper.selectCount(Wrappers.<Recipe>lambdaQuery()
                .eq(Recipe::getUserId, 0L));
        if (count != null && count > 0) {
            return;
        }
        List<SeedRecipe> seeds;
        try {
            ClassPathResource resource = new ClassPathResource("seed/recipes.json");
            seeds = List.of(JsonSupport.MAPPER.readValue(resource.getInputStream(), SeedRecipe[].class));
        } catch (Exception e) {
            log.warn("示例菜谱加载失败，跳过播种: {}", e.getMessage());
            return;
        }
        Map<String, Long> ids = new HashMap<>(categoryIds);
        int inserted = 0;
        for (SeedRecipe seed : seeds) {
            Recipe recipe = new Recipe();
            recipe.setUserId(0L);
            recipe.setCategoryId(ids.get(seed.category()));
            recipe.setTitle(seed.title());
            recipe.setDescription(seed.description());
            recipe.setDurationMin(Math.max(5, seed.durationMin()));
            recipe.setDifficulty(Math.max(1, Math.min(3, seed.difficulty())));
            recipe.setServings(Math.max(1, Math.min(10, seed.servings())));
            recipe.setMeatType(seed.meatType());
            recipe.setSpiceLevel(Math.max(0, Math.min(3, seed.spiceLevel())));
            recipe.setTagsJson(seed.tags() == null || seed.tags().isEmpty()
                    ? null : JsonSupport.toJson(seed.tags()));
            recipe.setAiSummary(seed.description());
            recipe.setSummaryGeneratedAt(LocalDateTime.now());
            recipe.setCoverUrl(coverService.generate(seed.title(), seed.meatType(),
                    seed.tags() == null ? null : String.join(" / ", seed.tags())));
            recipe.setCreatedAt(LocalDateTime.now());
            recipe.setUpdatedAt(LocalDateTime.now());
            recipe.setDeleted(0);
            recipeMapper.insert(recipe);

            if (seed.ingredients() != null) {
                int sort = 0;
                for (SeedRecipe.Ingredient ingredient : seed.ingredients()) {
                    RecipeIngredient entity = new RecipeIngredient();
                    entity.setRecipeId(recipe.getId());
                    entity.setName(ingredient.name());
                    entity.setAmount(ingredient.amount() == null ? "" : ingredient.amount());
                    entity.setUnit(ingredient.unit() == null ? "" : ingredient.unit());
                    entity.setSort(sort++);
                    ingredientMapper.insert(entity);
                }
            }
            if (seed.steps() != null) {
                int stepNo = 1;
                for (String content : seed.steps()) {
                    RecipeStep entity = new RecipeStep();
                    entity.setRecipeId(recipe.getId());
                    entity.setStepNo(stepNo++);
                    entity.setContent(content);
                    stepMapper.insert(entity);
                }
            }
            inserted++;
        }
        log.info("播种示例菜谱 {} 道", inserted);
    }

    private record CategorySeed(String name, String icon, String color, int sort) {
    }
}
