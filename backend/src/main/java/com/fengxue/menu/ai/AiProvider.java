package com.fengxue.menu.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_provider")
public class AiProvider {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String providerType;

    private String baseUrl;

    private String chatModel;

    private String imageModel;

    /** AES-256-GCM 密文，绝不返回给前端。 */
    private String apiKeyCipher;

    /** 仅用于展示的掩码提示。 */
    private String apiKeyHint;

    private Integer enabled;

    private Integer isDefault;

    private Integer lastTestOk;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
