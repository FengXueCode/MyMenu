<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { recipesApi } from '@/api/recipes'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const router = useRouter()
const toast = useToastStore()

const myRecipeTotal = ref<number | null>(null)
const logoutOpen = ref(false)

const ENTRIES = [
  {
    to: '/recipes?mine=true',
    icon: 'book' as const,
    title: '我的菜谱',
    hint: '你亲手记下的每一道',
    accent: 'bg-herb-soft text-herb',
  },
  {
    to: '/settings/preferences',
    icon: 'sliders' as const,
    title: '用餐偏好',
    hint: '人数 / 餐数 / 荤素 / 忌口',
    accent: 'bg-ginger-soft text-[#8a6215]',
  },
  {
    to: '/today',
    icon: 'bowl' as const,
    title: '今日菜单',
    hint: '按口味随机的一桌菜',
    accent: 'bg-tomato-soft text-tomato-deep',
  },
  {
    to: '/settings/ai',
    icon: 'sparkle' as const,
    title: 'AI 配置',
    hint: '多套模型，Key 只写不读',
    accent: 'bg-[#ece6f6] text-[#6b58a0]',
  },
  {
    to: '/import',
    icon: 'link' as const,
    title: '导入新菜谱',
    hint: '粘贴 B 站 / 抖音分享链接',
    accent: 'bg-paper-deep text-ink-soft',
  },
]

async function loadStats(): Promise<void> {
  try {
    const page = await recipesApi.list({ mine: true, pageNum: 1, pageSize: 1 })
    myRecipeTotal.value = page.total ?? 0
  } catch {
    myRecipeTotal.value = null
  }
}

function openLogout(): void {
  logoutOpen.value = true
}

async function confirmLogout(): Promise<void> {
  logoutOpen.value = false
  auth.logout()
  toast.success('已退出登录，随时回来开火')
  await router.replace({ name: 'login' })
}

onMounted(() => {
  void loadStats()
  // 用户信息可能还没拉回来（刷新页面时），这里再兜一次
  if (!auth.user) void auth.fetchMe()
})
</script>

<template>
  <div class="mx-auto w-full max-w-2xl space-y-4">
    <!-- 用户卡 -->
    <section
      class="animate-rise relative overflow-hidden rounded-card border border-line bg-card p-5 shadow-paper"
    >
      <div class="paper-grain pointer-events-none absolute inset-0 opacity-60" aria-hidden="true" />
      <div
        class="pointer-events-none absolute -right-10 -top-12 h-40 w-40 rounded-full bg-ginger/12 blur-2xl"
        aria-hidden="true"
      />

      <div class="relative flex items-start gap-4">
        <span
          class="grid h-16 w-16 shrink-0 -rotate-3 place-items-center rounded-[18px] border border-herb/25 bg-herb-soft font-display text-[26px] text-herb shadow-paper"
        >
          {{ auth.initial }}
        </span>

        <div class="min-w-0 flex-1">
          <h1 class="truncate font-display text-[24px] leading-tight text-ink">
            {{ auth.displayName }}
          </h1>
          <p class="mt-0.5 font-mono text-[11px] tracking-wide text-ink-muted">
            @{{ auth.user?.username ?? '—' }} · NO.{{ auth.user?.id ?? '—' }}
          </p>

          <div class="mt-2 flex flex-wrap gap-1.5">
            <RouterLink
              to="/recipes?mine=true"
              class="inline-flex h-11 items-center gap-1.5 rounded-full bg-paper-deep/80 px-3 text-[11.5px] text-ink-soft transition hover:bg-paper-deep"
            >
              <AppIcon name="book" :size="13" />
              我的菜谱
              <span class="font-mono text-ink">{{ myRecipeTotal ?? '—' }}</span>
            </RouterLink>
            <RouterLink
              to="/today"
              class="inline-flex h-11 items-center gap-1.5 rounded-full bg-paper-deep/80 px-3 text-[11.5px] text-ink-soft transition hover:bg-paper-deep"
            >
              <AppIcon name="bowl" :size="13" />
              今日菜单
            </RouterLink>
          </div>
        </div>

        <button
          type="button"
          class="tap-target grid shrink-0 place-items-center rounded-full border border-line bg-paper/60 text-ink-muted transition hover:border-tomato/40 hover:text-tomato-deep"
          aria-label="退出登录"
          @click="openLogout"
        >
          <AppIcon name="logout" :size="18" />
        </button>
      </div>
    </section>

    <!-- 手账入口 -->
    <section class="animate-rise" style="animation-delay: 60ms">
      <div class="mb-2 flex items-center gap-2">
        <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
        <h2 class="font-display text-[18px] tracking-wide text-ink">我的手账</h2>
      </div>

      <ul class="overflow-hidden rounded-card border border-line bg-card shadow-paper">
        <li v-for="(entry, index) in ENTRIES" :key="entry.to" :class="index > 0 ? 'border-t border-line/70' : ''">
          <RouterLink
            :to="entry.to"
            class="flex min-h-[3.75rem] items-center gap-3 px-4 py-3 transition hover:bg-paper/60"
          >
            <span :class="['grid h-9 w-9 shrink-0 place-items-center rounded-full', entry.accent]">
              <AppIcon :name="entry.icon" :size="17" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block text-[14px] font-medium text-ink">{{ entry.title }}</span>
              <span class="mt-0.5 block truncate text-[11.5px] text-ink-muted">{{ entry.hint }}</span>
            </span>
            <span
              v-if="entry.to === '/recipes?mine=true' && myRecipeTotal !== null"
              class="shrink-0 font-mono text-[11px] text-ink-muted"
            >
              {{ myRecipeTotal }} 道
            </span>
            <AppIcon name="chevron-right" :size="16" class="shrink-0 text-line-strong" />
          </RouterLink>
        </li>
      </ul>
    </section>

    <!-- 安全说明 -->
    <section class="animate-rise" style="animation-delay: 110ms">
      <div class="rounded-card border border-dashed border-line-strong/70 bg-card/60 p-4">
        <div class="flex items-start gap-2.5">
          <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-herb-soft text-herb">
            <AppIcon name="info" :size="15" />
          </span>
          <div class="min-w-0">
            <h3 class="font-display text-[14.5px] text-ink">关于 AI Key</h3>
            <p class="mt-1 text-[11.5px] leading-relaxed text-ink-muted">
              模型 Key 使用 AES-256-GCM 加密后保存在你本机的后端，任何接口都只返回
              <span class="font-mono text-ink-soft">hasKey</span> 与掩码；这个页面永远不会显示明文
              Key，也不会把 Key 写入浏览器存储。
            </p>
          </div>
        </div>
      </div>
    </section>

    <section class="animate-rise" style="animation-delay: 150ms">
      <div class="flex flex-col items-center gap-1 py-2 text-center">
        <p class="font-display text-[13px] tracking-[0.2em] text-ink-soft">一餐一饭，皆是手记。</p>
        <p class="font-mono text-[9.5px] tracking-[0.24em] text-ink-muted">
          MENU · STOVE NOTES · v0.1.0
        </p>
      </div>
    </section>

    <ConfirmDialog
      v-model="logoutOpen"
      title="要离开灶台了吗？"
      message="退出后需要重新登录才能查看菜谱与今日菜单，本地的登录状态会被清除。"
      confirm-text="退出登录"
      tone="danger"
      @confirm="confirmLogout"
    />
  </div>
</template>
