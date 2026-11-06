package com.fengxue.menu.category;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryDtos.CategoryVO>> list() {
        return Result.success(categoryService.list(CurrentUser.require()));
    }

    @PostMapping
    public Result<CategoryDtos.CategoryVO> create(@RequestBody CategoryDtos.CategoryInput input) {
        return Result.success(categoryService.create(CurrentUser.require(), input));
    }

    @PutMapping("/{id}")
    public Result<CategoryDtos.CategoryVO> update(@PathVariable Long id,
                                                  @RequestBody CategoryDtos.CategoryInput input) {
        return Result.success(categoryService.update(CurrentUser.require(), id, input));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(CurrentUser.require(), id);
        return Result.success();
    }
}
