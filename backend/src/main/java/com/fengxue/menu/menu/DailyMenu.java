package com.fengxue.menu.menu;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_menu")
public class DailyMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate menuDate;

    private Integer regenerateSeq;

    private String payloadJson;

    private LocalDateTime createdAt;
}
