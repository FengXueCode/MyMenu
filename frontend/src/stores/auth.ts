import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { authApi } from '@/api/auth'
import type { AuthPayload, LoginInput, RegisterInput, User } from '@/types/api'
import { clearToken, getToken, setToken } from '@/utils/token'

/**
 * 登录态：
 * - token 持久化在 localStorage（只存 JWT，不存任何 AI Key）；
 * - 初始化时若已存在 token，自动请求 /api/auth/me 校验并拉取用户信息。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<User | null>(null)
  const loading = ref(false)
  /** /me 初始化是否已结束（无论成功失败），用于避免页面闪烁 */
  const ready = ref(false)

  const isAuthed = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '厨子')
  const initial = computed(() => {
    const name = user.value?.nickname || user.value?.username || '灶'
    return Array.from(name.trim())[0]?.toUpperCase() ?? '灶'
  })

  function apply(payload: AuthPayload): void {
    token.value = payload.token
    user.value = payload.user
    setToken(payload.token)
    ready.value = true
  }

  function logout(): void {
    token.value = null
    user.value = null
    clearToken()
    ready.value = true
  }

  async function login(input: LoginInput, silent = false): Promise<User> {
    loading.value = true
    try {
      const payload = await authApi.login(input, silent)
      apply(payload)
      return payload.user
    } finally {
      loading.value = false
    }
  }

  async function register(input: RegisterInput, silent = false): Promise<User> {
    loading.value = true
    try {
      const payload = await authApi.register(input, silent)
      apply(payload)
      return payload.user
    } finally {
      loading.value = false
    }
  }

  async function fetchMe(): Promise<User | null> {
    if (!token.value) {
      ready.value = true
      return null
    }
    try {
      const me = await authApi.me()
      user.value = me
      return me
    } catch {
      // token 失效：http 层已清理并跳转登录，这里只同步本地状态
      if (!getToken()) {
        token.value = null
        user.value = null
      }
      return null
    } finally {
      ready.value = true
    }
  }

  /** 应用启动时调用：有 token 则拉取当前用户 */
  async function bootstrap(): Promise<void> {
    if (!token.value) {
      ready.value = true
      return
    }
    await fetchMe()
  }

  return {
    token,
    user,
    loading,
    ready,
    isAuthed,
    displayName,
    initial,
    apply,
    login,
    register,
    logout,
    fetchMe,
    bootstrap,
  }
})
