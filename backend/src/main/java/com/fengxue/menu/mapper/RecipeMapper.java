package com.fengxue.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengxue.menu.recipe.Recipe;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
