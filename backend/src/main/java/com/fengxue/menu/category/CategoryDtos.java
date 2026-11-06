package com.fengxue.menu.category;

import java.util.List;

/**
 * 分类 DTO。
 */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryVO(Long id, String name, String icon, String color, int sort, boolean isSystem) {
    }

    public record CategoryInput(String name, String icon, String color) {
    }

    public record CategoryList(List<CategoryVO> items) {
    }
}
