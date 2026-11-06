/**
 * 配料用量缩放。
 *
 * 规则（与后端 PortionScaler 保持一致的口径）：
 * - 缩放系数 ratio = 目标人数 / 菜谱基准份量；
 * - 质量/体积单位（g、ml、kg、l）取易读值：≥100 取 10 的倍数，≥20 取 5 的倍数，其余取整数；
 * - 计件单位（个、只、颗、根、瓣、片…）向上取整，避免出现 "1.3 个鸡蛋"；
 * - 无法解析的用量（"适量"、"少许"、"半勺"以外的自由文本）保持原样，不做猜测。
 */
import type { Dish, Ingredient } from '@/types/api'

/** 质量 / 体积单位 —— 按数值取易读倍数 */
const MASS_UNITS = new Set([
  'g',
  '克',
  'gram',
  'grams',
  'kg',
  '千克',
  '公斤',
  'ml',
  '毫升',
  'l',
  '升',
  'L',
  'ML',
])

/** 计件单位 —— 结果向上取整 */
const COUNT_UNITS = new Set([
  '个',
  '只',
  '颗',
  '根',
  '头',
  '瓣',
  '片',
  '块',
  '条',
  '张',
  '把',
  '勺',
  '茶匙',
  '汤匙',
  '大勺',
  '小勺',
  '杯',
  '碗',
  '份',
  '枚',
  '束',
  '粒',
  '支',
  '盒',
  '罐',
  '包',
  '朵',
  '撮',
  '挂',
  'pcs',
  'piece',
  'pieces',
])

const CHINESE_NUM: Record<string, number> = {
  半: 0.5,
  一: 1,
  二: 2,
  两: 2,
  三: 3,
  四: 4,
  五: 5,
  六: 6,
  七: 7,
  八: 8,
  九: 9,
  十: 10,
}

/**
 * 解析用量字符串，支持：
 * `300` / `1.5` / `1/2` / `1 1/2` / `2-3`（取中间值）/ `约200` / `半`。
 * 无法解析时返回 null（例如 "适量"）。
 */
export function parseAmount(raw: string | number | null | undefined): number | null {
  if (raw == null) return null
  if (typeof raw === 'number') return Number.isFinite(raw) ? raw : null

  const text = raw.trim()
  if (!text) return null

  // 纯中文数字（半 / 一 / 两 …）
  if (text.length <= 2 && CHINESE_NUM[text] !== undefined) return CHINESE_NUM[text]

  // 分数：1/2、1 1/2
  const fraction = text.match(/(\d+(?:\.\d+)?)\s*\/\s*(\d+(?:\.\d+)?)/)
  if (fraction) {
    const numerator = Number(fraction[1])
    const denominator = Number(fraction[2])
    if (denominator > 0) {
      const whole = text.match(/^(\d+(?:\.\d+)?)\s+\d+\s*\/\s*\d+$/)
      const wholePart = whole ? Number(whole[1]) : 0
      return wholePart + numerator / denominator
    }
  }

  // 区间：2-3 / 2~3 / 2 到 3，取中间值
  const range = text.match(/(\d+(?:\.\d+)?)\s*[-~至到]\s*(\d+(?:\.\d+)?)/)
  if (range) {
    const low = Number(range[1])
    const high = Number(range[2])
    return (low + high) / 2
  }

  // 普通数字（可带前后缀，如 "约200" / "200左右" / "1.5斤"）
  const plain = text.match(/(\d+(?:\.\d+)?)/)
  if (plain) {
    const value = Number(plain[1])
    return Number.isFinite(value) ? value : null
  }

  return null
}

/** 数字 → 简洁字符串（去掉多余的 .0） */
export function formatNumber(value: number): string {
  if (!Number.isFinite(value)) return '0'
  const rounded = Math.round(value * 10) / 10
  return Number.isInteger(rounded) ? String(rounded) : String(rounded)
}

/** 把数值按单位取易读值 */
export function roundReadable(value: number, unit: string): number {
  if (!Number.isFinite(value) || value <= 0) return 0
  const normalized = (unit ?? '').trim()

  if (MASS_UNITS.has(normalized)) {
    if (normalized === 'kg' || normalized === '千克' || normalized === '公斤') {
      // 千克保留 1 位小数，最小 0.1
      return Math.max(0.1, Math.round(value * 10) / 10)
    }
    if (normalized === 'l' || normalized === 'L' || normalized === '升') {
      return Math.max(0.1, Math.round(value * 10) / 10)
    }
    if (value >= 100) return Math.round(value / 10) * 10
    if (value >= 20) return Math.round(value / 5) * 5
    if (value >= 10) return Math.round(value)
    return Math.round(value * 10) / 10
  }

  if (COUNT_UNITS.has(normalized)) {
    return Math.max(1, Math.ceil(value))
  }

  // 未知单位：保留 1 位小数，至少 0.1
  if (value < 0.1) return 0.1
  return Math.round(value * 10) / 10
}

/** 单个用量按比例缩放后格式化，无法解析时原样返回 */
export function scaleAmount(
  amount: string | number | null | undefined,
  ratio: number,
  unit = '',
): string {
  const original = amount == null ? '' : String(amount)
  const value = parseAmount(amount)
  if (value == null || !Number.isFinite(ratio) || ratio <= 0) return original
  if (value === 0) return original
  return formatNumber(roundReadable(value * ratio, unit))
}

/**
 * 按人数缩放配料表。
 * @param ingredients 原始配料
 * @param fromServings 菜谱基准份量
 * @param toServings 目标人数
 */
export function scaleIngredients(
  ingredients: Ingredient[],
  fromServings: number | null | undefined,
  toServings: number | null | undefined,
): Ingredient[] {
  const from = Number(fromServings)
  const to = Number(toServings)
  const safeFrom = Number.isFinite(from) && from > 0 ? from : 1
  const safeTo = Number.isFinite(to) && to > 0 ? to : safeFrom
  const ratio = safeTo / safeFrom

  if (!ingredients?.length) return []
  if (ratio === 1) return ingredients.map((item) => ({ ...item }))

  return ingredients.map((item) => ({
    ...item,
    amount: scaleAmount(item.amount, ratio, item.unit),
  }))
}

/** 今日菜单里单道菜的配料按人数缩放 */
export function scaleDishIngredients(
  dish: Pick<Dish, 'ingredients' | 'baseServings'>,
  peopleCount: number,
): Ingredient[] {
  return scaleIngredients(dish.ingredients, dish.baseServings, peopleCount)
}

/** 一份配料拼成一行文字，如「五花肉 300 g」 */
export function ingredientLine(ingredient: Ingredient): string {
  const amount = (ingredient.amount ?? '').toString().trim()
  const unit = (ingredient.unit ?? '').toString().trim()
  const name = (ingredient.name ?? '').toString().trim()
  const measure = [amount, unit].filter(Boolean).join(' ')
  return measure ? `${name} ${measure}` : name
}
