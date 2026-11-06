import { http } from '@/api/http'
import type { AuthPayload, LoginInput, RegisterInput, User } from '@/types/api'

export const authApi = {
  /** 登录页会内联展示错误，silent 避免同时弹全局 Toast */
  login(input: LoginInput, silent = false) {
    return http<AuthPayload>('/api/auth/login', { method: 'POST', body: input, silent })
  },
  register(input: RegisterInput, silent = false) {
    return http<AuthPayload>('/api/auth/register', { method: 'POST', body: input, silent })
  },
  me() {
    return http<User>('/api/auth/me')
  },
}
