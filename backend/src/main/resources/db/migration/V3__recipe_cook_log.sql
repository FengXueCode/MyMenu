-- V3：菜谱实际烹饪计时记录
CREATE TABLE IF NOT EXISTS recipe_cook_log (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    recipe_id        BIGINT   NOT NULL,
    user_id          BIGINT   NOT NULL,
    duration_seconds INT      NOT NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_recipe_user (recipe_id, user_id),
    KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
