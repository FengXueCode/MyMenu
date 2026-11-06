import { http } from '@/api/http'
import type { MenuPlan, RerollInput, SaveMenuInput } from '@/types/api'

export const menuApi = {
  /** 读取/生成今日菜单；regenerate=true 会改变 seq 与组合 */
  today(date?: string, regenerate = false) {
    return http<MenuPlan>('/api/menu/today', {
      query: { date, regenerate: regenerate ? true : undefined },
    })
  },
  /** 单道换菜，返回整份新菜单（seq + 1） */
  reroll(input: RerollInput) {
    return http<MenuPlan>('/api/menu/today/reroll', { method: 'POST', body: input })
  },
  /** 保存菜单用于历史去重 */
  save(input: SaveMenuInput) {
    return http<null>('/api/menu/today/save', { method: 'POST', body: input })
  },
}
