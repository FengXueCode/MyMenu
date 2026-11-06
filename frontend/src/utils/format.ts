/**
 * 展示相关的格式化工具：时长、日期、枚举中文文案、配色映射。
 * 所有文案面向「灶台手账」的中文界面，不在这里发起任何请求。
 */
import type { Difficulty, MeatType, MealKey, SpiceLevel, SourcePlatform } from '@/types/api'

/* --------------------------------- 时长 ---------------------------------- */

/** 分钟 → 人读时长，例如 45 → "45 分钟"，75 → "1 小时 15 分" */
export function formatDuration(minutes: number | null | undefined): string {
  if (minutes == null || Number.isNaN(Number(minutes)) || Number(minutes) <= 0) return '—'
  const total = Math.round(Number(minutes))
  if (total < 60) return `${total} 分钟`
  const hours = Math.floor(total / 60)
  const mins = total % 60
  return mins === 0 ? `${hours} 小时` : `${hours} 小时 ${mins} 分`
}

/** 秒 → 人读时长，例如 90 → "1 分 30 秒" */
export function formatSeconds(seconds: number | null | undefined): string {
  if (seconds == null || Number.isNaN(Number(seconds)) || Number(seconds) <= 0) return ''
  const total = Math.round(Number(seconds))
  if (total < 60) return `${total} 秒`
  const mins = Math.floor(total / 60)
  const rest = total % 60
  return rest === 0 ? `${mins} 分钟` : `${mins} 分 ${rest} 秒`
}

/* --------------------------------- 日期 ---------------------------------- */

/** 本地日期 yyyy-MM-dd（后端 /menu/today 的 date 参数格式） */
export function todayISO(date: Date = new Date()): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'] as const

/** 2026-11-06 → "11 月 6 日 · 周五" */
export function formatDateCN(value: string | Date | null | undefined): string {
  if (!value) return ''
  const date = typeof value === 'string' ? parseDate(value) : value
  if (!date || Number.isNaN(date.getTime())) return typeof value === 'string' ? value : ''
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日 · ${WEEKDAYS[date.getDay()]}`
}

/** "2026-11-06T10:00:00" → "11 月 6 日"（不带星期） */
export function formatMonthDay(value: string | Date | null | undefined): string {
  if (!value) return ''
  const date = typeof value === 'string' ? parseDate(value) : value
  if (!date || Number.isNaN(date.getTime())) return typeof value === 'string' ? value : ''
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日`
}

/** 相对时间：刚刚 / x 分钟前 / x 小时前 / x 天前 / 具体日期 */
export function formatRelative(value: string | null | undefined): string {
  if (!value) return ''
  const date = parseDate(value)
  if (!date || Number.isNaN(date.getTime())) return value
  const diff = Date.now() - date.getTime()
  if (diff < 0) return formatMonthDay(date)
  const minute = 60_000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < day * 7) return `${Math.floor(diff / day)} 天前`
  return formatMonthDay(date)
}

function parseDate(value: string): Date | null {
  // 兼容 "2026-11-06 10:00:00"（Safari 不接受空格分隔）
  const normalized = /^\d{4}-\d{2}-\d{2} \d{2}/.test(value) ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

/* --------------------------------- 问候 ---------------------------------- */

/** 按当前时刻返回问候语 */
export function greeting(date: Date = new Date()): string {
  const hour = date.getHours()
  if (hour < 5) return '夜里的灶台还热着'
  if (hour < 9) return '早上好'
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 17) return '下午好'
  if (hour < 21) return '晚上好'
  return '夜里好'
}

/* ------------------------------ 枚举文案映射 ------------------------------ */

interface MeatTypeMeta {
  label: string
  emoji: string
  /** Tailwind 类：浅底 + 文字色 */
  chip: string
  /** 左侧竖条 / 圆点颜色 */
  dot: string
}

export const MEAT_TYPE_META: Record<MeatType, MeatTypeMeta> = {
  MEAT: {
    label: '荤',
    emoji: '🥩',
    chip: 'bg-tomato-soft text-tomato-deep',
    dot: 'bg-tomato',
  },
  VEG: {
    label: '素',
    emoji: '🥬',
    chip: 'bg-herb-soft text-herb',
    dot: 'bg-herb',
  },
  SOUP: {
    label: '汤羹',
    emoji: '🍲',
    chip: 'bg-ginger-soft text-[#8a6215]',
    dot: 'bg-ginger',
  },
  STAPLE: {
    label: '主食',
    emoji: '🍚',
    chip: 'bg-paper-deep text-ink-soft',
    dot: 'bg-line-strong',
  },
  DESSERT: {
    label: '甜点',
    emoji: '🍰',
    chip: 'bg-[#f3e2ea] text-[#8f4560]',
    dot: 'bg-[#c07a92]',
  },
  OTHER: {
    label: '其他',
    emoji: '🍴',
    chip: 'bg-paper-deep text-ink-muted',
    dot: 'bg-ink-muted',
  },
}

export const MEAT_TYPE_OPTIONS: { value: MeatType; label: string; emoji: string }[] = (
  Object.keys(MEAT_TYPE_META) as MeatType[]
).map((value) => ({
  value,
  label: MEAT_TYPE_META[value].label,
  emoji: MEAT_TYPE_META[value].emoji,
}))

export function meatTypeLabel(type: MeatType | null | undefined): string {
  return type ? (MEAT_TYPE_META[type]?.label ?? '其他') : '其他'
}

export function meatTypeEmoji(type: MeatType | null | undefined): string {
  return type ? (MEAT_TYPE_META[type]?.emoji ?? '🍴') : '🍴'
}

export const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  1: '简单',
  2: '中等',
  3: '进阶',
}

export function difficultyLabel(value: Difficulty | number | null | undefined): string {
  const level = Number(value) as Difficulty
  return DIFFICULTY_LABEL[level] ?? '简单'
}

export const SPICE_LABEL: Record<SpiceLevel, string> = {
  0: '不辣',
  1: '微辣',
  2: '中辣',
  3: '重辣',
}

export function spiceLabel(value: number | null | undefined): string {
  const level = (Number(value ?? 0) || 0) as SpiceLevel
  return SPICE_LABEL[level] ?? '不辣'
}

export function spiceEmoji(value: number | null | undefined): string {
  const level = Number(value ?? 0) || 0
  if (level <= 0) return ''
  return '🌶️'.repeat(Math.min(level, 3))
}

export const MEAL_LABEL: Record<MealKey, string> = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
}

export const MEAL_EMOJI: Record<MealKey, string> = {
  breakfast: '🌅',
  lunch: '☀️',
  dinner: '🌙',
}

export function mealLabel(key: MealKey | string | null | undefined, fallback = ''): string {
  if (!key) return fallback
  return MEAL_LABEL[key as MealKey] ?? fallback ?? key
}

export const PLATFORM_LABEL: Record<SourcePlatform, string> = {
  bilibili: 'B 站',
  douyin: '抖音',
  unknown: '外部链接',
}

export function platformLabel(platform: string | null | undefined): string {
  return PLATFORM_LABEL[platform as SourcePlatform] ?? '外部链接'
}

/* -------------------------------- 其他杂项 -------------------------------- */

/** 昵称首字（头像占位） */
export function initials(name: string | null | undefined): string {
  const text = (name ?? '').trim()
  if (!text) return '灶'
  // 中文取第一个字，英文取首字母
  const first = Array.from(text)[0] ?? '灶'
  return first.toUpperCase()
}

/** 数值夹取 */
export function clamp(value: number, min: number, max: number): number {
  if (Number.isNaN(value)) return min
  return Math.min(Math.max(value, min), max)
}

/** 份量文案 */
export function formatServings(servings: number | null | undefined): string {
  const n = Number(servings)
  return n > 0 ? `${n} 人份` : '—'
}

/** 空值占位 */
export function orDash(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  const text = String(value).trim()
  return text.length ? text : '—'
}

/** 截断长文本（按字符） */
export function truncate(text: string | null | undefined, max = 80): string {
  const value = (text ?? '').trim()
  if (value.length <= max) return value
  return `${value.slice(0, max).trimEnd()}…`
}
