# Menu · 灶台手账 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 交付一个可本地运行的全栈菜谱网站：视频链接导入、登录/分类、偏好配置、多厂商 AI 配置（Key 本地加密）、AI 摘要/配图、按偏好随机今日菜单，并同时适配桌面与手机。

**Architecture:** Vue 3 SPA（`frontend/`）通过 Vite 代理访问 Spring Boot 4 REST API（`backend/`）；MySQL 持久化，Redis 做缓存/限流（可降级）；AI 调用全部经后端代理，Key AES-GCM 加密存库；本地文件系统保存 AI 生成图片与主密钥。

**Tech Stack:** Vue 3.5 / TS / Vite 8 / Pinia / Tailwind v4；Java 21 / Spring Boot 4.0.9 / MyBatis-Plus 3.5.17 / Flyway / MySQL 8 / Redis；JUnit 5 / Vitest。

## Global Constraints

- JDK 21（`JAVA_HOME=D:\jdk-21.0.12.1`），Node `^22.18.0`，Maven 3.9+。
- 前端只使用 `<script setup>` + Composition API + Tailwind 工具类；不引 Sass/Less/Vuex。
- 后端统一返回 `Result{code,message,data,timestamp}`；异常统一 `GlobalExceptionHandler`。
- 所有价格/Key/密码等敏感值禁止出现在日志与响应中。
- 移动端 375px 起可用，无横向滚动；触控目标 ≥44px。
- 不新增与需求无关的依赖；AI 采用 OpenAI 兼容协议。

---

## 文件结构

```
Menu/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/java/com/fengxue/menu/
│     ├─ MenuApplication.java
│     ├─ common/{Result,ResultCode,PageResult,exception/*,util/*}
│     ├─ config/{MybatisPlusConfig,WebConfig,JacksonConfig,TraceIdFilter,AuthInterceptor,RateLimitFilter}
│     ├─ auth/{AuthController,AuthService,JwtUtil,CurrentUser}
│     ├─ user/{User,UserMapper}
│     ├─ category/{Category,CategoryMapper,CategoryController,CategoryService}
│     ├─ recipe/{Recipe,RecipeIngredient,RecipeStep,*Mapper,RecipeController,RecipeService,dto/*}
│     ├─ importer/{ImportController,VideoImportService,BilibiliClient,DouyinClient,UrlGuard,VideoMeta}
│     ├─ ai/{AiProvider,AiProviderMapper,AiController,AiProviderService,AiClient,PromptTemplates,AiCipher,ImageService,dto/*}
│     ├─ preference/{UserPreference,Mapper,PreferenceController}
│     ├─ menu/{DailyMenu,Mapper,MenuController,MenuService,MenuGenerator,MenuPlan/*}
│     └─ seed/{DataSeeder,RecipeSeedLoader}
│  └─ src/main/resources/{application.yml,application-dev.yml,db/migration/V1__init.sql,seed/recipes.json,logback-spring.xml}
├─ frontend/
│  ├─ package.json, vite.config.ts, index.html
│  └─ src/
│     ├─ main.ts, App.vue, assets/main.css
│     ├─ api/{http.ts,auth.ts,recipes.ts,ai.ts,menu.ts,preferences.ts,import.ts,categories.ts}
│     ├─ stores/{auth.ts,toast.ts,preferences.ts}
│     ├─ router/index.ts
│     ├─ components/{AppShell,TopNav,BottomTabs,RecipeCard,CategoryChips,MealCard,ModalSheet,ToastHost,EmptyState,SkeletonCard,ConfirmDialog,VideoSourceCard}
│     ├─ views/{LoginView,HomeView,RecipesView,RecipeDetailView,ImportView,TodayView,MeView,PreferencesView,AiSettingsView}
│     ├─ composables/{useMediaQuery,useAsync}
│     ├─ utils/{format.ts,scale.ts,platform.ts}
│     └─ types/api.ts
├─ docs/superpowers/{specs,plans}/
└─ README.md
```

---

### Task 1: 工程骨架、数据库与基础设施

**Files:**
- Create: `backend/pom.xml`, `backend/src/main/resources/application{,-dev}.yml`, `backend/src/main/java/com/fengxue/menu/MenuApplication.java`, `backend/src/main/java/com/fengxue/menu/common/**`, `backend/src/main/resources/db/migration/V1__init.sql`, `backend/.gitignore`
- Create: `frontend/`（复制 Vue 模板，去掉 node_modules/.git）
- Test: `backend/src/test/java/com/fengxue/menu/SmokeTest.java`（上下文加载）

**Interfaces:**
- Produces: `Result.ok(T)` / `Result.fail(ResultCode, String)`；MyBatis-Plus 分页插件；`JAVA_HOME` 使用 JDK21 的 Maven 命令。

- [x] 复制后端模板工程（排除 `.idea`），改 `groupId=com.fengxue`、`artifactId=menu-backend`、包名 `com.fengxue.menu`，保留 Spring Boot 4.0.9 / MP 3.5.17 / springdoc 3.2.0。
- [x] 复制前端模板（排除 `node_modules`、`.git`），`npm install`。
- [x] 编写 `V1__init.sql`：全部表 + 唯一索引 + 外键索引（见规格第 4 节）；`spring.flyway.enabled=true`。
- [x] 配置 datasource：`jdbc:mysql://localhost:3306/recipe_menu?...`，用户 `root`，密码 `${DB_PASSWORD:FengXue@}`。
- [x] 创建数据库：`mysql -uroot -pFengXue@ -e "CREATE DATABASE IF NOT EXISTS recipe_menu ..."`。
- [x] 启动 Redis：`Start-Process D:\envirment\redis\redis-server.exe`，`redis-cli ping` 返回 PONG；后端 Redis 连接失败不影响启动。
- [x] 验收：`mvn -q -DskipTests package` 成功；`java -jar target/menu-backend.jar` 启动并请求 `/api/health` 返回 200。

### Task 2: 认证（注册/登录/JWT/鉴权）

**Files:**
- Create: `auth/AuthController.java`, `auth/AuthService.java`, `auth/JwtUtil.java`, `common/util/PasswordUtil.java`, `config/AuthInterceptor.java`, `user/User.java`, `user/UserMapper.java`, `auth/dto/*`
- Test: `AuthServiceTest`（注册重复用户名冲突、登录密码错误、JWT 解析）

**Interfaces:**
- `POST /api/auth/register|login` → `{token, user:{id,username,nickname}}`
- `JwtUtil.createToken(Long userId)` / `parseUserId(String token)`
- `AuthInterceptor` 放行 `/api/auth/**`、`/api/health/**`、`/api/files/**`；其他 `/api/**` 校验 Bearer。

- [x] 用户表字段 `password_hash` 用 BCrypt；注册默认播种偏好与系统分类可见。
- [x] JWT HS256，secret 从 `data/jwt.secret` 读取/生成，7 天过期；密码不得写入日志。
- [x] 鉴权失败返回 401 统一结构；`/api/auth/me` 返回当前用户。
- [x] 单测覆盖上述三分支；`mvn test` 通过。

### Task 3: 分类与菜谱 CRUD

**Files:**
- Create: `category/*`, `recipe/{Recipe,RecipeIngredient,RecipeStep,Mapper,Controller,Service,dto/*}`, `common/PageResult.java`
- Test: `RecipeServiceTest`（创建带配料步骤、只允许改自己的、逻辑删除、分页过滤）

**Interfaces:**
- `GET /api/recipes?pageNum&pageSize&keyword&categoryId&meatType&mine` → `PageResult<RecipeCard>`
- `GET /api/recipes/{id}` → `RecipeDetail{..., ingredients:[{name,amount,unit}], steps:[{stepNo,content}]}`
- `POST/PUT /api/recipes` 使用同一 `RecipeUpsertDTO`；创建时 `userId` 取当前登录用户。

- [x] 分类：系统分类 `user_id=0` 所有人可见；自定义分类归属校验。
- [x] 列表只返回 `deleted=0`；关键词搜标题/描述；返回分页 `total/pageNum/pageSize/pages`。
- [x] 详情聚合配料/步骤，按 `sort/stepNo` 排序；`coverUrl` 支持本地 `/api/files/x.png` 与远程 URL。
- [x] 归属校验失败 403；不存在 404。单测通过。

### Task 4: 视频导入解析

**Files:**
- Create: `importer/{VideoImportService,BilibiliClient,DouyinClient,UrlGuard,VideoMeta,ImportController,ImportResolveDTO}`
- Test: `UrlGuardTest`（合法域名、短链、内网 IP、非法协议）、`ShareTextParserTest`（从分享文案抽标题/链接）

**Interfaces:**
- `POST /api/import/resolve {url?, shareText?, extraText?}` → `{platform, videoId, title, description, coverUrl, author, durationSec, bvid?, shareTextTitle, canEmbed, warnings[]}`
- `UrlGuard.assertSafe(String url)`：仅 http/https，域名后缀白名单，解析后 IP 非内网，最多 3 次重定向。
- B 站公开 API：`https://api.bilibili.com/x/web-interface/view?bvid=`（带 UA/Referer）；字幕失败静默。
- 抖音：`https://www.iesdouyin.com/share/video/{id}` 解析 `_ROUTER_DATA`/meta；失败返回 `warnings=["抖音页面需要登录，已提取分享文案标题，可粘贴视频文案提升识别质量"]`。

- [x] 从整段分享文案中提取第一个 URL；识别 `b23.tv` 并跟随跳转拿 BV 号。
- [x] B 站调用超时 5s、响应 ≤2MB；异常转业务提示而非 500。
- [x] 抖音失败必须返回 200 + 部分字段 + warnings，前端可继续手动补充。
- [x] 单测覆盖 SSRF 与分享文案解析。

### Task 5: AI 配置、加密与多厂商管理

**Files:**
- Create: `ai/{AiProvider,AiProviderMapper,AiCipher,AiProviderService,AiController,dto/AiProviderDTO,ProviderPreset}`, `ai/AiCipherTest`
- Modify: `application.yml`（`app.ai.master-key-file: data/secret.key`）
- Test: `AiCipherTest`（加解密、随机 IV、篡改失败、掩码）

**Interfaces:**
- `AiCipher.encrypt(String plain)` → Base64(iv+ciphertext)；`decrypt(String cipher)`
- `GET /api/ai/providers` → `[{id,name,providerType,baseUrl,chatModel,imageModel,hasKey,keyMasked,enabled,isDefault,lastTestOk}]`（绝不返回明文）
- `POST/PUT /api/ai/providers`：`apiKey` 为空表示保留旧 Key；`isDefault=true` 时取消同用户其他默认。
- `POST /api/ai/providers/{id}/test` → 调用 `/chat/completions` 最小请求，返回 `{ok,message,latencyMs}`。

- [x] 主密钥文件不存在时自动生成 32 随机字节；目录已在 `.gitignore`。
- [x] Key 长度、BaseURL 协议校验；不同用户配置隔离。
- [x] 掩码规则：长度 ≤8 显示 `****`，否则前 3 + `****` + 后 4。
- [x] 单测覆盖加解密/掩码/默认切换；接口响应断言不含 `apiKey` 字段。

### Task 6: AI 能力（提取/摘要/配图）

**Files:**
- Create: `ai/{AiClient,PromptTemplates,AiExtractService,AiSummaryService,AiImageService}`, `ai/dto/*`, `file/FileStorageService`, `config/WebConfig`（`/api/files/**` 静态映射）
- Test: `AiJsonParserTest`（去 ```json 围栏、前后废话、缺字段）、`ImageFallbackTest`（生成 SVG 兜底封面）

**Interfaces:**
- `POST /api/ai/extract {text, source?}` → `RecipeDraft`（title/description/categoryName/meatType/difficulty/servings/cookMinutes/spiceLevel/tags/ingredients/steps/tips）
- `POST /api/ai/recipes/{id}/summary` → `{aiSummary, ingredientsBrief[]}`，写回 `recipe.ai_summary`
- `POST /api/ai/recipes/{id}/image` → `{coverUrl}`，优先 `images/generations`（b64_json 或 url 下载），失败/未配置图像模型时用本地 SVG 兜底并返回警告
- `AiClient.chatJson(provider, system, user)` 使用 JDK `HttpClient` 或 `RestClient`，超时 60s，解析容错。

- [x] 提示词要求中文、严格 JSON、配料含 amount/unit、步骤 4–10 步；服务端校验字段并 clamp。
- [x] 无默认 provider 时返回 400 `请先在设置中配置 AI 模型`。
- [x] 生成图片存 `data/uploads/{uuid}.png|svg`，通过 `/api/files/{name}` 访问；路径穿越校验。
- [x] 单测覆盖 JSON 容错与兜底图。

### Task 7: 用餐偏好与随机今日菜单

**Files:**
- Create: `preference/*`, `menu/{DailyMenu,Mapper,MenuController,MenuService,MenuGenerator,MenuPlan/*}`, `menu/MenuGeneratorTest`, `menu/PortionScalerTest`
- Test: 固定种子结果稳定、配比数量正确、忌口过滤、人数缩放、近 7 天去重、候选不足降级

**Interfaces:**
- `GET/PUT /api/preferences`：`{peopleCount,mealsPerDay,dishesPerMeal,meatRatio,vegRatio,maxCookMinutes,spiceLevel,avoidIngredients[],preferredCategories[]}`
- `GET /api/menu/today?date=yyyy-MM-dd&regenerate=false` → `{date,seq,summary,meals:[{key,label,dishes:[{recipeId,title,coverUrl,meatType,cookMinutes,servings,ingredients:[{name,amount,unit}]}]}],warnings[]}`
- `POST /api/menu/today/reroll {date,seq,mealKey,index}` → 替换单道，返回整份菜单
- `POST /api/menu/today/save {date,seq,plan}` → 落库（同用户同日覆盖）
- `MenuGenerator.generate(userId, date, seq, prefs, candidates)` 纯函数，便于单测。

- [x] 荤素配额：`meatCount = round(dishes * meatRatio/(meatRatio+vegRatio))`，其余素；餐次≥2 时补汤/主食。
- [x] 权重：偏好菜系 +2、7 天未出现 +1、从未做过 +0.5；同餐不重复。
- [x] 分量：`scale = peopleCount / recipe.servings`，单位 g/ml 取 5 的倍数，个/只向上取整。
- [x] 同日同 seq 结果稳定；regenerate 后 seq+1 且组合变化。
- [x] 单测 6 个场景通过。

### Task 8: 前端基座与设计系统

**Files:**
- Create/Modify: `frontend/vite.config.ts`（proxy `/api`）、`src/assets/main.css`（`@theme` 颜色/字体/阴影）、`src/router/index.ts`（含守卫）、`src/api/http.ts`、`src/stores/{auth,toast}.ts`、`src/App.vue`、`src/components/{AppShell,TopNav,BottomTabs,ToastHost}.vue`、`src/types/api.ts`

**Interfaces:**
- `http<T>(path, {method, body, query})` 自动带 Bearer、401 跳登录、错误转 Toast。
- `useAuthStore`: `token/user`、`login/register/logout/fetchMe`，token 存 localStorage。
- 设计 token：`--color-paper #FAF6EE`、`--color-ink #2A211C`、`--color-tomato #D8503C`、`--color-herb #4C7A4F`、`--color-ginger #E7B24A`；字体 `font-display` 衬线。
- 移动端 `<BottomTabs>` 固定底部，`pb-[env(safe-area-inset-bottom)]`；桌面隐藏，顶部 `TopNav`。

- [x] 路由守卫：未登录访问业务页 → `/login?redirect=`；已登录访问 `/login` → `/`。
- [x] Toast 队列、确认弹窗、骨架屏组件。
- [x] 375px 下 AppShell 无横向滚动（`overflow-x-hidden` + 最大宽度）。

### Task 9: 前端页面（桌面 + 移动）

**Files:**
- Create: `views/{LoginView,HomeView,RecipesView,RecipeDetailView,ImportView,TodayView,MeView,PreferencesView,AiSettingsView}.vue`
- Create: `components/{RecipeCard,CategoryChips,MealCard,ModalSheet,EmptyState,SkeletonCard,VideoSourceCard}.vue`
- Create: `api/{recipes,ai,menu,preferences,import,categories}.ts`、`utils/{format,scale,platform}.ts`
- Test: `RecipeCard.spec.ts`、`scale.spec.ts`、`platform.spec.ts`

**Interfaces:** 全部消费 Task 2–7 的契约，字段名严格对齐 `types/api.ts`。

- [x] 登录页：登录/注册切换，demo 账号填充，错误提示。
- [x] 首页：问候 + 今日菜单预览（调 `/menu/today`）+ 快捷入口 + 最近菜谱。
- [x] 菜谱库：搜索防抖、分类胶囊、荤素筛选、分页；移动端筛选 bottom sheet。
- [x] 详情：封面、AI 摘要（生成按钮）、配料人数缩放、步骤、B站 iframe（`canEmbed`）、来源链接；移动端吸底操作。
- [x] 导入：URL/分享文案 → `/import/resolve` → 可补充文案 → `/ai/extract` → 可编辑表单（配料/步骤增删）→ 保存；抖音 warning 显示为可操作提示。
- [x] 今日菜单：按餐次分组、菜品卡、换一道、重新生成、保存；空状态引导配置偏好。
- [x] 我的：用户信息、我的菜谱、入口；偏好设置：人数/餐数/比例/忌口等；AI 设置：多套配置列表、新增/编辑、Key 输入（只写）、掩码、测试、设默认。
- [x] Vitest 组件/工具测试通过；`npm run build` 通过。

### Task 10: 移动端适配打磨

**Files:** Modify: `App.vue`、`components/*`、`views/*`、`assets/main.css`（仅必要的 safe-area/滚动条规则）

- [x] 375×812 截图逐页检查：首页/菜谱/详情/导入/今日/设置，无横向滚动。
- [x] Tab 栏、底部吸底按钮、bottom sheet 不遮挡内容（滚动 padding）。
- [x] 触控目标 ≥44px；输入字号 ≥16px；图片 `aspect-ratio` + `loading=lazy`。
- [x] iPhone 安全区：`env(safe-area-inset-bottom)`；`viewport-fit=cover`。
- [x] 桌面 1280px 布局不空旷：内容最大宽 1180px，卡片网格。

### Task 11: 集成验证、种子数据与文档

**Files:** Create: `backend/src/main/resources/seed/recipes.json`（18–24 道，8 分类，含配料步骤）、`seed/DataSeeder.java`；Create: `README.md`、`backend/.env.example`、`scripts/dev.ps1`

- [x] `DataSeeder` 幂等：系统分类 + 示例菜谱 + demo 账号（`demo/123456`，偏好默认）。
- [x] 冒烟脚本：注册/登录 → 列表 → 详情 → 创建 → 偏好 → 生成菜单 → 换菜 → 保存 → AI 配置掩码校验。
- [x] 启动 MySQL/Redis/后端/前端，浏览器实测并截图（桌面 + 375px）。
- [x] README：架构、启动、演示账号、AI 配置、安全说明、常见问题（抖音反爬、Redis 可选）。
- [x] 最终检查：`mvn test`、`npm run test`、`npm run build` 全绿。

---

## 风险与应对

| 风险 | 应对 |
| --- | --- |
| 抖音页面反爬导致解析失败 | 分享文案解析 + 手动补充文案 + 明确 warnings，不阻断流程 |
| 用户没有带图像能力的模型 | 本地 SVG 风格封面兜底，页面标注“未配置图像模型” |
| MySQL 8.0.12 与 Flyway 11 兼容性 | 先手动 `init.sql` 兜底；Flyway 失败则 `FLYWAY_ENABLED=false` + 启动执行 SQL |
| Spring Boot 4 依赖兼容 | 严格沿用模板版本（MP 3.5.17 / springdoc 3.2.0），JDK 21 |
| 移动端长页面/弹层遮挡 | 统一 AppShell padding + safe-area + 底部 sheet 滚动锁定 |
