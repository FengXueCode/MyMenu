import { http } from '@/api/http'
import type { ImportResolveInput, ImportResolveResult } from '@/types/api'

export const importApi = {
  /** 解析分享链接 / 分享文案，返回视频元数据与 warnings */
  resolve(input: ImportResolveInput) {
    return http<ImportResolveResult>('/api/import/resolve', { method: 'POST', body: input })
  },
}
