# menu-backend

Menu · 灶台手账 的后端：Spring Boot 4 + Java 21 + MyBatis-Plus + MySQL + Flyway，
AI 调用全部由本服务代理，API Key 在本地加密存储。

完整启动说明见仓库根目录 [README](../README.md)。

## 本地运行

```bash
# 1. 建库（只需一次）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS recipe_menu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 2. 启动（Windows PowerShell）
$env:JAVA_HOME='D:\jdk-21.0.12.1'
mvn spring-boot:run
```

- 默认端口：8080
- 健康检查：<http://localhost:8080/api/health>
- Swagger：<http://localhost:8080/swagger-ui.html>
- 表结构由 Flyway 自动执行 `src/main/resources/db/migration/V1__init.sql`
- 首次启动自动播种：8 个分类、20 道示例菜谱、演示账号 `demo / 123456`

## 配置

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | localhost / 3306 / recipe_menu | MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | root / FengXue@ | 本地默认，可用环境变量覆盖 |
| `REDIS_*` | localhost:6379 | 可选，不可用时自动降级 |
| `JWT_SECRET_FILE` | data/jwt.secret | 首次启动生成，已 gitignore |
| `AI_KEY_MASTER_FILE` | data/secret.key | AI Key 加密主密钥，首次启动生成，已 gitignore |
| `UPLOAD_DIR` | data/uploads | AI 配图目录 |

## 安全要点

- 密码 BCrypt；JWT HS256，密钥本地文件保存
- AI Key 使用 AES-256-GCM 加密后入库，接口只返回 `hasKey` 与掩码
- 视频导入仅允许 B 站/抖音域名，逐跳校验重定向、拦截内网地址、限制超时与响应大小
- 日志不记录 Key 与请求体，全局异常不泄露内部堆栈

## 测试

```bash
mvn test          # 33 个单元测试
mvn -DskipTests package
```
