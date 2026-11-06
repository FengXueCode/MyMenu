-- V2：视频账号 / 收藏夹自动扫描 / 待处理链接队列
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS video_account (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    platform       VARCHAR(20)  NOT NULL DEFAULT 'bilibili',
    platform_uid   VARCHAR(64)  NULL,
    nickname       VARCHAR(100) NULL,
    avatar         VARCHAR(500) NULL,
    cookie_cipher  TEXT         NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at  DATETIME     NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform (user_id, platform)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS favorite_source (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    platform         VARCHAR(20)  NOT NULL DEFAULT 'bilibili',
    source_type      VARCHAR(30)  NOT NULL DEFAULT 'BILIBILI_FOLDER',
    folder_id        VARCHAR(64)  NULL,
    folder_name      VARCHAR(200) NULL,
    source_url       VARCHAR(500) NULL,
    auto_scan        TINYINT      NOT NULL DEFAULT 1,
    scan_limit       INT          NOT NULL DEFAULT 20,
    last_scan_at     DATETIME     NULL,
    last_scan_status VARCHAR(20)  NULL,
    last_scan_message VARCHAR(500) NULL,
    last_scan_added  INT          NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_auto (auto_scan)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS video_import_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    source_id   BIGINT       NULL,
    platform    VARCHAR(20)  NOT NULL,
    video_id    VARCHAR(80)  NOT NULL,
    video_url   VARCHAR(500) NULL,
    title       VARCHAR(300) NULL,
    cover_url   VARCHAR(500) NULL,
    author      VARCHAR(100) NULL,
    description TEXT         NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    recipe_id   BIGINT       NULL,
    reason      VARCHAR(500) NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform_video (user_id, platform, video_id),
    KEY idx_status (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
