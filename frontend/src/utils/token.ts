/** token 持久化：只保存 JWT 字符串，不保存任何 AI Key */
const TOKEN_KEY = 'menu.token'

const storage = (): Storage | null => {
  try {
    return typeof window !== 'undefined' ? window.localStorage : null
  } catch {
    return null
  }
}

export function getToken(): string | null {
  return storage()?.getItem(TOKEN_KEY) ?? null
}

export function setToken(token: string): void {
  storage()?.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  storage()?.removeItem(TOKEN_KEY)
}
