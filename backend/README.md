# Spring Boot 后端模板（Java 21 + Spring Boot 4 + MyBatis-Plus + MySQL + Redis）

一个**单模块 Maven**、可直接编译运行的后端模板工程。包含统一返回体、全局异常处理、用户 CRUD/分页、MySQL/Redis 健康检查、springdoc-openapi v3、Logback 按天滚动、Docker 多阶段构建与 docker-compose 编排。

> 所有敏感信息均通过 `${ENV_VAR:default}` 读取，不在代码/配置中硬编码生产密码。鉴权按需求留空，没有引入 Spring Security、Sa-Token、MQ、Nacos 等额外组件。

## 1. 版本与技术选型

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | `maven.compiler.release=21` |
| Spring Boot | **4.0.9** | 4.0.x 最新补丁；使用 Spring Framework 7、Jackson 3、Tomcat 11 |
| 构建 | Maven 3.9+ | 单模块，无父工程 |
| MyBatis-Plus | **3.5.17** | 使用 `mybatis-plus-spring-boot4-starter`，与 Boot 4.0.x 对齐 |
| MySQL | 8.4 | 容器 `mysql:8.4`，字符集 `utf8mb4` |
| Redis | 7.4 | 容器 `redis:7.4`，Lettuce 客户端 |
| 接口文档 | **springdoc-openapi 3.2.0** | `springdoc-openapi-starter-webmvc-ui`，适配 Spring Boot 4 |
| 日志 | Logback | Spring Boot 默认，未替换为 Log4j2 |
| 数据库迁移 | Flyway 11.14.1 | 已引入依赖，默认关闭；见第 6 节 |

### 为什么 MyBatis-Plus 选 3.5.17 而不是 3.5.18？

`mybatis-plus-spring-boot4-starter:3.5.18` 的 POM 基于 **Spring Boot 4.1.0** 编译，而本模板固定使用 **Boot 4.0.x**；3.5.17 基于 Boot 4.0.1，版本线一致，兼容性更稳。

另外，3.5.17 起 MyBatis-Plus 做了模块拆分：
- `IService` / `ServiceImpl` 从 `com.baomidou.mybatisplus.extension.service.*` 移到了
  `com.baomidou.mybatisplus.spring.service.*`；
- `PaginationInnerInterceptor` 在 `mybatis-plus-jsqlparser` 模块，`ServiceImpl` 在 `mybatis-plus-extension` 模块；
- 因此 pom 中显式引入了 `mybatis-plus-extension` 和 `mybatis-plus-jsqlparser`，否则会出现“找不到 IService / PaginationInnerInterceptor”的编译错误。

### springdoc 3.2.0 与 Boot 4.0.9 的兼容性

`springdoc-openapi 3.2.0` 的父 POM 是 Boot 4.1.2，但本模板已在 **Boot 4.0.9 + JDK 21** 下实测：
- `/v3/api-docs` 返回 200；
- `/swagger-ui.html` 302 跳转到 `/swagger-ui/index.html`，页面正常；
- 应用可正常启动。

如果后续升级依赖出现 `NoSuchMethodError` 等二进制不兼容，按以下顺序处理：
1. 保持 springdoc 3.2.0，把 Boot 升到 4.1.x；或
2. 保持 Boot 4.0.x，把 springdoc 降到 3.0.3（3.0.x 是 Boot 4.0.x 对应线路）。

## 2. 目录结构

```text
.
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
├── .env.example
├── README.md
└── src/main
    ├── java/com/example/template
    │   ├── TemplateApplication.java
    │   ├── common
    │   │   ├── Result.java
    │   │   ├── ResultCode.java
    │   │   ├── PageResult.java
    │   │   ├── exception/BusinessException.java
    │   │   ├── exception/GlobalExceptionHandler.java
    │   │   └── util/RedisUtil.java
    │   ├── config
    │   │   ├── MybatisPlusConfig.java
    │   │   ├── MyMetaObjectHandler.java
    │   │   ├── RedisConfig.java
    │   │   ├── OpenApiConfig.java
    │   │   └── TraceIdFilter.java
    │   ├── controller
    │   │   ├── UserController.java
    │   │   └── HealthController.java
    │   ├── dto
    │   │   ├── UserCreateDTO.java
    │   │   ├── UserUpdateDTO.java
    │   │   ├── UserQueryDTO.java
    │   │   └── UserVO.java
    │   ├── entity/User.java
    │   ├── mapper/UserMapper.java
    │   └── service
    │       ├── UserService.java
    │       └── impl/UserServiceImpl.java
    └── resources
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        ├── logback-spring.xml
        └── db
            ├── init.sql
            └── migration/V1__init_user.sql
```

## 3. 本地开发启动

### 3.1 推荐：只启动 MySQL/Redis 容器，应用在 IDE 里跑

前置：JDK 21、Maven 3.9+、Docker Desktop（或 Docker Engine + Compose v2）。

```bash
# 1) 只启动依赖服务；不会启动 app
docker compose up -d mysql redis

# 2) 确认两个依赖都 healthy
docker compose ps

# 3) 本地启动应用（默认 profile 就是 dev）
mvn spring-boot:run

# 或者先打包再运行
mvn clean package -DskipTests
java -jar target/springboot-template.jar --spring.profiles.active=dev
```

访问：
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/api/health>

### 3.2 不用 Docker，使用本机 MySQL/Redis

1. 在本机 MySQL 8.4 中创建库 `template`（或改 `application-dev.yml` 的 `DB_NAME`）。
2. 执行 `src/main/resources/db/init.sql`：
   ```bash
   mysql -u root -p template < src/main/resources/db/init.sql
   ```
3. 确认本机 Redis 已启动；若密码不同，修改 `REDIS_PASSWORD` 环境变量或 `application-dev.yml`。
4. `mvn spring-boot:run`。

### 3.3 本地默认连接信息

| 项目 | 默认值 | 环境变量 |
| --- | --- | --- |
| MySQL 地址 | `localhost:3306` | `DB_HOST` / `DB_PORT` |
| MySQL 库 | `template` | `DB_NAME` |
| MySQL 用户 | `template` | `DB_USERNAME` |
| MySQL 密码 | `template123456` | `DB_PASSWORD` |
| Redis 地址 | `localhost:6379` | `REDIS_HOST` / `REDIS_PORT` |
| Redis 密码 | `redis123456` | `REDIS_PASSWORD` |
| 日志目录 | `logs` | `LOG_DIR` |
| 服务端口 | `8080` | `SERVER_PORT` |

## 4. 一键 docker-compose 部署

```bash
# 可选：复制环境变量文件，修改默认密码
cp .env.example .env

# 构建并后台启动 app + mysql + redis
docker compose up -d --build

# 查看编排状态
docker compose ps

# 查看应用日志（首次启动会等 mysql/redis healthy）
docker compose logs -f app
```

启动顺序由 `depends_on.condition: service_healthy` 保证：MySQL、Redis 都通过健康检查后，app 才会启动。

停止：

```bash
docker compose down          # 保留数据卷
docker compose down -v       # 删除数据卷，下次启动重新执行 init.sql
```

### 验证

```bash
curl -i http://localhost:8080/api/health
curl -i http://localhost:8080/api/health/mysql
curl -i http://localhost:8080/api/health/redis
```

正常时三个接口都返回 HTTP 200；任一依赖不可用时返回 HTTP 503，响应体仍是统一 `Result` 结构。

## 5. 接口清单与 curl 示例

统一返回体：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1762310400000
}
```

业务成功判断 `code == 200`；HTTP 状态码同时尽量与语义对齐（参数错误 400、资源不存在 404、冲突 409、依赖不可用 503、未处理异常 500）。

### 5.1 新增用户

```bash
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: demo-trace-001' \
  -d '{
    "username": "zhaoliu",
    "nickname": "赵六",
    "email": "zhaoliu@example.com",
    "age": 27,
    "status": 1
  }'
```

### 5.2 分页查询

```bash
curl 'http://localhost:8080/api/users?pageNum=1&pageSize=10&username=zhang&status=1'
```

### 5.3 查询详情 / 更新 / 删除

```bash
# 详情
curl 'http://localhost:8080/api/users/1'

# 更新（只更新传入的非 null 字段）
curl -X PUT 'http://localhost:8080/api/users/1' \
  -H 'Content-Type: application/json' \
  -d '{"nickname":"张三丰","age":30,"status":1}'

# 逻辑删除
curl -X DELETE 'http://localhost:8080/api/users/1'
```

### 5.4 参数校验示例

```bash
curl -i -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{"username":"x","nickname":""}'
```

返回 HTTP 400，`code=400`，`message` 中包含具体字段错误。

### 5.5 接口文档

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 自定义路径：修改 `application.yml` 中的 `springdoc.swagger-ui.path` / `springdoc.api-docs.path`。

## 6. 表结构管理与 Flyway

本模板提供两套初始化方式，**同一个数据库二选一**：

### 方式 A：MySQL 容器自动执行 init.sql（默认）

`docker-compose.yml` 把 `src/main/resources/db/init.sql` 挂载到
`/docker-entrypoint-initdb.d/01-init.sql`，MySQL 数据卷第一次初始化时自动执行。

注意：
- 只在数据卷为空时执行；已有数据卷不会重复执行；
- 需要重新初始化时执行 `docker compose down -v`；
- 不要用它和 Flyway 同时管理同一套表（避免“表已存在但没有迁移历史”的混乱）。

### 方式 B：Flyway 管理表结构（推荐生产使用）

pom 已包含：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
```

`application.yml` 默认：

```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:false}
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    clean-disabled: true
```

启用步骤：

1. 删除 docker-compose 中 `./src/main/resources/db/init.sql:/docker-entrypoint-initdb.d/01-init.sql:ro` 这一行；
2. 设置 `FLYWAY_ENABLED=true`；
3. 启动应用，Flyway 会执行 `src/main/resources/db/migration/V1__init_user.sql`；
4. 以后所有变更新增 `V2__xxx.sql`、`V3__xxx.sql`，**不要修改已经执行过的版本文件**；
5. 生产环境保持 `clean-disabled: true`，禁止 `flyway clean` 清库。

Flyway 的 `baseline-on-migrate: true` 用于接管已有库；`baseline-version: 0` 保证 `V1` 会执行。
如果是全新区，Flyway 直接按版本顺序执行即可。

## 7. 日志说明

`logback-spring.xml`（必须带 `-spring` 后缀）包含：

- 控制台输出；
- 文件输出：`RollingFileAppender` + `SizeAndTimeBasedRollingPolicy`；
- 按天 + 按大小滚动：`fileNamePattern: logs/app.%d{yyyy-MM-dd}.%i.log.gz`；
- `maxFileSize=100MB`、`maxHistory=30`、`totalSizeCap=3GB`；
- 格式包含：时间、级别、线程、类名、`traceId`（MDC）、消息；
- UTF-8 编码；
- 文件路径来自 `application.yml` 的 `logging.file.name`：
  ```yaml
  logging:
    file:
      name: ${LOG_DIR:logs}/app.log
    logback:
      rollingpolicy:
        file-name-pattern: ${LOG_DIR:logs}/app.%d{yyyy-MM-dd}.%i.log.gz
        max-file-size: 100MB
        max-history: 30
  ```

`TraceIdFilter` 会把请求头 `X-Request-Id` 写入 MDC；未传时自动生成 UUID，并通过响应头返回。日志示例：

```text
2026-11-05 13:52:30.799 [http-nio-8080-exec-1] WARN  c.e.t.common.exception.GlobalExceptionHandler [traceId=test-trace-001] - 参数校验失败: ...
```

`UserController` 的增删改查方法使用 Lombok `@Slf4j` 打印关键日志。

## 8. 常见坑

### 8.1 MyBatis-Plus 与 Spring Boot 4

1. **不要用 `mybatis-plus-spring-boot3-starter`**。Spring Boot 4 的自动配置注册方式有变化，应使用
   `mybatis-plus-spring-boot4-starter`。
2. Boot 4.0.x 对应 MP `3.5.17`；MP `3.5.18` 的 boot4 starter 基于 Boot 4.1.0。跨版本使用可能能跑，但不在同一条兼容线上。
3. MP 3.5.17 的包路径变化：
   - `IService`：`com.baomidou.mybatisplus.spring.service.IService`
   - `ServiceImpl`：`com.baomidou.mybatisplus.spring.service.impl.ServiceImpl`
   - `PaginationInnerInterceptor`：`com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor`，但类在 `mybatis-plus-jsqlparser` jar 中；
   - 必须显式引入 `mybatis-plus-extension` 和 `mybatis-plus-jsqlparser`，否则编译报“找不到符号”。
4. 逻辑删除要同时满足：
   - 实体字段加 `@TableLogic`；
   - `application.yml` 配置 `logic-delete-field/deleted/1/0`；
   - 数据库有 `deleted` 字段且默认 0。
5. 分页插件需要在 `MybatisPlusConfig` 中注册，`pageNum/pageSize` 才会真正生成 `LIMIT`。

### 8.2 springdoc v3 与 Boot 4

1. springdoc 3.x 才是 Boot 4 线路；不要用 springdoc 2.x。
2. springdoc 3.2.0 的构建基线是 Boot 4.1.2；本模板在 4.0.9 实测可用，但升级依赖时要回归 `/v3/api-docs` 和 Swagger UI。
3. 出现 `NoSuchMethodError` / `NoClassDefFoundError` 时，优先检查 springdoc 与 Boot 的版本线，而不是改代码。
4. Swagger UI 的访问入口是 `/swagger-ui.html`，它会 302 到 `/swagger-ui/index.html`；`curl` 要加 `-L` 才能看到最终页面。

### 8.3 Jackson 3（Boot 4）与 Redis

1. Boot 4 使用 Jackson 3（`tools.jackson`），因此 Redis 使用
   `GenericJacksonJsonRedisSerializer`，不要照抄 Boot 3 的 `GenericJackson2JsonRedisSerializer`。
2. **`spring.jackson.serialization.write-dates-as-timestamps` 在 Boot 4 会启动失败**：
   Jackson 3 的 `SerializationFeature` 已移除该枚举；`java.time` 默认按 ISO 输出，DTO 上用 `@JsonFormat` 控制格式。
   本模板已经移除了该配置，这是实测踩过的坑。
3. Redis JSON 反序列化开启了类型校验，只允许 `com.example.template.`、`java.util.`、`java.time.` 前缀，避免反序列化任意类。

### 8.4 MySQL 时区与字符集

1. JDBC URL 使用：
   ```text
   connectionTimeZone=Asia/Shanghai
   ```
   `serverTimezone` 是旧参数，Connector/J 9.x 下建议用新参数。
2. **不要随手加 `forceConnectionTimeZoneToSession=true`**：它会让驱动执行
   `SET time_zone='Asia/Shanghai'`，而 MySQL 官方镜像/精简安装通常没有导入时区表，
   会直接报 `Unknown or incorrect time zone: 'Asia/Shanghai'`，连接池初始化失败。
   本模板已去掉该参数；需要统一数据库会话时区时，用 MySQL 启动参数
   `--default-time-zone=+08:00`（本模板 compose 已配置），或改用 `+08:00` 这种偏移量。
3. MySQL 容器启动参数已设置 `--default-time-zone=+08:00`，并通过 `TZ=Asia/Shanghai` 统一容器时区。
4. 字符集统一 `utf8mb4`：
   - 服务端：`--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci`；
   - 表：`DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`；
   - JDBC：`useUnicode=true&characterEncoding=UTF-8`。
5. MySQL 8.4 默认认证插件是 `caching_sha2_password`，JDBC URL 中的 `allowPublicKeyRetrieval=true` 用于本地无 SSL 场景；生产建议启用 SSL 并去掉该参数。

### 8.5 Logback 中文/UTF-8

1. `logback-spring.xml` 的 encoder 已设置 `<charset>UTF-8</charset>`；
2. Windows 控制台如果仍乱码，执行 `chcp 65001` 或把终端切到 UTF-8；
3. Docker 中设置 `LANG=C.UTF-8`、`-Dfile.encoding=UTF-8`；
4. 日志文件路径由 `LOG_DIR` 控制，Docker 中挂载 `/app/logs`；非 root 用户需要对目录有写权限，镜像已 `chown`，若改成 bind mount 需要手动授权。

### 8.6 init.sql、数据卷与逻辑删除

1. `init.sql` 只在 MySQL 数据卷第一次初始化时执行；修改 SQL 后必须 `docker compose down -v` 才会重新执行。
2. `sys_user.username` 有唯一索引，而删除是逻辑删除，所以**已删除用户的用户名仍被占用**。如需允许复用用户名，可改成“删除时同时改写 username”或使用包含删除标记的复合唯一索引，并在业务层处理重复删除。
3. 示例数据使用 `INSERT IGNORE`，脚本可重复执行不报唯一键错误。

### 8.7 Docker 构建

1. 多阶段构建第一阶段需要联网下载 Maven 依赖；国内网络慢时可在 `pom.xml` 或 Maven `settings.xml` 配置镜像。
2. 运行阶段使用 `eclipse-temurin:21-jre`，非 root 用户 `spring`，工作目录 `/app`。
3. `JAVA_OPTS` 默认 `-XX:MaxRAMPercentage=75.0`，容器内存限制较小时不要给 JVM 固定 `-Xmx`。
4. app 使用 `depends_on` 的健康检查只保证启动顺序，不保证 MySQL 内部初始化脚本已全部完成；本模板的 init.sql 很短，若数据量大可在 app 侧加重试或再加一层就绪探针。

## 9. 已验证项

在 JDK 21 + Maven 3.9.6 下实际执行过：

- `mvn -DskipTests package`：BUILD SUCCESS；
- 应用启动：Spring Boot 4.0.9 正常启动，Tomcat 8080；
- `/v3/api-docs`：HTTP 200，包含用户与健康检查接口；
- `/swagger-ui.html`：302 → `/swagger-ui/index.html`，HTTP 200；
- `/api/health`、`/api/health/mysql`、`/api/health/redis`：MySQL/Redis 可用时均返回 HTTP 200；
- 用户 CRUD：新增、详情、更新、逻辑删除全部通过；分页查询返回 `total/pageNum/pageSize/pages` 正确；
- 唯一用户名重复新增：HTTP 409 + `code=40901`；
- 删除后查询详情：HTTP 404 + `code=40401`；
- 乐观锁：更新后数据库 `version` 从 1 自增到 2；
- 参数校验：HTTP 400，统一返回体；
- traceId：请求头 `X-Request-Id` 能出现在日志中；
- 日志文件：`logs/app.log` 生成，UTF-8 中文正常。

> 本机没有安装 Docker，`docker compose up` 未实际执行；Dockerfile 与 compose 按标准语法编写，建议首次部署时先执行 `docker compose config` 校验，并查看 `docker compose logs -f app`。
>
> 本机运行时联调使用的是临时 MySQL 8.0.45 / Redis 5.0（协议兼容），用于验证 JDBC、MyBatis-Plus、Redis 序列化与接口逻辑；模板目标运行版本仍为 MySQL 8.4 / Redis 7.4（compose 镜像）。MySQL 8.4 的差异主要在认证/默认参数，模板已按 8.4 的 `caching_sha2_password` 与 `utf8mb4` 配置。

