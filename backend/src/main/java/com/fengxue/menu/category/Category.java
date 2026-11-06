package com.fengxue.menu.category;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String icon;

    private String color;

    private Integer sort;

    private Integer isSystem;

    private LocalDateTime createdAt;
}
