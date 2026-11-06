package com.fengxue.menu.recipe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private String description;

    private String coverUrl;

    private String sourcePlatform;

    private String sourceUrl;

    private String sourceAuthor;

    private Integer durationMin;

    private Integer difficulty;

    private Integer servings;

    private String meatType;

    private Integer spiceLevel;

    private String tagsJson;

    private String aiSummary;

    private LocalDateTime summaryGeneratedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
