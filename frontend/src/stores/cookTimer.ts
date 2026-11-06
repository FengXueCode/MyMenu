import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  COOK_TIMER_STORAGE_KEY,
  LEGACY_COOK_TIMER_PREFIX,
  formatTimerClock,
  parseLegacyTimer,
  parseTimerSnapshot,
  resolveElapsedMs,
  type CookTimerSnapshot,
} from '@/utils/cookTimer'

/** 与后端 CookSessionRequest 的上限保持一致，超过则视为过期计时 */
const MAX_DURATION_MS = 12 * 3600 * 1000

/**
 * 全局开火计时器：同一时间只保留一个，状态存 localStorage。
 * 详情页负责记录接口调用，悬浮条负责跨页面展示。
 */
export const useCookTimerStore = defineStore('cookTimer', () => {
  const recipeId = ref<number | null>(null)
  const recipeTitle = ref('')
  const running = ref(false)
  const startedAt = ref<number | null>(null)
  const accumulatedMs = ref(0)
  const elapsedMs = ref(0)

  let ticker: ReturnType<typeof setInterval> | null = null

  const isActive = computed(() => running.value || elapsedMs.value > 0)
  const clock = computed(() => formatTimerClock(elapsedMs.value))
  const statusText = computed(() =>
    running.value ? '计时中' : isActive.value ? '已暂停' : '未开始',
  )

  function snapshot(): CookTimerSnapshot {
    return {
      recipeId: recipeId.value ?? 0,
      recipeTitle: recipeTitle.value,
      running: running.value,
      startedAt: startedAt.value,
      accumulatedMs: accumulatedMs.value,
    }
  }

  function sync(now = Date.now()): void {
    elapsedMs.value = resolveElapsedMs(snapshot(), now)
  }

  function startTicker(): void {
    if (!ticker) ticker = setInterval(sync, 250)
  }

  function stopTicker(): void {
    if (!ticker) return
    clearInterval(ticker)
    ticker = null
  }

  function persist(): void {
    if (!recipeId.value) return
    try {
      localStorage.setItem(COOK_TIMER_STORAGE_KEY, JSON.stringify(snapshot()))
    } catch {
      // 隐私模式下 localStorage 不可写时，计时仍可在当前页面使用
    }
  }

  function clearStorage(): void {
    try {
      localStorage.removeItem(COOK_TIMER_STORAGE_KEY)
    } catch {
      // ignore
    }
  }

  function apply(saved: CookTimerSnapshot): void {
    recipeId.value = saved.recipeId
    recipeTitle.value = saved.recipeTitle
    running.value = saved.running
    startedAt.value = saved.startedAt
    accumulatedMs.value = saved.accumulatedMs
    sync()
    if (running.value) startTicker()
  }

  /** 从 localStorage 恢复；超过后端允许的上限时直接丢弃 */
  function hydrate(): void {
    try {
      const saved = parseTimerSnapshot(localStorage.getItem(COOK_TIMER_STORAGE_KEY))
      if (!saved) return
      if (resolveElapsedMs(saved) > MAX_DURATION_MS) {
        clearStorage()
        return
      }
      apply(saved)
    } catch {
      clearStorage()
    }
  }

  /** 迁移旧版按菜谱存储的计时状态（menu:cook-timer:{id}） */
  function adoptLegacy(id: number, title: string): void {
    if (recipeId.value !== null) return
    const key = `${LEGACY_COOK_TIMER_PREFIX}${id}`
    try {
      const saved = parseLegacyTimer(localStorage.getItem(key), id, title)
      if (!saved) return
      localStorage.removeItem(key)
      if (resolveElapsedMs(saved) > MAX_DURATION_MS) return
      apply(saved)
      persist()
    } catch {
      // 旧数据损坏时忽略
    }
  }

  /** 开始或继续计时；换一道菜时会覆盖上一道（由页面先做确认） */
  function start(id: number, title: string): void {
    if (recipeId.value !== id) {
      stopTicker()
      running.value = false
      startedAt.value = null
      accumulatedMs.value = 0
      elapsedMs.value = 0
    }
    recipeId.value = id
    recipeTitle.value = title
    if (!running.value) {
      running.value = true
      startedAt.value = Date.now()
    }
    sync()
    startTicker()
    persist()
  }

  function pause(): void {
    if (!running.value) return
    sync()
    accumulatedMs.value = elapsedMs.value
    running.value = false
    startedAt.value = null
    stopTicker()
    persist()
  }

  function reset(): void {
    stopTicker()
    recipeId.value = null
    recipeTitle.value = ''
    running.value = false
    startedAt.value = null
    accumulatedMs.value = 0
    elapsedMs.value = 0
    clearStorage()
  }

  hydrate()

  return {
    recipeId,
    recipeTitle,
    running,
    elapsedMs,
    isActive,
    clock,
    statusText,
    sync,
    start,
    pause,
    reset,
    adoptLegacy,
  }
})
