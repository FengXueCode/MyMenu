import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

import RecipeCard from '@/components/RecipeCard.vue'
import type { RecipeCard as RecipeCardData } from '@/types/api'

const recipe: RecipeCardData = {
  id: 7,
  title: '番茄炖牛腩',
  description: '先煎后炖，汤汁拌饭一绝',
  coverUrl: '/api/files/beef.png',
  categoryId: 1,
  categoryName: '家常菜',
  meatType: 'MEAT',
  difficulty: 2,
  servings: 2,
  durationMin: 75,
  spiceLevel: 1,
  tags: ['下饭'],
  aiSummary: null,
  sourcePlatform: 'bilibili',
  sourceUrl: 'https://www.bilibili.com/video/BV1xx411c7mD',
  sourceAuthor: '灶台老王',
  owner: 'system',
  createdAt: '2026-11-06T09:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/recipes' },
      { path: '/recipes', name: 'recipes', component: { template: '<div />' } },
      {
        path: '/recipes/:id',
        name: 'recipe-detail',
        component: { template: '<div />' },
      },
    ],
  })
}

async function mountCard(data: Partial<RecipeCardData> = {}) {
  const router = createTestRouter()
  await router.push('/recipes')
  await router.isReady()
  return mount(RecipeCard, {
    props: { recipe: { ...recipe, ...data } },
    global: { plugins: [router] },
  })
}

describe('RecipeCard', () => {
  it('渲染菜名、时长、份量与难度', async () => {
    const wrapper = await mountCard()
    expect(wrapper.text()).toContain('番茄炖牛腩')
    expect(wrapper.text()).toContain('1 小时 15 分')
    expect(wrapper.text()).toContain('2 人份')
    expect(wrapper.text()).toContain('中等')
    expect(wrapper.text()).toContain('家常菜')
  })

  it('封面使用 aspect-ratio + 懒加载', async () => {
    const wrapper = await mountCard()
    const image = wrapper.find('img')
    expect(image.exists()).toBe(true)
    expect(image.attributes('loading')).toBe('lazy')
    expect(image.attributes('decoding')).toBe('async')
    expect(image.attributes('src')).toBe('/api/files/beef.png')
  })

  it('链接指向菜谱详情页', async () => {
    const wrapper = await mountCard()
    expect(wrapper.element.tagName).toBe('A')
    expect(wrapper.attributes('href')).toBe('/recipes/7')
  })

  it('自定义 to 覆盖默认跳转', async () => {
    const wrapper = await mountCard()
    await wrapper.setProps({ to: '/recipes?mine=true' })
    expect(wrapper.attributes('href')).toBe('/recipes?mine=true')
  })

  it('无封面时展示占位，不渲染 img', async () => {
    const wrapper = await mountCard({ coverUrl: null })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('暂无配图')
  })

  it('荤素角标与 B 站来源标记正确', async () => {
    const wrapper = await mountCard()
    expect(wrapper.text()).toContain('荤')
    expect(wrapper.text()).toContain('B 站')
  })

  it('自己的菜谱显示「我的手记」印章', async () => {
    const wrapper = await mountCard({ owner: 'mine' })
    expect(wrapper.text()).toContain('我的手记')
  })
})
