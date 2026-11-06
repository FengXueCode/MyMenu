-- Menu · 灶台手账 数据库初始化
-- MySQL 8.0+，字符集 utf8mb4
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(32)  NOT NULL DEFAULT '',
    avatar        VARCHAR(500) NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS category (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL DEFAULT 0,
    name       VARCHAR(32) NOT NULL,
    icon       VARCHAR(16) NOT NULL DEFAULT '🍽️',
    color      VARCHAR(16) NOT NULL DEFAULT '#D8503C',
    sort       INT         NOT NULL DEFAULT 0,
    is_system  TINYINT     NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_id, name),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS recipe (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    category_id         BIGINT       NULL,
    title               VARCHAR(100) NOT NULL,
    description         VARCHAR(500) NULL,
    cover_url           VARCHAR(500) NULL,
    source_platform     VARCHAR(20)  NULL,
    source_url          VARCHAR(500) NULL,
    source_author       VARCHAR(100) NULL,
    duration_min        INT          NOT NULL DEFAULT 30,
    difficulty          TINYINT      NOT NULL DEFAULT 1,
    servings            INT          NOT NULL DEFAULT 2,
    meat_type           VARCHAR(16)  NOT NULL DEFAULT 'OTHER',
    spice_level         TINYINT      NOT NULL DEFAULT 0,
    tags_json           VARCHAR(255) NULL,
    ai_summary          VARCHAR(500) NULL,
    summary_generated_at DATETIME    NULL,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_category (category_id),
    KEY idx_visible (user_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS recipe_ingredient (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT      NOT NULL,
    name      VARCHAR(64) NOT NULL,
    amount    VARCHAR(32) NOT NULL DEFAULT '',
    unit      VARCHAR(16) NOT NULL DEFAULT '',
    category  VARCHAR(32) NULL,
    sort      INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_recipe (recipe_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS recipe_step (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    recipe_id    BIGINT        NOT NULL,
    step_no      INT           NOT NULL,
    content      VARCHAR(1000) NOT NULL,
    duration_sec INT           NULL,
    PRIMARY KEY (id),
    KEY idx_recipe (recipe_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS user_preference (
    user_id                  BIGINT      NOT NULL,
    people_count             INT         NOT NULL DEFAULT 2,
    meals_per_day            INT         NOT NULL DEFAULT 3,
    dishes_per_meal          INT         NOT NULL DEFAULT 3,
    meat_ratio               INT         NOT NULL DEFAULT 3,
    veg_ratio                INT         NOT NULL DEFAULT 4,
    max_cook_minutes         INT         NOT NULL DEFAULT 90,
    spice_level              INT         NOT NULL DEFAULT 2,
    avoid_ingredients_json   VARCHAR(500) NULL,
    preferred_categories_json VARCHAR(500) NULL,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ai_provider (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    provider_type  VARCHAR(32)  NOT NULL DEFAULT 'custom',
    base_url       VARCHAR(300) NOT NULL,
    chat_model     VARCHAR(100) NOT NULL DEFAULT '',
    image_model    VARCHAR(100) NULL,
    api_key_cipher VARCHAR(1000) NULL,
    api_key_hint   VARCHAR(32)  NULL,
    enabled        TINYINT      NOT NULL DEFAULT 1,
    is_default     TINYINT      NOT NULL DEFAULT 0,
    last_test_ok   TINYINT      NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS daily_menu (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    menu_date      DATE        NOT NULL,
    regenerate_seq INT         NOT NULL DEFAULT 0,
    payload_json   MEDIUMTEXT  NOT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_date (user_id, menu_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
