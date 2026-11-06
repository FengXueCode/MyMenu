<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import AppIcon from '@/components/AppIcon.vue'
import { useCookTimerStore } from '@/stores/cookTimer'

const route = useRoute()
const timer = useCookTimerStore()

/** 正在查看计时所属的菜谱详情时，页内已有完整计时卡，不再重复显示 */
const visible = computed(() => {
  if (!timer.isActive || !timer.recipeId) return false
  const sameRecipe = route.name === 'recipe-detail' && Number(route.params.id) === timer.recipeId
  return !sameRecipe
})
</script>

<template>
  <div v-if="visible" class="sticky top-0 z-30 px-4 pt-2 md:top-16 md:px-8">
    <RouterLink
      :to="{ name: 'recipe-detail', params: { id: timer.recipeId } }"
      class="mx-auto flex h-11 max-w-[1180px] items-center gap-2 rounded-full border border-tomato/30 bg-tomato-soft/95 px-3.5 text-[12.5px] text-tomato-deep shadow-paper backdrop-blur transition hover:border-tomato/50"
      :aria-label="`${timer.statusText} ${timer.clock}，${timer.recipeTitle}，回到菜谱详情`"
    >
      <span
        class="h-2 w-2 shrink-0 rounded-full"
        :class="timer.running ? 'animate-pulse bg-tomato' : 'bg-ink-muted'"
        aria-hidden="true"
      />
      <AppIcon name="clock" :size="15" />
      <span class="shrink-0 font-medium">{{ timer.statusText }}</span>
      <span class="shrink-0 font-mono tabular-nums">{{ timer.clock }}</span>
      <span class="min-w-0 flex-1 truncate text-tomato-deep/70">· {{ timer.recipeTitle }}</span>
      <span class="hidden shrink-0 text-[11px] text-tomato-deep/70 sm:inline">回去看看</span>
      <AppIcon name="chevron-right" :size="14" class="shrink-0" />
    </RouterLink>
  </div>
</template>
