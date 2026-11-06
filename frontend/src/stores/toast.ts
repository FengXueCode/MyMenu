import { ref } from 'vue'
import { defineStore } from 'pinia'

export type ToastKind = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  kind: ToastKind
  message: string
  duration: number
}

const DEFAULT_DURATION: Record<ToastKind, number> = {
  success: 2600,
  info: 3200,
  error: 4200,
}

/** 全局轻提示队列：API 错误、操作结果都走这里 */
export const useToastStore = defineStore('toast', () => {
  const items = ref<ToastItem[]>([])
  const timers = new Map<number, ReturnType<typeof setTimeout>>()
  let seq = 0

  function dismiss(id: number): void {
    const timer = timers.get(id)
    if (timer) {
      clearTimeout(timer)
      timers.delete(id)
    }
    items.value = items.value.filter((item) => item.id !== id)
  }

  function push(message: string, kind: ToastKind = 'info', duration?: number): number {
    const text = (message ?? '').toString().trim()
    if (!text) return -1
    // 同一时刻并发请求失败时，避免刷屏重复提示
    const last = items.value[items.value.length - 1]
    if (last && last.kind === kind && last.message === text) return last.id
    const id = ++seq
    const ttl = duration ?? DEFAULT_DURATION[kind]
    items.value = [...items.value.slice(-3), { id, kind, message: text, duration: ttl }]
    const timer = setTimeout(() => dismiss(id), ttl)
    timers.set(id, timer)
    return id
  }

  const success = (message: string, duration?: number) => push(message, 'success', duration)
  const error = (message: string, duration?: number) => push(message, 'error', duration)
  const info = (message: string, duration?: number) => push(message, 'info', duration)

  function clear(): void {
    timers.forEach((timer) => clearTimeout(timer))
    timers.clear()
    items.value = []
  }

  return { items, push, success, error, info, dismiss, clear }
})
