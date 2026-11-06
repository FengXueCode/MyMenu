import { http } from '@/api/http'
import type { Category, CategoryInput } from '@/types/api'

export const categoriesApi = {
  list() {
    return http<Category[]>('/api/categories')
  },
  create(input: CategoryInput) {
    return http<Category>('/api/categories', { method: 'POST', body: input })
  },
  update(id: number, input: Partial<CategoryInput>) {
    return http<Category>(`/api/categories/${id}`, { method: 'PUT', body: input })
  },
  remove(id: number) {
    return http<null>(`/api/categories/${id}`, { method: 'DELETE' })
  },
}
