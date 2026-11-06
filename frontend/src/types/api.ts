/**
 * 与后端 API 契约严格对应的类型定义。
 * 契约来源：docs/api-contract.md —— 字段名、可空性、枚举值以此为准。
 */

/** 统一返回包裹：成功判断 code === 200 */
export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页返回 */
export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  pages: number
  records: T[]
}

/** 荤素类型：荤 / 素 / 汤羹 / 主食 / 甜点 / 其他 */
export type MeatType = 'MEAT' | 'VEG' | 'SOUP' | 'STAPLE' | 'DESSERT' | 'OTHER'

/** 难度：1 简单 / 2 中等 / 3 进阶 */
export type Difficulty = 1 | 2 | 3

/** 辣度：0 不辣 … 3 重辣 */
export type SpiceLevel = 0 | 1 | 2 | 3

export type MealKey = 'breakfast' | 'lunch' | 'dinner'

export type SourcePlatform = 'bilibili' | 'douyin' | 'unknown'

export type RecipeOwner = 'system' | 'mine'

/* ---------------------------------- 认证 --------------------------------- */

export interface User {
  id: number
  username: string
  nickname: string
  avatar?: string | null
}

export interface AuthPayload {
  token: string
  user: User
}

export interface LoginInput {
  username: string
  password: string
}

export interface RegisterInput {
  username: string
  password: string
  nickname?: string
}

/* ---------------------------------- 分类 --------------------------------- */

export interface Category {
  id: number
  name: string
  icon: string
  color: string
  sort: number
  isSystem: boolean
}

export interface CategoryInput {
  name: string
  icon?: string
  color?: string
}

/* ---------------------------------- 菜谱 --------------------------------- */

export interface Ingredient {
  id?: number
  name: string
  amount: string
  unit: string
  category?: string | null
  sort?: number
}

export interface Step {
  id?: number
  stepNo: number
  content: string
  durationSec?: number | null
}

export interface RecipeCard {
  id: number
  title: string
  description: string
  coverUrl: string | null
  categoryId: number | null
  categoryName: string | null
  meatType: MeatType
  difficulty: Difficulty
  servings: number
  durationMin: number
  spiceLevel: SpiceLevel
  tags: string[]
  aiSummary: string | null
  sourcePlatform: SourcePlatform | null
  sourceUrl: string | null
  sourceAuthor: string | null
  owner: RecipeOwner
  createdAt: string
}

export interface RecipeDetail extends RecipeCard {
  ingredients: Ingredient[]
  steps: Step[]
}

export interface CookSessionResult {
  actualMinutes: number
  estimateMinutes: number
  updatedEstimate: boolean
}

/** POST/PUT /api/recipes 请求体 */
export interface RecipeUpsertInput {
  title: string
  description: string
  categoryId: number | null
  coverUrl: string | null
  sourcePlatform: SourcePlatform | null
  sourceUrl: string | null
  sourceAuthor: string | null
  durationMin: number
  difficulty: Difficulty
  servings: number
  meatType: MeatType
  spiceLevel: SpiceLevel
  tags: string[]
  ingredients: Ingredient[]
  steps: Step[]
}

export interface RecipeQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  categoryId?: number | null
  meatType?: MeatType | null
  mine?: boolean
}

/* --------------------------------- 视频导入 -------------------------------- */

export interface ImportResolveInput {
  url?: string
  shareText?: string
  extraText?: string
}

export interface ImportResolveResult {
  platform: SourcePlatform
  videoId?: string
  title?: string
  description?: string
  coverUrl?: string
  author?: string
  durationSec?: number
  bvid?: string
  canEmbed: boolean
  transcript?: string
  warnings: string[]
}

/** AI 抽取出的菜谱草稿（未落库） */
export interface RecipeDraft {
  title: string
  description: string
  categoryName: string
  meatType: MeatType
  difficulty: Difficulty
  servings: number
  cookMinutes: number
  spiceLevel: SpiceLevel
  tags: string[]
  tips?: string
  ingredients: Ingredient[]
  steps: string[]
}

export interface AiExtractInput {
  text: string
  title?: string
  platform?: string
  extraText?: string
}

/* -------------------------------- AI 服务商 ------------------------------- */

export type ProviderType =
  | 'openai'
  | 'deepseek'
  | 'qwen'
  | 'moonshot'
  | 'siliconflow'
  | 'opencode'
  | 'ollama'
  | 'custom'

export interface AiProvider {
  id: number
  name: string
  providerType: ProviderType
  baseUrl: string
  chatModel: string
  imageModel: string | null
  /** 是否已配置 Key —— 后端永不明文返回 Key */
  hasKey: boolean
  /** 掩码，如 sk-****abcd */
  keyMasked: string | null
  enabled: boolean
  isDefault: boolean
  lastTestOk: boolean | null
}

/** 新增/更新配置；apiKey 省略或为空字符串表示不修改已存 Key */
export interface AiProviderInput {
  name: string
  providerType: ProviderType
  baseUrl: string
  chatModel: string
  imageModel?: string | null
  apiKey?: string
  enabled?: boolean
  isDefault?: boolean
}

export interface ProviderTestResult {
  ok: boolean
  message: string
  latencyMs: number
}

export interface AiSummaryResult {
  aiSummary: string
  ingredientsBrief: string[]
  pairing: string
}

export interface AiImageResult {
  coverUrl: string
  aiGenerated: boolean
  warning?: string
}

/* --------------------------------- 用餐偏好 -------------------------------- */

export interface Preferences {
  peopleCount: number
  mealsPerDay: number
  dishesPerMeal: number
  meatRatio: number
  vegRatio: number
  maxCookMinutes: number
  spiceLevel: number
  avoidIngredients: string[]
  preferredCategories: number[]
}

/* --------------------------------- 今日菜单 -------------------------------- */

export interface Dish {
  recipeId: number
  title: string
  coverUrl: string | null
  meatType: MeatType
  spiceLevel: SpiceLevel
  cookMinutes: number
  baseServings: number
  ingredients: Ingredient[]
}

export interface Meal {
  key: MealKey
  label: string
  dishes: Dish[]
}

export interface MenuPlan {
  date: string
  seq: number
  peopleCount: number
  mealsPerDay: number
  meals: Meal[]
  warnings: string[]
}

export interface RerollInput {
  date: string
  seq: number
  mealKey: MealKey
  index: number
}

export interface SaveMenuInput {
  date: string
  plan: MenuPlan
}

/* --------------------------------- 健康检查 -------------------------------- */

export interface HealthResult {
  status: 'UP' | string
  time: string
  db: 'UP' | 'DOWN' | string
}

/* ------------------------------- 视频账号/收藏夹 ------------------------------- */

export interface VideoAccount {
  id: number
  platform: 'bilibili' | string
  nickname: string
  avatar: string | null
  status: 'ACTIVE' | 'EXPIRED' | string
  lastLoginAt: string | null
}

export interface IntegrationSource {
  id: number
  platform: 'bilibili' | 'douyin' | string
  sourceType: 'BILIBILI_FOLDER' | 'DOUYIN_QUEUE' | string
  folderId: string | null
  folderName: string | null
  sourceUrl: string | null
  autoScan: boolean
  scanLimit: number
  lastScanStatus: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'IDLE' | string | null
  lastScanAt: string | null
  lastScanMessage: string | null
  lastScanAdded: number
}

export interface IntegrationItem {
  id: number
  platform: 'bilibili' | 'douyin' | string
  videoId: string
  videoUrl: string | null
  title: string | null
  coverUrl: string | null
  author: string | null
  status: 'PENDING' | 'PROCESSING' | 'IMPORTED' | 'SKIPPED' | 'FAILED' | string
  recipeId: number | null
  recipeTitle: string | null
  reason: string | null
  createdAt: string
  processedAt: string | null
}

export interface QueueStats {
  pending: number
  imported: number
  skipped: number
  failed: number
}

export interface IntegrationStatus {
  bilibili?: VideoAccount | null
  sources: IntegrationSource[]
  queue: QueueStats
  recentItems: IntegrationItem[]
}

export interface QrStartResult {
  key: string
  qrImage: string
}

export interface QrPollResult {
  status: 'WAITING' | 'SCANNED' | 'CONFIRMED' | 'EXPIRED' | 'FAILED' | string
  account?: VideoAccount | null
  message: string
}

export interface ScanStartResult {
  status: string
  message: string
}

export interface QueueResult {
  added: number
  duplicates: number
  invalid: number
  total: number
}

