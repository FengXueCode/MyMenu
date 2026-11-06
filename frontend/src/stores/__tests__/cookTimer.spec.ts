import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { useCookTimerStore } from '@/stores/cookTimer'
import { COOK_TIMER_STORAGE_KEY } from '@/utils/cookTimer'

describe('cookTimer store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-11-06T10:00:00'))
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
  })

  it('暂停期间不计入实际用时', () => {
    const timer = useCookTimerStore()
    timer.start(1, '红烧肉')
    vi.advanceTimersByTime(61_000)
    expect(timer.clock).toBe('00:01:01')
    expect(timer.statusText).toBe('计时中')

    timer.pause()
    vi.advanceTimersByTime(30_000)
    expect(timer.clock).toBe('00:01:01')
    expect(timer.statusText).toBe('已暂停')

    timer.start(1, '红烧肉')
    vi.advanceTimersByTime(5_000)
    expect(timer.clock).toBe('00:01:06')
  })

  it('暂停后刷新页面能从 localStorage 恢复', () => {
    const timer = useCookTimerStore()
    timer.start(7, '番茄炒蛋')
    vi.advanceTimersByTime(10_000)
    timer.pause()
    expect(localStorage.getItem(COOK_TIMER_STORAGE_KEY)).not.toBeNull()

    setActivePinia(createPinia())
    const restored = useCookTimerStore()
    expect(restored.recipeId).toBe(7)
    expect(restored.recipeTitle).toBe('番茄炒蛋')
    expect(restored.elapsedMs).toBe(10_000)
    expect(restored.statusText).toBe('已暂停')
  })

  it('换一道菜会覆盖上一道计时', () => {
    const timer = useCookTimerStore()
    timer.start(1, '红烧肉')
    vi.advanceTimersByTime(5_000)

    timer.start(2, '清炒时蔬')
    expect(timer.recipeId).toBe(2)
    expect(timer.recipeTitle).toBe('清炒时蔬')
    expect(timer.elapsedMs).toBe(0)
    expect(timer.running).toBe(true)
  })

  it('放弃后清空状态与存储', () => {
    const timer = useCookTimerStore()
    timer.start(1, '红烧肉')
    vi.advanceTimersByTime(5_000)

    timer.reset()
    expect(timer.recipeId).toBeNull()
    expect(timer.isActive).toBe(false)
    expect(localStorage.getItem(COOK_TIMER_STORAGE_KEY)).toBeNull()
  })
})
