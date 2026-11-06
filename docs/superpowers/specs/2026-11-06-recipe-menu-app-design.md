# Menu · 灶台手账 — 设计规格

- 日期：2026-11-06
- 状态：已确认（含移动端适配补充）
- 代码位置：`Menu/backend`、`Menu/frontend`

## 1. 目标

做一个个人/家庭自用的菜谱网站：从 B 站、抖音的分享链接**快速录入菜谱**，支持登录、分类、
用餐偏好配置、可配置多家 AI 大模型、AI 生成配图与菜谱摘要，以及**按偏好随机今日菜品**。
前端必须同时适配桌面与手机（移动端为一等需求）。大模型 API Key 只能保存在本地，不能泄露。

## 2. 技术基座

| 层 | 选型 | 来源 |
| --- | --- | --- |
| 前端 | Vue 3.5 + TypeScript + Vite 8 + Vue Router 5 + Pinia + Tailwind CSS v4 + Vitest | `FengXueCode/Vue3NewTemplate` / 本地 `template/Vue模板` |
| 后端 | Java 21 + Spring Boot 4.0.9 + MyBatis-Plus 3.5.17 + MySQL 8 + Redis + Flyway + springdoc | `FengXueCode/SpringBootTemplate` / 本地 `template/Java后端模板/SpringBoot模板` |
| 数据库 | MySQL 8.0（本机 3306，库名 `recipe_menu`） | 本机已运行 |
| 缓存/限流 | Redis 7（本机 6379，不可用时降级） | 本机 Redis 服务 |
| 构建 | Maven 3.9 + npm（Node 22） | 本机 |

前端开发时通过 Vite 代理 `/api` → `http://localhost:8080`，生产可打包进后端静态目录或独立部署。

## 3. 关键决策

### 3.1 视频导入（稳妥版）

```
分享链接/整段分享文案
  → 识别平台（bilibili / douyin）
  → B站：b23.tv 短链跟随跳转 → BV 号 → 公开 web API 取标题/简介/封面/时长
         → 尝试 player/v2 字幕（失败静默）
  → 抖音：短链跟随跳转 → 分享页 meta / _ROUTER_DATA best-effort
         → 失败则从分享文案解析标题、作者
  → 用户可补充“视频文案/字幕”文本
  → AI 结构化抽取（JSON）
  → 预览页可编辑（标题/分类/份量/荤素/配料/步骤）
  → 保存
```

不下载视频、不做语音转写。抖音抓取失败属于正常分支，前端必须给出清晰提示，而不是报错卡死。

### 3.2 AI Key 存储与调用（安全）

- 每用户可配置**多套** AI 服务商配置（`ai_provider`），前端可新增/编辑/删除/设为默认/测试连接。
- Key 使用 **AES-256-GCM** 加密后入库；主密钥在 `backend/data/secret.key` 首次启动生成（32 字节随机，
  文件权限尽量收紧），已被 `.gitignore` 忽略；可用环境变量 `AI_KEY_MASTER` 覆盖（Base64 32 字节）。
- 所有 AI 请求由后端代理，Key 不出后端；任何接口响应只返回 `hasKey` + 掩码 `sk-****abcd`。
- 日志只记录 provider/model，不记录 Key、请求头、完整请求体。
- 支持 OpenAI 兼容协议：`POST {baseUrl}/chat/completions`、`POST {baseUrl}/images/generations`。
  内置预设：OpenAI、DeepSeek、通义千问（DashScope 兼容模式）、Moonshot Kimi、硅基流动、Ollama、自定义。

### 3.3 随机今日菜单

- 以 `userId + 日期 + 重生成序号` 作为随机种子，**同一天默认结果稳定**；点“重新生成”序号 +1。
- 按偏好：用餐人数、每日餐数（1–3）、每餐菜品数、荤素配比、忌口食材、辣度、最长烹饪时间、菜系偏好。
- 菜品配额：先按荤素比分配荤/素数量，再补汤/主食（餐次允许时），最后按权重抽取。
- 权重：偏好菜系加权、近 7 天未做过的加权、忌口/超时过滤；无足够候选时放宽限制并提示。
- 配料用量按“目标人数 / 菜谱基准份量”等比缩放，四舍五入到易读值。
- 单道“换一道”保留其他菜；菜单可保存到 `daily_menu`，用于历史去重。

## 4. 数据模型（MySQL，utf8mb4）

```sql
sys_user(id, username UNIQUE, password_hash, nickname, avatar, status, created_at, updated_at)
category(id, user_id, name, icon, color, sort, is_system, created_at)
recipe(id, user_id, category_id, title, description, cover_url, source_platform,
       source_url, source_author, duration_min, difficulty, servings, meat_type,
       spice_level, tags_json, ai_summary, summary_generated_at, deleted, created_at, updated_at)
recipe_ingredient(id, recipe_id, name, amount, unit, category, sort)
recipe_step(id, recipe_id, step_no, content, duration_sec)
user_preference(user_id UNIQUE, people_count, meals_per_day, dishes_per_meal,
                meat_ratio, veg_ratio, max_cook_minutes, spice_level,
                avoid_ingredients_json, preferred_categories_json, updated_at)
ai_provider(id, user_id, name, provider_type, base_url, chat_model, image_model,
            api_key_cipher, api_key_hint, enabled, is_default, last_test_ok, created_at, updated_at)
daily_menu(id, user_id, menu_date, regenerate_seq, payload_json, created_at)
```

`meat_type` 枚举：`MEAT` / `VEG` / `SOUP` / `STAPLE` / `DESSERT` / `OTHER`。
`difficulty`：1 简单 / 2 中等 / 3 进阶。`spice_level`：0–3。

## 5. 后端 API 契约

统一返回：`{ code, message, data, timestamp }`，成功 `code=200`；HTTP 语义对齐 400/401/403/404/409/500。
鉴权：`Authorization: Bearer <jwt>`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册（username/password/nickname） |
| POST | `/api/auth/login` | 登录，返回 token + user |
| GET | `/api/auth/me` | 当前用户 |
| GET | `/api/categories` | 分类列表（系统 + 我的） |
| POST | `/api/categories` | 新建自定义分类 |
| PUT/DELETE | `/api/categories/{id}` | 改名/删除（系统分类不可改） |
| GET | `/api/recipes` | 分页列表：keyword/categoryId/meatType/mine/pageNum/pageSize |
| GET | `/api/recipes/{id}` | 详情（配料 + 步骤） |
| POST | `/api/recipes` | 创建（完整结构） |
| PUT/DELETE | `/api/recipes/{id}` | 更新/删除（逻辑删除） |
| POST | `/api/import/resolve` | `{url, shareText?, extraText?}` 解析视频元数据 |
| POST | `/api/ai/extract` | `{text, source?}` → 结构化菜谱草稿 |
| GET | `/api/ai/providers` | 服务商配置列表（Key 掩码） |
| POST | `/api/ai/providers` | 新增配置（含 key） |
| PUT | `/api/ai/providers/{id}` | 更新（key 为空表示不修改） |
| DELETE | `/api/ai/providers/{id}` | 删除 |
| POST | `/api/ai/providers/{id}/test` | 测试连接 |
| POST | `/api/ai/recipes/{id}/summary` | 生成/刷新 AI 摘要 |
| POST | `/api/ai/recipes/{id}/image` | 生成配图（存本地并回填 cover） |
| GET | `/api/preferences` | 用餐偏好 |
| PUT | `/api/preferences` | 保存偏好 |
| GET | `/api/menu/today?date=&regenerate=` | 生成/读取今日菜单 |
| POST | `/api/menu/today/reroll` | 单道换菜 `{date, mealKey, index, seq}` |
| POST | `/api/menu/today/save` | 保存菜单 |
| GET | `/api/files/{name}` | 本地图片访问 |

安全附加：导入地址做协议/域名白名单 + 内网 IP 拦截 + 超时/体积限制；AI 与导入接口限流（Redis，失败降级内存）。

## 6. 前端信息架构与移动端

页面：

1. `/login` 登录/注册（演示账号 `demo / 123456` 一键填充）
2. `/` 首页：今日菜单预览 + 快捷入口 + 最近菜谱 + 分类胶囊
3. `/recipes` 菜谱库：搜索、分类/荤素筛选、无限/分页加载、卡片
4. `/recipes/:id` 详情：封面、AI 摘要、配料（按人数缩放）、步骤、原视频嵌入
5. `/import` 导入：三步（链接/文案 → 解析预览 → 编辑保存），移动端为纵向步骤条
6. `/today` 今日菜单：餐次分组、菜品卡、换一道、重新生成、保存
7. `/me` 我的：我的菜谱、偏好设置入口、AI 配置入口、退出
8. `/settings/preferences` 偏好设置
9. `/settings/ai` AI 配置（多套、默认、测试、掩码显示）

移动端要求（验收硬指标）：

- 375×812、390×844 下无横向滚动，文字不截断错位
- 底部固定 Tab（首页/菜谱/今日/我的），含 `env(safe-area-inset-bottom)`；桌面端顶部导航
- 卡片单列/双列自适应；筛选在移动端用底部弹层（bottom sheet），桌面端用侧栏/胶囊
- 可点击元素 ≥ 44×44px；输入框 `font-size ≥ 16px` 防 iOS 缩放；表单全宽
- 导入/生成等长流程有明确 loading、骨架屏、错误重试
- 详情页操作按钮移动端吸底
- 图片懒加载、`aspect-ratio` 占位避免抖动

视觉方向“灶台手账”：奶油纸底 `#FAF6EE`、墨色 `#2A211C`、番茄红 `#D8503C`、香草绿 `#4C7A4F`、
姜黄 `#E7B24A`；标题用衬线中文字体栈，正文无衬线；纸张纹理 + 圆角卡片 + 细描边；避免后台模板感。
Tailwind v4 `@theme` 定义颜色/字体/圆角；仅用工具类，必要时少量 `@layer` 基础样式。

## 7. 安全清单

- BCrypt 密码；JWT HS256，secret 本地文件/环境变量生成，默认 7 天过期
- 鉴权拦截器统一 401；资源归属校验（只能改自己的菜谱/配置）
- AI Key：AES-GCM（随机 IV）、密文入库、响应脱敏、日志脱敏、`GET` 不回传 Key
- SSRF：仅允许 `bilibili.com`、`b23.tv`、`douyin.com`、`v.douyin.com`、`iesdouyin.com` 及其重定向结果，
  禁止 `file:`、内网/回环/链路本地地址；限制 3 次重定向、5s 超时、2MB 响应
- 上传：图片类型/大小校验，文件名 UUID，禁止路径穿越
- 限流：登录 10 次/分钟/IP，AI 20 次/分钟/用户，导入 10 次/分钟/用户
- 不在前端持久化任何 Key；AI 配置页只显示掩码

## 8. 验收标准

1. 新库启动自动建表 + 内置分类/示例菜谱/演示账号。
2. 注册、登录、鉴权、退出全链路可用。
3. 粘贴 B 站链接可解析出标题/封面并 AI 提取（或手动补文案后提取）；抖音失败有可操作提示。
4. AI 配置可新增多家厂商、Key 只写不读、接口/页面均只见掩码；重启后配置仍可用。
5. AI 摘要与配图可用；未配置图像模型时本地兜底封面可用。
6. 今日菜单按偏好生成，同日稳定、可重新生成、可单道换菜、可保存；人数变化配料等比缩放。
7. 前端 `npm run build`、后端 `mvn package` 通过；桌面与 375px 移动端截图验收通过。
8. README 给出完整启动步骤与安全说明。

## 9. 非目标

- 不做视频下载、语音转写、评论区抓取
- 不做公网多租户/SaaS、支付、社交
- 不承诺抖音解析 100% 成功（平台反爬），以分享文案 + 手动补充兜底
