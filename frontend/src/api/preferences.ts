import { http } from '@/api/http'
import type { Preferences } from '@/types/api'

export const preferencesApi = {
  get() {
    return http<Preferences>('/api/preferences')
  },
  update(input: Preferences) {
    return http<Preferences>('/api/preferences', { method: 'PUT', body: input })
  },
}
