package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_import_item")
public class VideoImportItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sourceId;

    private String platform;

    private String videoId;

    private String videoUrl;

    private String title;

    private String coverUrl;

    private String author;

    private String description;

    /** PENDING / PROCESSING / IMPORTED / SKIPPED / FAILED */
    private String status;

    private Long recipeId;

    private String reason;

    private Integer attempts;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
