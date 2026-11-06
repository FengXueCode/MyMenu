package com.fengxue.menu.category;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.mapper.CategoryMapper;
import com.fengxue.menu.mapper.RecipeMapper;
import com.fengxue.menu.recipe.Recipe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final RecipeMapper recipeMapper;

    public List<CategoryDtos.CategoryVO> list(Long userId) {
        List<Category> categories = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId))
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        return categories.stream().map(this::toVo).toList();
    }

    public Category requireVisible(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || (category.getUserId() != 0L && !category.getUserId().equals(userId))) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    public CategoryDtos.CategoryVO create(Long userId, CategoryDtos.CategoryInput input) {
        String name = validateName(input.name());
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setIcon(input.icon() == null || input.icon().isBlank() ? "🍽️" : input.icon().trim());
        category.setColor(input.color() == null || input.color().isBlank() ? "#D8503C" : input.color().trim());
        category.setSort(99);
        category.setIsSystem(0);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return toVo(category);
    }

    public CategoryDtos.CategoryVO update(Long userId, Long id, CategoryDtos.CategoryInput input) {
        Category category = requireOwned(userId, id);
        String name = validateName(input.name());
        category.setName(name);
        if (input.icon() != null && !input.icon().isBlank()) {
            category.setIcon(input.icon().trim());
        }
        if (input.color() != null && !input.color().isBlank()) {
            category.setColor(input.color().trim());
        }
        categoryMapper.updateById(category);
        return toVo(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        Long used = recipeMapper.selectCount(
                Wrappers.<Recipe>lambdaQuery().eq(Recipe::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BusinessException(ResultCode.CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
    }

    private Category requireOwned(Long userId, Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        if (category.getUserId() == 0L || !category.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己的分类");
        }
        return category;
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 16) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名最多 16 字");
        }
        return trimmed;
    }

    private CategoryDtos.CategoryVO toVo(Category category) {
        return new CategoryDtos.CategoryVO(
                category.getId(), category.getName(), category.getIcon(), category.getColor(),
                category.getSort() == null ? 0 : category.getSort(),
                category.getIsSystem() != null && category.getIsSystem() == 1);
    }
}
