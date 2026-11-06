package com.fengxue.menu.recipe;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fengxue.menu.category.Category;
import com.fengxue.menu.common.PageResult;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.mapper.CategoryMapper;
import com.fengxue.menu.mapper.RecipeIngredientMapper;
import com.fengxue.menu.mapper.RecipeMapper;
import com.fengxue.menu.mapper.RecipeStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final Set<String> MEAT_TYPES =
            Set.of("MEAT", "VEG", "SOUP", "STAPLE", "DESSERT", "OTHER");

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final CategoryMapper categoryMapper;
    private final CoverService coverService;

    public PageResult<RecipeDtos.RecipeCardVO> list(Long userId, long pageNum, long pageSize,
                                                    String keyword, Long categoryId,
                                                    String meatType, boolean mine) {
        long safePage = Math.max(1, pageNum);
        long safeSize = Math.min(50, Math.max(1, pageSize));
        var query = Wrappers.<Recipe>lambdaQuery();
        if (mine) {
            query.eq(Recipe::getUserId, userId);
        } else {
            query.and(w -> w.eq(Recipe::getUserId, 0L).or().eq(Recipe::getUserId, userId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(w -> w.like(Recipe::getTitle, kw).or().like(Recipe::getDescription, kw));
        }
        if (categoryId != null) {
            query.eq(Recipe::getCategoryId, categoryId);
        }
        if (meatType != null && !meatType.isBlank()) {
            query.eq(Recipe::getMeatType, meatType);
        }
        query.orderByDesc(Recipe::getId);
        Page<Recipe> page = recipeMapper.selectPage(new Page<>(safePage, safeSize), query);
        Map<Long, String> categoryNames = categoryNameMap();
        List<RecipeDtos.RecipeCardVO> records = page.getRecords().stream()
                .map(recipe -> toCard(recipe, categoryNames))
                .toList();
        return PageResult.of(records, page.getTotal(), safePage, safeSize);
    }

    public RecipeDtos.RecipeDetailVO detail(Long userId, Long id) {
        Recipe recipe = requireVisible(userId, id);
        return toDetail(recipe);
    }

    @Transactional
    public RecipeDtos.RecipeDetailVO create(Long userId, RecipeDtos.RecipeUpsertRequest request) {
        validate(request);
        validateCategory(userId, request.categoryId());
        Recipe recipe = new Recipe();
        recipe.setUserId(userId);
        applyRequest(recipe, request);
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setDeleted(0);
        if (recipe.getCoverUrl() == null || recipe.getCoverUrl().isBlank()) {
            recipe.setCoverUrl(coverService.generate(recipe.getTitle(), recipe.getMeatType(),
                    request.tags() == null ? null : String.join(" / ", request.tags())));
        }
        recipeMapper.insert(recipe);
        saveRelations(recipe.getId(), request);
        return toDetail(recipe);
    }

    @Transactional
    public RecipeDtos.RecipeDetailVO update(Long userId, Long id, RecipeDtos.RecipeUpsertRequest request) {
        validate(request);
        validateCategory(userId, request.categoryId());
        Recipe recipe = requireOwned(userId, id);
        applyRequest(recipe, request);
        recipe.setUpdatedAt(LocalDateTime.now());
        if (recipe.getCoverUrl() == null || recipe.getCoverUrl().isBlank()) {
            recipe.setCoverUrl(coverService.generate(recipe.getTitle(), recipe.getMeatType(),
                    request.tags() == null ? null : String.join(" / ", request.tags())));
        }
        recipeMapper.updateById(recipe);
        ingredientMapper.delete(Wrappers.<RecipeIngredient>lambdaQuery()
                .eq(RecipeIngredient::getRecipeId, recipe.getId()));
        stepMapper.delete(Wrappers.<RecipeStep>lambdaQuery()
                .eq(RecipeStep::getRecipeId, recipe.getId()));
        saveRelations(recipe.getId(), request);
        return toDetail(recipe);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        recipeMapper.deleteById(id);
    }

    public Recipe requireVisible(Long userId, Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ResultCode.RECIPE_NOT_FOUND);
        }
        if (recipe.getUserId() != 0L && !recipe.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPE_NOT_FOUND);
        }
        return recipe;
    }

    public Recipe requireOwned(Long userId, Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ResultCode.RECIPE_NOT_FOUND);
        }
        if (recipe.getUserId() == 0L) {
            throw new BusinessException(ResultCode.RECIPE_FORBIDDEN, "系统示例菜谱不可修改");
        }
        if (!recipe.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECIPE_FORBIDDEN);
        }
        return recipe;
    }

    public List<Recipe> listVisibleRecipes(Long userId) {
        return recipeMapper.selectList(Wrappers.<Recipe>lambdaQuery()
                .and(w -> w.eq(Recipe::getUserId, 0L).or().eq(Recipe::getUserId, userId))
                .orderByDesc(Recipe::getId));
    }

    public Map<Long, List<RecipeIngredient>> ingredientsByRecipeIds(List<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Map.of();
        }
        return ingredientMapper.selectList(Wrappers.<RecipeIngredient>lambdaQuery()
                        .in(RecipeIngredient::getRecipeId, recipeIds)
                        .orderByAsc(RecipeIngredient::getSort)
                        .orderByAsc(RecipeIngredient::getId))
                .stream()
                .collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
    }

    public List<RecipeIngredient> ingredientsOf(Long recipeId) {
        return ingredientMapper.selectList(Wrappers.<RecipeIngredient>lambdaQuery()
                .eq(RecipeIngredient::getRecipeId, recipeId)
                .orderByAsc(RecipeIngredient::getSort)
                .orderByAsc(RecipeIngredient::getId));
    }

    public void updateSummary(Long recipeId, String summary) {
        Recipe recipe = new Recipe();
        recipe.setId(recipeId);
        recipe.setAiSummary(summary);
        recipe.setSummaryGeneratedAt(LocalDateTime.now());
        recipeMapper.updateById(recipe);
    }

    public void updateCover(Long recipeId, String coverUrl) {
        Recipe recipe = new Recipe();
        recipe.setId(recipeId);
        recipe.setCoverUrl(coverUrl);
        recipeMapper.updateById(recipe);
    }

    public RecipeDtos.RecipeDetailVO toDetail(Recipe recipe) {
        List<RecipeIngredient> ingredients = ingredientsOf(recipe.getId());
        List<RecipeStep> steps = stepMapper.selectList(Wrappers.<RecipeStep>lambdaQuery()
                .eq(RecipeStep::getRecipeId, recipe.getId())
                .orderByAsc(RecipeStep::getStepNo));
        Map<Long, String> names = categoryNameMap();
        return new RecipeDtos.RecipeDetailVO(
                recipe.getId(), recipe.getTitle(), recipe.getDescription(), recipe.getCoverUrl(),
                recipe.getCategoryId(), names.get(recipe.getCategoryId()), recipe.getMeatType(),
                recipe.getDifficulty(), recipe.getServings(), recipe.getDurationMin(),
                recipe.getSpiceLevel(), parseTags(recipe.getTagsJson()), recipe.getAiSummary(),
                recipe.getSourcePlatform(), recipe.getSourceUrl(), recipe.getSourceAuthor(),
                owner(recipe.getUserId()), recipe.getCreatedAt(),
                ingredients.stream().map(i -> new RecipeDtos.IngredientDto(
                        i.getId(), i.getName(), i.getAmount(), i.getUnit(), i.getCategory(), i.getSort()))
                        .toList(),
                steps.stream().map(s -> new RecipeDtos.StepDto(
                        s.getId(), s.getStepNo(), s.getContent(), s.getDurationSec()))
                        .toList());
    }

    private RecipeDtos.RecipeCardVO toCard(Recipe recipe, Map<Long, String> categoryNames) {
        return new RecipeDtos.RecipeCardVO(
                recipe.getId(), recipe.getTitle(), recipe.getDescription(), recipe.getCoverUrl(),
                recipe.getCategoryId(), categoryNames.get(recipe.getCategoryId()), recipe.getMeatType(),
                recipe.getDifficulty(), recipe.getServings(), recipe.getDurationMin(),
                recipe.getSpiceLevel(), parseTags(recipe.getTagsJson()), recipe.getAiSummary(),
                recipe.getSourcePlatform(), recipe.getSourceUrl(), recipe.getSourceAuthor(),
                owner(recipe.getUserId()), recipe.getCreatedAt());
    }

    private void applyRequest(Recipe recipe, RecipeDtos.RecipeUpsertRequest request) {
        recipe.setTitle(request.title().trim());
        recipe.setDescription(trimToNull(request.description()));
        recipe.setCategoryId(request.categoryId());
        recipe.setCoverUrl(trimToNull(request.coverUrl()));
        recipe.setSourcePlatform(trimToNull(request.sourcePlatform()));
        recipe.setSourceUrl(trimToNull(request.sourceUrl()));
        recipe.setSourceAuthor(trimToNull(request.sourceAuthor()));
        recipe.setDurationMin(defaultInt(request.durationMin(), 30));
        recipe.setDifficulty(clamp(defaultInt(request.difficulty(), 1), 1, 3));
        recipe.setServings(clamp(defaultInt(request.servings(), 2), 1, 10));
        recipe.setMeatType(normalizeMeatType(request.meatType()));
        recipe.setSpiceLevel(clamp(defaultInt(request.spiceLevel(), 0), 0, 3));
        List<String> tags = request.tags() == null ? List.of()
                : request.tags().stream().filter(Objects::nonNull).map(String::trim)
                .filter(t -> !t.isBlank()).distinct().limit(10).toList();
        recipe.setTagsJson(tags.isEmpty() ? null : JsonSupport.toJson(tags));
    }

    private void saveRelations(Long recipeId, RecipeDtos.RecipeUpsertRequest request) {
        if (request.ingredients() != null) {
            int sort = 0;
            for (RecipeDtos.IngredientDto dto : request.ingredients()) {
                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipeId(recipeId);
                ingredient.setName(dto.name().trim());
                ingredient.setAmount(dto.amount() == null ? "" : dto.amount().trim());
                ingredient.setUnit(dto.unit() == null ? "" : dto.unit().trim());
                ingredient.setCategory(trimToNull(dto.category()));
                ingredient.setSort(dto.sort() == null ? sort : dto.sort());
                ingredientMapper.insert(ingredient);
                sort++;
            }
        }
        if (request.steps() != null) {
            int stepNo = 1;
            for (RecipeDtos.StepDto dto : request.steps()) {
                RecipeStep step = new RecipeStep();
                step.setRecipeId(recipeId);
                step.setStepNo(stepNo++);
                step.setContent(dto.content().trim());
                step.setDurationSec(dto.durationSec());
                stepMapper.insert(step);
            }
        }
    }

    private void validate(RecipeDtos.RecipeUpsertRequest request) {
        if (request.ingredients() != null) {
            for (RecipeDtos.IngredientDto dto : request.ingredients()) {
                if (dto.name() == null || dto.name().isBlank()) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "配料名不能为空");
                }
            }
        }
        if (request.steps() != null) {
            for (RecipeDtos.StepDto dto : request.steps()) {
                if (dto.content() == null || dto.content().isBlank()) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "步骤内容不能为空");
                }
            }
        }
    }

    private void validateCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || (category.getUserId() != 0L && !category.getUserId().equals(userId))) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> names = new LinkedHashSet<>(categoryMapper.selectList(null)).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return names;
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            String[] tags = JsonSupport.fromJson(json, String[].class);
            return List.of(tags);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizeMeatType(String meatType) {
        if (meatType == null || !MEAT_TYPES.contains(meatType)) {
            return "OTHER";
        }
        return meatType;
    }

    private String owner(Long userId) {
        return userId != null && userId == 0L ? "system" : "mine";
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
