import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * 响应式媒体查询。SSR / jsdom 没有 matchMedia 时安全降级为 false。
 * 主要用于「桌面用顶部导航 + 侧栏筛选，移动端用底部 Tab + 底部弹层」的分支。
 */
export function useMediaQuery(query: string): Ref<boolean> {
  const matches = ref(false)

  const getMql = (): MediaQueryList | null => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return null
    return window.matchMedia(query)
  }

  let mql: MediaQueryList | null = null
  const handler = (event: MediaQueryListEvent | MediaQueryList) => {
    matches.value = event.matches
  }

  onMounted(() => {
    mql = getMql()
    if (!mql) return
    matches.value = mql.matches
    if (typeof mql.addEventListener === 'function') {
      mql.addEventListener('change', handler)
    } else {
      // 老版 Safari 回退
      ;(mql as MediaQueryList & { addListener?: (cb: typeof handler) => void }).addListener?.(
        handler,
      )
    }
  })

  onBeforeUnmount(() => {
    if (!mql) return
    if (typeof mql.removeEventListener === 'function') {
      mql.removeEventListener('change', handler)
    } else {
      ;(mql as MediaQueryList & { removeListener?: (cb: typeof handler) => void }).removeListener?.(
        handler,
      )
    }
  })

  return matches
}
