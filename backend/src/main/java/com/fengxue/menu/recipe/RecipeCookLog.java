package com.fengxue.menu.recipe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe_cook_log")
public class RecipeCookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipeId;

    private Long userId;

    private Integer durationSeconds;

    private LocalDateTime createdAt;
}
