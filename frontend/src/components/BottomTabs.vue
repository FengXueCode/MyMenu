<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()

interface TabItem {
  name: string
  label: string
  icon: 'home' | 'book' | 'bowl' | 'user'
  match: string[]
}

const TABS: TabItem[] = [
  { name: 'home', label: '首页', icon: 'home', match: ['home'] },
  { name: 'recipes', label: '菜谱', icon: 'book', match: ['recipes', 'recipe-detail'] },
  { name: 'today', label: '今日', icon: 'bowl', match: ['today'] },
  { name: 'me', label: '我的', icon: 'user', match: ['me', 'preferences', 'ai-settings'] },
]

const currentName = computed(() => String(route.name ?? ''))
const isActive = (tab: TabItem): boolean => tab.match.includes(currentName.value)
</script>

<template>
  <!-- 移动端底部固定 Tab：安全区留白 + 每个热区 ≥44px -->
  <nav
    class="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-paper/95 pb-safe backdrop-blur-md md:hidden"
    aria-label="底部导航"
  >
    <div class="grid grid-cols-4">
      <RouterLink
        v-for="tab in TABS"
        :key="tab.name"
        :to="{ name: tab.name }"
        class="relative flex min-h-[56px] flex-col items-center justify-center gap-0.5 px-1 pb-1.5 pt-2 text-[11px] transition-colors"
        :class="isActive(tab) ? 'text-tomato' : 'text-ink-muted active:text-ink-soft'"
        :aria-current="isActive(tab) ? 'page' : undefined"
      >
        <span
          v-if="isActive(tab)"
          class="absolute top-0 h-[3px] w-7 rounded-full bg-tomato"
          aria-hidden="true"
        />
        <span
          class="grid h-7 w-7 place-items-center rounded-full transition-colors"
          :class="isActive(tab) ? 'bg-tomato-soft' : ''"
        >
          <AppIcon :name="tab.icon" :size="21" :stroke-width="isActive(tab) ? 2 : 1.6" />
        </span>
        <span :class="isActive(tab) ? 'font-medium' : ''">{{ tab.label }}</span>
      </RouterLink>
    </div>
  </nav>
</template>
