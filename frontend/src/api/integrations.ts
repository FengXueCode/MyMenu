import { http } from '@/api/http'
import type {
  IntegrationItem,
  IntegrationSource,
  IntegrationStatus,
  PageResult,
  QrPollResult,
  QrStartResult,
  QueueResult,
  ScanStartResult,
} from '@/types/api'

export const integrationsApi = {
  status(): Promise<IntegrationStatus> {
    return http<IntegrationStatus>('/api/integrations/status')
  },

  startBilibiliQr(): Promise<QrStartResult> {
    return http<QrStartResult>('/api/integrations/bilibili/qr', { method: 'POST' })
  },

  pollBilibiliQr(key: string): Promise<QrPollResult> {
    return http<QrPollResult>(`/api/integrations/bilibili/qr/${encodeURIComponent(key)}`)
  },

  disconnectBilibili(): Promise<null> {
    return http<null>('/api/integrations/bilibili', { method: 'DELETE' })
  },

  addBilibiliFolder(url: string): Promise<IntegrationSource> {
    return http<IntegrationSource>('/api/integrations/bilibili/folder', {
      method: 'POST',
      body: { url },
    })
  },

  updateSource(
    id: number,
    body: { autoScan?: boolean; scanLimit?: number },
  ): Promise<IntegrationSource> {
    return http<IntegrationSource>(`/api/integrations/sources/${id}`, { method: 'PUT', body })
  },

  deleteSource(id: number): Promise<null> {
    return http<null>(`/api/integrations/sources/${id}`, { method: 'DELETE' })
  },

  scanSource(id: number): Promise<ScanStartResult> {
    return http<ScanStartResult>(`/api/integrations/sources/${id}/scan`, { method: 'POST' })
  },

  scanAll(): Promise<ScanStartResult> {
    return http<ScanStartResult>('/api/integrations/scan', { method: 'POST' })
  },

  enqueueDouyin(text: string): Promise<QueueResult> {
    return http<QueueResult>('/api/integrations/douyin/queue', {
      method: 'POST',
      body: { text },
    })
  },

  items(params: {
    status?: string
    pageNum?: number
    pageSize?: number
  }): Promise<PageResult<IntegrationItem>> {
    return http<PageResult<IntegrationItem>>('/api/integrations/items', { query: params })
  },
}
