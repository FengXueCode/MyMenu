<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import AppIcon from '@/components/AppIcon.vue'
import type { Dish, Meal } from '@/types/api'
import { formatDuration, meatTypeEmoji, spiceEmoji } from '@/utils/format'
import { ingredientLine, scaleDishIngredients } from '@/utils/scale'

const props = withDefaults(
  defineProps<{
    meal: Meal
    peopleCount: number
    /** 正在换菜的菜品下标（用于局部 loading） */
    loadingIndex?: number | null
    /** 是否显示「换一道」 */
    editable?: boolean
  }>(),
  { loadingIndex: null, editable: true },
)

defineEmits<{ reroll: [index: number] }>()

const MEAL_EMOJI: Record<string, string> = {
  breakfast: '🌅',
  lunch: '☀️',
  dinner: '🌙',
}

/** 菜品卡展示缩放后的配料（只列前 4 项，避免手机端太长） */
function scaledIngredientLines(dish: Dish): string[] {
  return scaleDishIngredients(dish, props.peopleCount)
    .slice(0, 4)
    .map((item) => ingredientLine(item))
}

function moreCount(dish: Dish): number {
  return Math.max(0, dish.ingredients.length - 4)
}

const emoji = computed(() => MEAL_EMOJI[props.meal.key] ?? '🍽️')
</script>

<template>
  <section class="overflow-hidden rounded-card border border-line bg-card shadow-paper">
    <header
      class="flex flex-wrap items-center gap-x-2 gap-y-1 border-b border-line/70 bg-paper/70 px-3.5 py-2.5"
    >
      <span class="text-[15px] leading-none">{{ emoji }}</span>
      <h3 class="font-display text-[17px] leading-none text-ink">{{ meal.label }}</h3>
      <span class="text-[11px] text-ink-muted">
        {{ meal.dishes.length }} 道 · {{ peopleCount }} 人份
      </span>
      <span
        v-if="meal.key === 'dinner'"
        class="ml-auto rounded-full border border-ginger/50 bg-ginger-soft/60 px-2 py-0.5 text-[10.5px] text-[#7d5a12]"
      >
        一天收个尾
      </span>
    </header>

    <ul v-if="meal.dishes.length" class="divide-y divide-line/60">
      <li v-for="(dish, index) in meal.dishes" :key="dish.recipeId" class="flex gap-3 p-3.5">
        <!-- 缩略图 -->
        <RouterLink
          :to="{ name: 'recipe-detail', params: { id: dish.recipeId } }"
          class="relative h-16 w-16 shrink-0 overflow-hidden rounded-xl border border-line/70 bg-paper-deep"
          :aria-label="`查看菜谱：${dish.title}`"
        >
          <img
            v-if="dish.coverUrl"
            :src="dish.coverUrl"
            :alt="dish.title"
            loading="lazy"
            decoding="async"
            class="h-full w-full object-cover"
          />
          <span v-else class="grid h-full w-full place-items-center text-lg">
            {{ meatTypeEmoji(dish.meatType) }}
          </span>
        </RouterLink>

        <div class="min-w-0 flex-1">
          <div class="flex items-start gap-2">
            <RouterLink
              :to="{ name: 'recipe-detail', params: { id: dish.recipeId } }"
              class="flex min-h-11 min-w-0 flex-1 items-center font-display text-[15px] leading-snug text-ink underline-offset-4 hover:underline"
            >
              <span class="line-clamp-1">{{ dish.title }}</span>
            </RouterLink>

            <button
              v-if="editable"
              type="button"
              class="inline-flex h-11 shrink-0 items-center gap-1 rounded-full border border-line bg-paper px-3 text-[12px] text-ink-soft transition hover:border-line-strong hover:text-ink disabled:opacity-60"
              :disabled="loadingIndex === index"
              :aria-label="`换一道：${dish.title}`"
              @click="$emit('reroll', index)"
            >
              <AppIcon name="swap" :size="13" :class="loadingIndex === index ? 'animate-spin' : ''" />
              {{ loadingIndex === index ? '换…' : '换一道' }}
            </button>
          </div>

          <div class="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-ink-muted">
            <span>{{ meatTypeEmoji(dish.meatType) }}</span>
            <span>{{ formatDuration(dish.cookMinutes) }}</span>
            <span v-if="dish.spiceLevel > 0">{{ spiceEmoji(dish.spiceLevel) }}</span>
          </div>

          <p class="mt-1 line-clamp-2 text-[11.5px] leading-relaxed text-ink-soft">
            <span v-for="(line, i) in scaledIngredientLines(dish)" :key="i">
              <span v-if="i > 0" class="px-1 text-line-strong">·</span>{{ line }}
            </span>
            <span v-if="moreCount(dish) > 0" class="text-ink-muted"> 等 {{ moreCount(dish) }} 样</span>
          </p>
        </div>
      </li>
    </ul>

    <div v-else class="px-4 py-6 text-center">
      <p class="text-[12.5px] text-ink-muted">这一餐还没有安排，换一道或重新生成试试</p>
    </div>
  </section>
</template>
