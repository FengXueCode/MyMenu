# Menu API 契约（前后端共同遵守）

Base URL：开发环境由 Vite 代理 `/api` → `http://localhost:8080`。
统一包裹：`{ "code": 200, "message": "操作成功", "data": <T>, "timestamp": 1762310400000 }`。
成功判断 `code === 200`。HTTP 状态码语义对齐；`data` 可为 `null`。
鉴权：除 `/api/auth/register|login`、`/api/health/**`、`/api/files/**` 外，均需 `Authorization: Bearer <token>`。
未登录/过期统一 `code=401`，前端跳转 `/login?redirect=`。

## 枚举

```ts
type MeatType = 'MEAT' | 'VEG' | 'SOUP' | 'STAPLE' | 'DESSERT' | 'OTHER'
// 荤 / 素 / 汤羹 / 主食 / 甜点 / 其他
type Difficulty = 1 | 2 | 3
```

## 认证

`POST /api/auth/register` `{username, password, nickname?}` → `{ token, user }`
`POST /api/auth/login` `{username, password}` → `{ token, user }`
`GET /api/auth/me` → `user`
`User = { id, username, nickname, avatar? }`

校验：username 3–20 位字母数字下划线；password 6–32 位。用户名重复 → `code=409`。

## 分类

`GET /api/categories` → `Category[]`（系统分类 + 当前用户自定义，按 sort 升序）
`POST /api/categories` `{name, icon?, color?}` → `Category`
`PUT /api/categories/{id}` `{name?, icon?, color?}` → `Category`（系统分类 403）
`DELETE /api/categories/{id}` → `null`（系统分类 403；有菜谱引用 → 409）
`Category = { id, name, icon, color, sort, isSystem }`
内置：家常菜🍳 / 川菜🌶️ / 粤菜🥢 / 快手菜⚡ / 汤羹🍲 / 主食🍚 / 早餐🥟 / 甜点🍰

## 菜谱

`GET /api/recipes?pageNum=1&pageSize=12&keyword=&categoryId=&meatType=&mine=false`
→ `PageResult<RecipeCard>`
`PageResult<T> = { total, pageNum, pageSize, pages, records: T[] }`
`RecipeCard = { id, title, description, coverUrl, categoryId, categoryName, meatType, difficulty, servings, durationMin, spiceLevel, tags: string[], aiSummary, sourcePlatform, sourceUrl, sourceAuthor, owner: 'system'|'mine', createdAt }`

`GET /api/recipes/{id}` → `RecipeDetail = RecipeCard & { ingredients: Ingredient[], steps: Step[] }`
`Ingredient = { id?, name, amount, unit, category?, sort? }`
`Step = { id?, stepNo, content, durationSec? }`

`POST /api/recipes` → `RecipeDetail`，请求体：
```json
{ "title":"", "description":"", "categoryId":1, "coverUrl":null,
  "sourcePlatform":"bilibili", "sourceUrl":"", "sourceAuthor":"",
  "durationMin":30, "difficulty":1, "servings":2, "meatType":"MEAT", "spiceLevel":1,
  "tags":["下饭"], "ingredients":[{"name":"五花肉","amount":"300","unit":"g"}],
  "steps":[{"content":"切块","durationSec":120}] }
```
`PUT /api/recipes/{id}` 同上；`DELETE /api/recipes/{id}` → `null`（仅本人，系统菜谱 403）。

## 视频导入

`POST /api/import/resolve` `{url?, shareText?, extraText?}`（url 与 shareText 至少一个）→
```ts
{ platform: 'bilibili'|'douyin'|'unknown', videoId?, title?, description?,
  coverUrl?, author?, durationSec?, bvid?, canEmbed: boolean,
  transcript?: string, warnings: string[] }
```
bilibili 的 iframe：`https://player.bilibili.com/player.html?bvid={bvid}&page=1&high_quality=1&danmaku=0`
抖音解析失败仍返回 200 + 部分信息 + warnings。

`POST /api/ai/extract` `{text: string, title?: string, platform?: string, extraText?: string}` → `RecipeDraft`
```ts
{ title, description, categoryName, meatType, difficulty: 1|2|3, servings,
  cookMinutes, spiceLevel: 0|1|2|3, tags: string[], tips?: string,
  ingredients: [{name, amount, unit}], steps: string[] }
```
未配置 AI → `code=400`，message=`请先在设置中配置 AI 模型`。

## AI 服务商配置

`GET /api/ai/providers` → `AiProvider[]`（**绝不返回 apiKey 明文**）
`AiProvider = { id, name, providerType, baseUrl, chatModel, imageModel, hasKey, keyMasked, enabled, isDefault, lastTestOk }`
`POST /api/ai/providers` `AiProviderInput` → `AiProvider`
`PUT /api/ai/providers/{id}` `AiProviderInput`（`apiKey` 为空/省略 = 不修改）→ `AiProvider`
`DELETE /api/ai/providers/{id}` → `null`
`POST /api/ai/providers/{id}/test` → `{ ok: boolean, message: string, latencyMs: number }`

```ts
AiProviderInput = { name, providerType, baseUrl, chatModel, imageModel?,
                    apiKey?, enabled?, isDefault? }
```
providerType 预设：`openai|deepseek|qwen|moonshot|siliconflow|ollama|custom`

`POST /api/ai/recipes/{id}/summary` → `{ aiSummary, ingredientsBrief: string[], pairing: string }`
`POST /api/ai/recipes/{id}/image` → `{ coverUrl, aiGenerated: boolean, warning?: string }`
（未配置图像模型 / 调用失败时后端生成 SVG 兜底封面，`aiGenerated=false` 且可能带 warning）

`GET /api/files/{filename}` → 图片二进制（公开访问）

## 用餐偏好

`GET /api/preferences` → `Preferences`；不存在时返回默认值（不落库）
`PUT /api/preferences` `Preferences` → `Preferences`
```ts
Preferences = { peopleCount: number,      // 1–10
  mealsPerDay: number,                    // 1–3
  dishesPerMeal: number,                  // 1–6
  meatRatio: number, vegRatio: number,    // 荤素比，>=0 且不同时为 0
  maxCookMinutes: number,                 // 15–240
  spiceLevel: number,                     // 0–3，0 表示不吃辣
  avoidIngredients: string[],             // 忌口食材
  preferredCategories: number[] }         // 偏爱分类 id
```

## 今日菜单

`GET /api/menu/today?date=2026-11-06&regenerate=false` → `MenuPlan`
```ts
MenuPlan = { date: 'YYYY-MM-DD', seq: number, peopleCount, mealsPerDay,
  meals: [{ key: 'breakfast'|'lunch'|'dinner', label: '早餐'|'午餐'|'晚餐',
            dishes: Dish[] }], warnings: string[] }
Dish = { recipeId, title, coverUrl, meatType, spiceLevel, cookMinutes,
         baseServings, ingredients: [{name, amount, unit}] }
```
`POST /api/menu/today/reroll` `{date, seq, mealKey, index}` → `MenuPlan`（seq+1）
`POST /api/menu/today/save` `{date, plan: MenuPlan}` → `null`
同日同 seq 结果稳定；`regenerate=true` 改变 seq 与组合。

## 健康检查

`GET /api/health` → `{ status: 'UP', time, db: 'UP'|'DOWN' }`
