import { ref, type Ref } from 'vue'

import { ApiError } from '@/api/http'

export interface UseAsyncOptions {
  /** 是否在创建时立即执行（默认 false） */
  immediate?: boolean
  /** 失败时是否静默（不改变 error 状态，交由全局 Toast 处理） */
  silentError?: boolean
}

export interface UseAsyncResult<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  error: Ref<string | null>
  run: (...args: never[]) => Promise<T | null>
  reset: () => void
}

/**
 * 轻量异步包装：统一 loading / error / data 三态，避免每个页面重复写样板。
 * 具体参数千差万别时，页面直接用 try/catch + ref 也完全可以（本类只用于简单加载）。
 */
export function useAsync<T>(
  loader: () => Promise<T>,
  options: UseAsyncOptions = {},
): UseAsyncResult<T> {
  const data = ref<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const error = ref<string | null>(null)

  let running = 0

  async function run(): Promise<T | null> {
    const current = ++running
    loading.value = true
    error.value = null
    try {
      const result = await loader()
      // 只接受最后一次调用的结果，避免快速切换时旧响应覆盖新数据
      if (current === running) data.value = result
      return result
    } catch (err) {
      if (current === running && !options.silentError) {
        error.value = err instanceof ApiError ? err.message : '加载失败，请稍后重试'
      }
      return null
    } finally {
      if (current === running) loading.value = false
    }
  }

  function reset(): void {
    data.value = null
    error.value = null
    loading.value = false
  }

  if (options.immediate) void run()

  return { data, loading, error, run, reset }
}
