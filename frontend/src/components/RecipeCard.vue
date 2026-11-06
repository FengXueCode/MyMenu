<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'

import AppIcon from '@/components/AppIcon.vue'
import type { RecipeCard } from '@/types/api'
import {
  difficultyLabel,
  formatDuration,
  meatTypeEmoji,
  meatTypeLabel,
  platformLabel,
  spiceEmoji,
  truncate,
} from '@/utils/format'

const props = withDefaults(
  defineProps<{
    recipe: RecipeCard
    /** 自定义跳转目标，默认菜谱详情 */
    to?: RouteLocationRaw
    /** 紧凑模式：首页横向列表用 */
    compact?: boolean
  }>(),
  { to: undefined, compact: false },
)

const coverError = ref(false)
const target = computed<RouteLocationRaw>(
  () => props.to ?? { name: 'recipe-detail', params: { id: props.recipe.id } },
)

const snippet = computed(() => truncate(props.recipe.aiSummary || props.recipe.description, 52))
const hasCover = computed(() => Boolean(props.recipe.coverUrl) && !coverError.value)
</script>

<template>
  <RouterLink
    :to="target"
    class="group relative flex flex-col overflow-hidden rounded-card border border-line bg-card shadow-paper transition duration-300 hover:-translate-y-1 hover:border-line-strong hover:shadow-lift active:translate-y-0"
  >
    <!-- 封面：aspect-ratio 占位 + 懒加载，避免图片抖动 -->
    <div class="relative aspect-[4/3] w-full overflow-hidden bg-paper-deep">
      <img
        v-if="hasCover"
        :src="recipe.coverUrl ?? ''"
        :alt="recipe.title"
        loading="lazy"
        decoding="async"
        class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.035]"
        @error="coverError = true"
      />
      <div
        v-else
        class="paper-grain flex h-full w-full flex-col items-center justify-center gap-1 text-ink-muted"
      >
        <span class="text-2xl leading-none">{{ meatTypeEmoji(recipe.meatType) }}</span>
        <span class="font-display text-[11px] tracking-[0.2em] text-ink-muted/80">暂无配图</span>
      </div>

      <!-- 荤素角标 -->
      <span
        class="absolute left-2.5 top-2.5 inline-flex items-center gap-1 rounded-full border border-white/40 bg-card/92 px-2 py-0.5 text-[11px] font-medium shadow-sm backdrop-blur"
      >
        <span
          class="h-1.5 w-1.5 rounded-full"
          :class="
            recipe.meatType === 'MEAT'
              ? 'bg-tomato'
              : recipe.meatType === 'VEG'
                ? 'bg-herb'
                : 'bg-ginger'
          "
        />
        {{ meatTypeLabel(recipe.meatType) }}
      </span>

      <!-- 来源平台 -->
      <span
        v-if="recipe.sourcePlatform && recipe.sourcePlatform !== 'unknown'"
        class="absolute right-2.5 top-2.5 rounded-full bg-ink/72 px-2 py-0.5 text-[10.5px] text-paper backdrop-blur"
      >
        {{ platformLabel(recipe.sourcePlatform) }}
      </span>

      <!-- 时长 -->
      <span
        class="absolute bottom-2.5 right-2.5 inline-flex items-center gap-1 rounded-full bg-ink/72 px-2 py-0.5 text-[10.5px] text-paper backdrop-blur"
      >
        <AppIcon name="clock" :size="11" :stroke-width="2" />
        {{ formatDuration(recipe.durationMin) }}
      </span>

      <!-- 自己录入的菜谱盖一枚小红章 -->
      <span
        v-if="recipe.owner === 'mine'"
        class="absolute bottom-2.5 left-2.5 -rotate-6 rounded-[4px] border border-tomato/50 bg-tomato/92 px-1.5 py-0.5 font-display text-[10px] tracking-[0.18em] text-paper"
      >
        我的手记
      </span>
    </div>

    <div class="flex flex-1 flex-col gap-1.5 p-3.5" :class="compact ? 'gap-1' : ''">
      <h3 class="line-clamp-2 font-display text-[16px] leading-snug text-ink">
        {{ recipe.title }}
      </h3>
      <p v-if="snippet" class="line-clamp-2 text-[11.5px] leading-relaxed text-ink-muted">
        {{ snippet }}
      </p>

      <div class="mt-auto flex flex-wrap items-center gap-x-2.5 gap-y-1 pt-1 text-[11px] text-ink-muted">
        <span v-if="recipe.categoryName" class="text-ink-soft">{{ recipe.categoryName }}</span>
        <span v-if="recipe.spiceLevel > 0" class="tracking-[-0.15em]">
          {{ spiceEmoji(recipe.spiceLevel) }}
        </span>
        <span>难度 · {{ difficultyLabel(recipe.difficulty) }}</span>
        <span class="ml-auto inline-flex items-center gap-0.5 font-mono text-[10.5px] tracking-wide">
          {{ recipe.servings }} 人份
        </span>
      </div>
    </div>
  </RouterLink>
</template>
