<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

interface NavItem {
  name: string
  label: string
  /** 用于判定当前分区的路径前缀 */
  match: string[]
}

const NAV_ITEMS: NavItem[] = [
  { name: 'home', label: '首页', match: ['home'] },
  { name: 'recipes', label: '菜谱库', match: ['recipes', 'recipe-detail'] },
  { name: 'today', label: '今日菜单', match: ['today'] },
  { name: 'import', label: '导入', match: ['import'] },
  { name: 'me', label: '我的', match: ['me', 'preferences', 'ai-settings'] },
]

const currentName = computed(() => String(route.name ?? ''))
const isActive = (item: NavItem): boolean => item.match.includes(currentName.value)
</script>

<template>
  <header class="sticky top-0 z-40 border-b border-line/80 bg-paper/85 backdrop-blur-md">
    <div class="mx-auto flex h-16 w-full max-w-[1180px] items-center gap-2 px-4 md:px-8">
      <!-- 品牌：番茄红印章 + 衬线字 -->
      <RouterLink
        to="/"
        class="group flex min-h-11 items-center gap-3 rounded-full pr-2"
        aria-label="灶台手账首页"
      >
        <span
          class="grid h-9 w-9 -rotate-6 place-items-center rounded-[10px] bg-tomato font-display text-lg leading-none text-paper shadow-[0_2px_0_rgba(176,59,41,0.9)] transition-transform duration-300 group-hover:rotate-0"
        >
          灶
        </span>
        <span class="flex flex-col leading-tight">
          <span class="font-display text-[19px] tracking-[0.14em] text-ink">灶台手账</span>
          <span class="font-mono text-[9px] tracking-[0.34em] text-ink-muted">STOVE NOTES</span>
        </span>
      </RouterLink>

      <nav class="ml-4 hidden items-center gap-1 md:flex" aria-label="主导航">
        <RouterLink
          v-for="item in NAV_ITEMS"
          :key="item.name"
          :to="{ name: item.name }"
          class="relative flex h-11 items-center rounded-full px-3.5 text-[13.5px] transition-colors"
          :class="
            isActive(item)
              ? 'bg-tomato-soft font-medium text-tomato-deep'
              : 'text-ink-soft hover:bg-paper-deep hover:text-ink'
          "
        >
          {{ item.label }}
          <span
            v-if="isActive(item)"
            class="absolute -bottom-[3px] left-1/2 h-[3px] w-4 -translate-x-1/2 rounded-full bg-tomato"
          />
        </RouterLink>
      </nav>

      <div class="ml-auto flex items-center gap-2 md:gap-3">
        <RouterLink
          to="/import"
          class="hidden h-11 items-center gap-1.5 rounded-full border border-tomato/30 bg-card px-4 text-[13px] font-medium text-tomato-deep shadow-paper transition hover:-translate-y-px hover:bg-tomato-soft md:flex"
        >
          <AppIcon name="pen" :size="16" />
          记一笔
        </RouterLink>

        <RouterLink
          to="/me"
          class="flex min-h-11 items-center gap-2 rounded-full border border-line bg-card py-1 pl-1 pr-1 transition hover:border-line-strong md:pr-3"
          :aria-label="`我的：${auth.displayName}`"
        >
          <span
            class="grid h-8 w-8 place-items-center rounded-full bg-herb/12 font-display text-sm text-herb"
          >
            {{ auth.initial }}
          </span>
          <span class="hidden max-w-[7rem] truncate text-[13px] text-ink-soft md:inline">
            {{ auth.displayName }}
          </span>
        </RouterLink>
      </div>
    </div>
  </header>
</template>
