import { http } from '@/api/http'
import type { PageResult, RecipeCard, RecipeDetail, RecipeQuery, RecipeUpsertInput } from '@/types/api'

export const recipesApi = {
  list(query: RecipeQuery = {}) {
    return http<PageResult<RecipeCard>>('/api/recipes', {
      query: {
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        keyword: query.keyword,
        categoryId: query.categoryId ?? undefined,
        meatType: query.meatType ?? undefined,
        mine: query.mine,
      },
    })
  },
  detail(id: number) {
    return http<RecipeDetail>(`/api/recipes/${id}`)
  },
  create(input: RecipeUpsertInput) {
    return http<RecipeDetail>('/api/recipes', { method: 'POST', body: input })
  },
  update(id: number, input: RecipeUpsertInput) {
    return http<RecipeDetail>(`/api/recipes/${id}`, { method: 'PUT', body: input })
  },
  remove(id: number) {
    return http<null>(`/api/recipes/${id}`, { method: 'DELETE' })
  },
}
