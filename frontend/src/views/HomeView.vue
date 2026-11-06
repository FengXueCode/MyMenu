<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { categoriesApi } from '@/api/categories'
import { menuApi } from '@/api/menu'
import { recipesApi } from '@/api/recipes'
import AppIcon from '@/components/AppIcon.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import EmptyState from '@/components/EmptyState.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import { useAuthStore } from '@/stores/auth'
import type { Category, Meal, MenuPlan, RecipeCard as RecipeCardData } from '@/types/api'
import {
  formatDateCN,
  greeting,
  mealLabel,
  meatTypeEmoji,
  todayISO,
} from '@/utils/format'

const auth = useAuthStore()

const menu = ref<MenuPlan | null>(null)
const categories = ref<Category[]>([])
const recent = ref<RecipeCardData[]>([])
const loadingMenu = ref(true)
const loadingCategories = ref(true)
const loadingRecipes = ref(true)
const menuFailed = ref(false)

const today = todayISO()
const dateLabel = formatDateCN(today)
const hello = greeting()

const QUICK_LINKS = [
  {
    to: { name: 'import' },
    icon: 'link' as const,
    index: '01',
    title: '导入新菜谱',
    hint: '粘贴分享链接',
    accent: 'bg-tomato-soft text-tomato-deep',
  },
  {
    to: { name: 'recipes' },
    icon: 'book' as const,
    index: '02',
    title: '翻翻菜谱库',
    hint: '按分类找灵感',
    accent: 'bg-herb-soft text-herb',
  },
  {
    to: { name: 'preferences' },
    icon: 'sliders' as const,
    index: '03',
    title: '调一调口味',
    hint: '人数 / 辣度',
    accent: 'bg-ginger-soft text-[#8a6215]',
  },
]

const dishCount = computed(() =>
  (menu.value?.meals ?? []).reduce((total, meal) => total + meal.dishes.length, 0),
)

const mealPreview = computed(() =>
  (menu.value?.meals ?? []).map((meal) => ({
    ...meal,
    label: meal.label || mealLabel(meal.key),
  })),
)

/** 移动端：把一餐的菜名压成一行 */
function dishTitles(meal: Meal): string {
  return meal.dishes.map((dish) => dish.title).join('、')
}

async function loadMenu(): Promise<void> {
  loadingMenu.value = true
  menuFailed.value = false
  try {
    menu.value = await menuApi.today(today)
  } catch {
    menuFailed.value = true
  } finally {
    loadingMenu.value = false
  }
}

async function loadCategories(): Promise<void> {
  loadingCategories.value = true
  try {
    categories.value = await categoriesApi.list()
  } catch {
    categories.value = []
  } finally {
    loadingCategories.value = false
  }
}

async function loadRecent(): Promise<void> {
  loadingRecipes.value = true
  try {
    const page = await recipesApi.list({ pageNum: 1, pageSize: 6 })
    recent.value = page.records ?? []
  } catch {
    recent.value = []
  } finally {
    loadingRecipes.value = false
  }
}

onMounted(() => {
  void loadMenu()
  void loadCategories()
  void loadRecent()
})
</script>

<template>
  <div class="space-y-6 md:space-y-8">
    <!-- 问候 -->
    <header class="animate-rise">
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0">
          <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">
            {{ dateLabel.toUpperCase() }} · STOVE NOTES
          </p>
          <h1 class="mt-1.5 font-display text-[26px] leading-tight text-ink md:text-[32px]">
            {{ hello }}，{{ auth.displayName }}
          </h1>
          <p class="mt-1 text-[12.5px] leading-relaxed text-ink-muted md:text-[13px]">
            今天灶台上要不要来点新的？先把饭吃了，别的都好说。
          </p>
        </div>

        <!-- 手账日期贴纸 -->
        <div
          class="hidden shrink-0 -rotate-2 flex-col items-center rounded-[10px] border border-line-strong/70 bg-card px-3 py-2 shadow-paper sm:flex"
        >
          <span class="font-mono text-[9px] tracking-[0.24em] text-ink-muted">TODAY</span>
          <span class="font-display text-[22px] leading-none text-tomato">
            {{ new Date().getDate() }}
          </span>
          <span class="mt-0.5 text-[9.5px] text-ink-muted">
            {{ new Date().getMonth() + 1 }} 月
          </span>
        </div>
      </div>
    </header>

    <!-- 快捷入口 -->
    <section class="grid grid-cols-3 gap-2.5 animate-rise md:gap-4" style="animation-delay: 60ms">
      <RouterLink
        v-for="link in QUICK_LINKS"
        :key="link.index"
        :to="link.to"
        class="group relative flex min-h-[6.5rem] flex-col justify-between overflow-hidden rounded-card border border-line bg-card p-3 shadow-paper transition duration-300 hover:-translate-y-0.5 hover:border-line-strong hover:shadow-lift md:p-4"
      >
        <div class="flex items-start justify-between">
          <span :class="['grid h-8 w-8 place-items-center rounded-full', link.accent]">
            <AppIcon :name="link.icon" :size="16" />
          </span>
          <span class="font-mono text-[9.5px] tracking-widest text-line-strong">{{ link.index }}</span>
        </div>
        <div class="mt-2">
          <p class="truncate font-display text-[14px] leading-tight text-ink md:text-[15px]">
            {{ link.title }}
          </p>
          <p class="mt-0.5 truncate text-[10.5px] leading-snug text-ink-muted md:text-[11.5px]">
            {{ link.hint }}
          </p>
        </div>
      </RouterLink>
    </section>

    <!-- 今日菜单预览 -->
    <section class="animate-rise" style="animation-delay: 110ms">
      <div class="mb-2.5 flex items-end justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
          <h2 class="font-display text-[19px] tracking-wide text-ink">今日菜单</h2>
          <span v-if="menu" class="text-[11px] text-ink-muted">
            {{ menu.peopleCount }} 人 · {{ dishCount }} 道
          </span>
        </div>
        <RouterLink
          to="/today"
          class="inline-flex h-11 items-center gap-0.5 text-[12px] text-ink-soft transition hover:text-tomato-deep"
        >
          查看全部
          <AppIcon name="chevron-right" :size="14" />
        </RouterLink>
      </div>

      <div class="overflow-hidden rounded-card border border-line bg-card shadow-paper">
        <template v-if="loadingMenu">
          <div class="space-y-3 p-4">
            <SkeletonCard variant="line" :lines="2" />
            <div class="grid gap-2.5 sm:grid-cols-3">
              <div v-for="i in 3" :key="i" class="rounded-xl border border-line/70 bg-paper/50 p-3">
                <SkeletonCard variant="line" :lines="2" />
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="mealPreview.length">
          <div class="flex flex-col divide-y divide-line/70 md:grid md:grid-cols-3 md:divide-y-0">
            <RouterLink
              v-for="meal in mealPreview"
              :key="meal.key"
              to="/today"
              class="flex min-h-[3.5rem] items-center gap-3 px-4 py-2.5 transition hover:bg-paper/60 md:block md:px-3.5 md:py-3"
            >
              <div class="flex shrink-0 items-center gap-1.5 md:mb-1.5">
                <span class="text-[13px] leading-none">
                  {{ meal.key === 'breakfast' ? '🌅' : meal.key === 'lunch' ? '☀️' : '🌙' }}
                </span>
                <span class="font-display text-[14px] text-ink">{{ meal.label }}</span>
                <span class="font-mono text-[10px] text-ink-muted">
                  {{ meal.dishes.length }} 道
                </span>
              </div>

              <!-- 移动端压成一行，整块都可点 -->
              <p class="min-w-0 flex-1 line-clamp-1 text-[12px] text-ink-soft md:hidden">
                {{ dishTitles(meal) }}
              </p>

              <ul class="hidden md:block md:space-y-1">
                <li
                  v-for="dish in meal.dishes"
                  :key="dish.recipeId"
                  class="flex items-center gap-1.5 text-[12.5px] leading-snug text-ink-soft"
                >
                  <span class="shrink-0 text-[11px]">{{ meatTypeEmoji(dish.meatType) }}</span>
                  <span class="line-clamp-1">{{ dish.title }}</span>
                </li>
              </ul>

              <AppIcon
                name="chevron-right"
                :size="15"
                class="ml-1 shrink-0 text-line-strong md:hidden"
              />
            </RouterLink>
          </div>
          <div class="flex items-center justify-between gap-2 border-t border-line/70 bg-paper/40 px-3.5 py-2.5">
            <p class="text-[11px] text-ink-muted">同一天默认结果稳定，想换可以重新生成</p>
            <RouterLink
              to="/today"
              class="inline-flex h-11 items-center gap-1 rounded-full bg-ink px-3.5 text-[12px] font-medium text-paper transition hover:bg-ink-soft"
            >
              去换一道
              <AppIcon name="swap" :size="12" />
            </RouterLink>
          </div>
        </template>

        <div v-else class="p-3.5">
          <EmptyState
            icon="bowl"
            :title="menuFailed ? '今日菜单还没端上来' : '先说说你家口味'"
            :description="
              menuFailed
                ? '可能是后端还没启动，稍后刷新试试；也可以先去菜谱库逛逛。'
                : '设置好人数与荤素偏好，就能一键生成今天的菜单。'
            "
            action-label="去设置偏好"
            @action="$router.push('/settings/preferences')"
          />
        </div>
      </div>
    </section>

    <!-- 分类 -->
    <section class="animate-rise" style="animation-delay: 150ms">
      <div class="mb-2 flex items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-herb" aria-hidden="true" />
          <h2 class="font-display text-[19px] tracking-wide text-ink">按口味找菜</h2>
        </div>
        <RouterLink
          to="/recipes"
          class="inline-flex h-11 items-center gap-0.5 text-[12px] text-ink-soft transition hover:text-tomato-deep"
        >
          全部菜谱
          <AppIcon name="chevron-right" :size="14" />
        </RouterLink>
      </div>

      <div v-if="loadingCategories" class="flex gap-2">
        <div v-for="i in 4" :key="i" class="h-11 w-20 animate-pulse rounded-full bg-paper-deep" />
      </div>
      <CategoryChips
        v-else
        :categories="categories"
        :model-value="null"
        all-label="随便看看"
        @update:model-value="(id) => $router.push({ path: '/recipes', query: id ? { categoryId: String(id) } : {} })"
      />
    </section>

    <!-- 最近菜谱 -->
    <section class="animate-rise" style="animation-delay: 190ms">
      <div class="mb-2.5 flex items-end justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-ginger" aria-hidden="true" />
          <h2 class="font-display text-[19px] tracking-wide text-ink">最近收录</h2>
        </div>
        <RouterLink
          to="/recipes"
          class="inline-flex h-11 items-center gap-0.5 text-[12px] text-ink-soft transition hover:text-tomato-deep"
        >
          菜谱库
          <AppIcon name="chevron-right" :size="14" />
        </RouterLink>
      </div>

      <div v-if="loadingRecipes" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <SkeletonCard v-for="i in 3" :key="i" />
      </div>

      <div v-else-if="recent.length" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <RecipeCard
          v-for="(recipe, index) in recent"
          :key="recipe.id"
          :recipe="recipe"
          class="animate-rise"
          :style="{ animationDelay: `${index * 45}ms` }"
        />
      </div>

      <EmptyState
        v-else
        icon="book"
        title="菜谱库还是空的"
        description="粘贴一条 B 站或抖音的分享链接，让它变成你的第一页手记。"
        action-label="去导入第一条"
        @action="$router.push('/import')"
      />

      <div
        v-if="recent.length"
        class="mt-3 flex items-center justify-between rounded-card border border-dashed border-line-strong/70 bg-card/60 px-4 py-3"
      >
        <p class="text-[11.5px] leading-relaxed text-ink-muted">
          想再添一道？把分享链接丢进来，剩下的交给 AI。
        </p>
        <RouterLink
          to="/import"
          class="inline-flex h-11 shrink-0 items-center gap-1 rounded-full bg-tomato px-3.5 text-[12px] font-medium text-paper transition hover:bg-tomato-deep"
        >
          <AppIcon name="plus" :size="13" />
          导入
        </RouterLink>
      </div>
    </section>
  </div>
</template>
