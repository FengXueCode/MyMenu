package com.fengxue.menu.preference;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private Integer peopleCount;

    private Integer mealsPerDay;

    private Integer dishesPerMeal;

    private Integer meatRatio;

    private Integer vegRatio;

    private Integer maxCookMinutes;

    private Integer spiceLevel;

    private String avoidIngredientsJson;

    private String preferredCategoriesJson;

    private LocalDateTime updatedAt;
}
