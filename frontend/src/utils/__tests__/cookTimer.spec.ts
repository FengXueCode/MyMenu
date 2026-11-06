import { describe, expect, it } from 'vitest'

import {
  formatTimerClock,
  parseLegacyTimer,
  parseTimerSnapshot,
  resolveElapsedMs,
  type CookTimerSnapshot,
} from '@/utils/cookTimer'

const snapshot = (patch: Partial<CookTimerSnapshot> = {}): CookTimerSnapshot => ({
  recipeId: 1,
  recipeTitle: '红烧肉',
  running: false,
  startedAt: null,
  accumulatedMs: 0,
  ...patch,
})

describe('formatTimerClock', () => {
  it('格式化为 HH:MM:SS', () => {
    expect(formatTimerClock(0)).toBe('00:00:00')
    expect(formatTimerClock(61_000)).toBe('00:01:01')
    expect(formatTimerClock(3_661_000)).toBe('01:01:01')
  })

  it('负数与非法值按 0 处理', () => {
    expect(formatTimerClock(-1000)).toBe('00:00:00')
    expect(formatTimerClock(Number.NaN)).toBe('00:00:00')
  })
})

describe('resolveElapsedMs', () => {
  it('暂停时保持累计用时', () => {
    expect(resolveElapsedMs(snapshot({ accumulatedMs: 5000 }), 99_999)).toBe(5000)
  })

  it('运行中累加已过时间', () => {
    expect(
      resolveElapsedMs(snapshot({ running: true, startedAt: 1000, accumulatedMs: 2000 }), 6000),
    ).toBe(7000)
  })

  it('系统时钟回拨时不出现负数', () => {
    expect(
      resolveElapsedMs(snapshot({ running: true, startedAt: 10_000, accumulatedMs: 1000 }), 5000),
    ).toBe(1000)
  })
})

describe('parseTimerSnapshot', () => {
  it('解析合法快照', () => {
    const parsed = parseTimerSnapshot(JSON.stringify(snapshot({ running: true, startedAt: 1000 })))
    expect(parsed?.recipeId).toBe(1)
    expect(parsed?.running).toBe(true)
    expect(parsed?.startedAt).toBe(1000)
  })

  it('缺少 startedAt 的运行态降级为暂停态', () => {
    const parsed = parseTimerSnapshot(
      JSON.stringify(snapshot({ running: true, startedAt: null, accumulatedMs: 3000 })),
    )
    expect(parsed?.running).toBe(false)
    expect(parsed?.accumulatedMs).toBe(3000)
  })

  it('非法 JSON 或菜谱 id 返回 null', () => {
    expect(parseTimerSnapshot('{')).toBeNull()
    expect(parseTimerSnapshot(JSON.stringify(snapshot({ recipeId: 0 })))).toBeNull()
  })
})

describe('parseLegacyTimer', () => {
  it('把旧版按菜谱存储的状态迁移成快照', () => {
    const parsed = parseLegacyTimer(
      JSON.stringify({ running: false, startedAt: null, accumulatedMs: 8000 }),
      3,
      '番茄炒蛋',
    )
    expect(parsed).toEqual({
      recipeId: 3,
      recipeTitle: '番茄炒蛋',
      running: false,
      startedAt: null,
      accumulatedMs: 8000,
    })
  })
})
