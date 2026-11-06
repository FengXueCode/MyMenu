import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AppShell from '@/components/AppShell.vue'
import { getToken } from '@/utils/token'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题，用于 document.title */
    title: string
    /** 公开页面：无需登录即可访问 */
    public?: boolean
    /** 页面外壳不显示顶部导航 / 底部 Tab（登录页） */
    chromeless?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true, chromeless: true },
  },
  {
    path: '/',
    component: AppShell,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'recipes',
        name: 'recipes',
        component: () => import('@/views/RecipesView.vue'),
        meta: { title: '菜谱库' },
      },
      {
        path: 'recipes/:id',
        name: 'recipe-detail',
        component: () => import('@/views/RecipeDetailView.vue'),
        meta: { title: '菜谱详情' },
      },
      {
        path: 'import',
        name: 'import',
        component: () => import('@/views/ImportView.vue'),
        meta: { title: '导入菜谱' },
      },
      {
        path: 'today',
        name: 'today',
        component: () => import('@/views/TodayView.vue'),
        meta: { title: '今日菜单' },
      },
      {
        path: 'me',
        name: 'me',
        component: () => import('@/views/MeView.vue'),
        meta: { title: '我的' },
      },
      {
        path: 'settings/preferences',
        name: 'preferences',
        component: () => import('@/views/PreferencesView.vue'),
        meta: { title: '用餐偏好' },
      },
      {
        path: 'settings/ai',
        name: 'ai-settings',
        component: () => import('@/views/AiSettingsView.vue'),
        meta: { title: 'AI 设置' },
      },
      {
        path: 'settings/integrations',
        name: 'integrations',
        component: () => import('@/views/VideoAccountsView.vue'),
        meta: { title: '视频收藏' },
      },
    ],
  },
  {
    // 未匹配路由统一回到首页
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
})

/**
 * 路由守卫：
 * - 未登录访问业务页 → /login?redirect=目标地址；
 * - 已登录访问 /login → 首页。
 * （token 是否仍然有效由 /api/auth/me 与 http 层的 401 兜底）
 */
router.beforeEach((to) => {
  const authed = Boolean(getToken())
  const isPublic = to.meta.public === true

  if (!authed && !isPublic) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (authed && to.name === 'login') {
    return { name: 'home' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 灶台手账` : '灶台手账 · Menu'
})

export default router
