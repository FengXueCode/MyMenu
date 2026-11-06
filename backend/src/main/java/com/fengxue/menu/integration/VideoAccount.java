package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_account")
public class VideoAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String platform;

    private String platformUid;

    private String nickname;

    private String avatar;

    /** AES-256-GCM 加密后的 Cookie JSON，绝不返回前端 */
    private String cookieCipher;

    private String status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
