import { http } from '@/api/http'
import type {
  AiExtractInput,
  AiImageResult,
  AiProvider,
  AiProviderInput,
  AiSummaryResult,
  ProviderTestResult,
  RecipeDraft,
} from '@/types/api'

/**
 * AI 相关接口。
 * 注意安全约束：前端只提交 apiKey（只写），任何响应都只有 hasKey / keyMasked，页面不得展示明文 Key。
 */
export const aiApi = {
  /** 把视频文案抽取为结构化菜谱草稿；未配置模型时后端返回 code=400 */
  extract(input: AiExtractInput) {
    return http<RecipeDraft>('/api/ai/extract', { method: 'POST', body: input })
  },

  providers() {
    return http<AiProvider[]>('/api/ai/providers')
  },
  createProvider(input: AiProviderInput) {
    return http<AiProvider>('/api/ai/providers', { method: 'POST', body: input })
  },
  updateProvider(id: number, input: AiProviderInput) {
    return http<AiProvider>(`/api/ai/providers/${id}`, { method: 'PUT', body: input })
  },
  deleteProvider(id: number) {
    return http<null>(`/api/ai/providers/${id}`, { method: 'DELETE' })
  },
  testProvider(id: number) {
    return http<ProviderTestResult>(`/api/ai/providers/${id}/test`, { method: 'POST' })
  },

  /** 生成/刷新菜谱 AI 摘要 */
  recipeSummary(recipeId: number) {
    return http<AiSummaryResult>(`/api/ai/recipes/${recipeId}/summary`, { method: 'POST' })
  },
  /** 生成菜谱配图（后端返回兜底封面时 aiGenerated=false） */
  recipeImage(recipeId: number) {
    return http<AiImageResult>(`/api/ai/recipes/${recipeId}/image`, { method: 'POST' })
  },
}
