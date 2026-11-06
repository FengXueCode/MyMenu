package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorite_source")
public class FavoriteSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String platform;

    /** BILIBILI_FOLDER / DOUYIN_QUEUE */
    private String sourceType;

    private String folderId;

    private String folderName;

    private String sourceUrl;

    private Integer autoScan;

    private Integer scanLimit;

    private LocalDateTime lastScanAt;

    private String lastScanStatus;

    private String lastScanMessage;

    private Integer lastScanAdded;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
