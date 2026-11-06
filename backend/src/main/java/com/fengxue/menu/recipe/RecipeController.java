package com.fengxue.menu.recipe;

import com.fengxue.menu.common.PageResult;
import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public Result<PageResult<RecipeDtos.RecipeCardVO>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "12") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String meatType,
            @RequestParam(defaultValue = "false") boolean mine) {
        return Result.success(recipeService.list(CurrentUser.require(), pageNum, pageSize,
                keyword, categoryId, meatType, mine));
    }

    @GetMapping("/{id}")
    public Result<RecipeDtos.RecipeDetailVO> detail(@PathVariable Long id) {
        return Result.success(recipeService.detail(CurrentUser.require(), id));
    }

    @PostMapping
    public Result<RecipeDtos.RecipeDetailVO> create(
            @Valid @RequestBody RecipeDtos.RecipeUpsertRequest request) {
        return Result.success(recipeService.create(CurrentUser.require(), request));
    }

    @PutMapping("/{id}")
    public Result<RecipeDtos.RecipeDetailVO> update(
            @PathVariable Long id, @Valid @RequestBody RecipeDtos.RecipeUpsertRequest request) {
        return Result.success(recipeService.update(CurrentUser.require(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipeService.delete(CurrentUser.require(), id);
        return Result.success();
    }
}
