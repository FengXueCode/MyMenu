/**
 * 统一 HTTP 层：
 * - 自动拼接 Vite 代理的 /api 路径与查询参数；
 * - 自动附加 Authorization: Bearer <token>；
 * - 解包后端统一返回 Result{code,message,data,timestamp}，成功（code === 200）直接返回 data；
 * - 401：清空 token → 回调跳转 /login?redirect=当前页；
 * - 其他失败：抛出 ApiError，并由注册的 Toast 通知器展示错误文案。
 */
import type { Result } from '@/types/api'
import { clearToken, getToken } from '@/utils/token'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export type QueryValue = string | number | boolean | null | undefined

export interface RequestOptions {
  method?: HttpMethod
  body?: unknown
  query?: Record<string, QueryValue>
  /** 静默模式：不弹 Toast（页面自己展示错误时使用） */
  silent?: boolean
  signal?: AbortSignal
}

export class ApiError extends Error {
  readonly code: number
  readonly status: number

  constructor(message: string, code: number, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

type ErrorNotifier = (message: string) => void
type UnauthorizedHandler = () => void

let errorNotifier: ErrorNotifier | null = null
let unauthorizedHandler: UnauthorizedHandler | null = null

/** 由 main.ts 注册：把 API 错误接到全局 Toast */
export function setApiErrorNotifier(notifier: ErrorNotifier | null): void {
  errorNotifier = notifier
}

/** 由 main.ts 注册：401 时清空登录态并跳转登录页 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

const STATUS_MESSAGE: Record<number, string> = {
  400: '请求参数有误',
  401: '登录已过期，请重新登录',
  403: '没有权限执行该操作',
  404: '请求的内容不存在',
  409: '数据冲突，请刷新后重试',
  429: '操作太频繁，请稍后再试',
  500: '服务开小差了，请稍后再试',
  502: '网关异常，请稍后再试',
  503: '服务暂不可用，请稍后再试',
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = path.startsWith('/') ? path : `/${path}`
  if (!query) return url
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    search.append(key, String(value))
  }
  const qs = search.toString()
  return qs ? `${url}?${qs}` : url
}

async function parseResult<T>(response: Response): Promise<Result<T> | null> {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text) as Result<T>
  } catch {
    return null
  }
}

function isResultPayload(value: unknown): value is Result<unknown> {
  // 业务错误时后端可能不返回 data 字段（例如 code=40010），只要 code 是数字就按统一结构处理
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { code?: unknown }).code === 'number'
  )
}

/**
 * 发起请求并解包 data。
 * 失败统一抛 ApiError，调用方通常只需 `try/catch` + 局部 loading。
 */
export async function http<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, silent = false, signal } = options

  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    const message = '网络连接失败，请确认后端服务已启动'
    if (!silent) errorNotifier?.(message)
    throw new ApiError(message, 0, 0)
  }

  const payload = await parseResult<T>(response)

  // 401 优先按契约处理：清空本地登录态后跳登录页
  if (response.status === 401 || (isResultPayload(payload) && payload.code === 401)) {
    clearToken()
    const message = '登录已过期，请重新登录'
    if (!silent) errorNotifier?.(message)
    unauthorizedHandler?.()
    throw new ApiError(message, 401, 401)
  }

  const okPayload = isResultPayload(payload)
  if (!response.ok || !okPayload || payload.code !== 200) {
    const message =
      (okPayload && payload.message) || STATUS_MESSAGE[response.status] || '请求失败，请稍后再试'
    if (!silent) errorNotifier?.(message)
    throw new ApiError(message, okPayload ? payload.code : response.status, response.status)
  }

  return payload.data as T
}
