/**
 * 开火计时的纯计算与持久化结构。
 *
 * 口径约定：
 * - 暂停期间不计入实际用时；
 * - 实际用时按秒记录，预估分钟数取整规则由后端统一处理；
 * - 同一时间只保留一个计时器，状态存在 localStorage，刷新/切页后继续。
 */

export const COOK_TIMER_STORAGE_KEY = 'menu:cook-timer'
export const LEGACY_COOK_TIMER_PREFIX = 'menu:cook-timer:'

export interface CookTimerSnapshot {
  recipeId: number
  recipeTitle: string
  running: boolean
  startedAt: number | null
  accumulatedMs: number
}

/** 毫秒 → "HH:MM:SS"（小时不封顶） */
export function formatTimerClock(ms: number): string {
  const total = Math.floor(Math.max(0, Number(ms) || 0) / 1000)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}

/** 根据快照和当前时刻算出实际用时（暂停期间不增长） */
export function resolveElapsedMs(snapshot: CookTimerSnapshot, now = Date.now()): number {
  const base = Math.max(0, Number(snapshot.accumulatedMs) || 0)
  if (!snapshot.running || !snapshot.startedAt) return base
  return base + Math.max(0, now - snapshot.startedAt)
}

function validStartedAt(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : null
}

/** 解析 localStorage 里的计时快照，字段不合法时返回 null */
export function parseTimerSnapshot(raw: string | null): CookTimerSnapshot | null {
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as Partial<CookTimerSnapshot>
    const recipeId = Number(value.recipeId)
    const accumulatedMs = Number(value.accumulatedMs)
    if (!Number.isFinite(recipeId) || recipeId <= 0) return null
    if (!Number.isFinite(accumulatedMs) || accumulatedMs < 0) return null
    const startedAt = validStartedAt(value.startedAt)
    const running = value.running === true && startedAt !== null
    return {
      recipeId,
      recipeTitle: typeof value.recipeTitle === 'string' ? value.recipeTitle : '',
      running,
      startedAt,
      accumulatedMs,
    }
  } catch {
    return null
  }
}

/** 解析旧版按菜谱分开存的计时（menu:cook-timer:{id}），用于平滑迁移 */
export function parseLegacyTimer(
  raw: string | null,
  recipeId: number,
  recipeTitle: string,
): CookTimerSnapshot | null {
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as {
      running?: boolean
      startedAt?: number | null
      accumulatedMs?: number
    }
    const accumulatedMs = Number(value.accumulatedMs)
    if (!Number.isFinite(accumulatedMs) || accumulatedMs < 0) return null
    const startedAt = validStartedAt(value.startedAt)
    const running = value.running === true && startedAt !== null
    return {
      recipeId,
      recipeTitle,
      running,
      startedAt,
      accumulatedMs,
    }
  } catch {
    return null
  }
}
