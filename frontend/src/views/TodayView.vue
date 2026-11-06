<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { ApiError } from '@/api/http'
import { menuApi } from '@/api/menu'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import MealCard from '@/components/MealCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import { useToastStore } from '@/stores/toast'
import type { Meal, MenuPlan } from '@/types/api'
import { formatDateCN, mealLabel, todayISO } from '@/utils/format'

const toast = useToastStore()

const date = todayISO()
const dateLabel = formatDateCN(date)

const menu = ref<MenuPlan | null>(null)
const loading = ref(true)
const loadError = ref(false)
const regenerating = ref(false)
const saving = ref(false)
const saved = ref(false)
const rerollingKey = ref<string | null>(null)
const regenerateConfirmOpen = ref(false)

const dishCount = computed(() =>
  (menu.value?.meals ?? []).reduce((total, meal) => total + meal.dishes.length, 0),
)

const meals = computed<Meal[]>(() =>
  (menu.value?.meals ?? []).map((meal) => ({ ...meal, label: meal.label || mealLabel(meal.key) })),
)

const isEmpty = computed(
  () => !loading.value && !loadError.value && (!menu.value || dishCount.value === 0),
)

async function load(regenerate = false): Promise<void> {
  if (regenerate) {
    regenerating.value = true
  } else {
    loading.value = true
  }
  loadError.value = false
  saved.value = false
  try {
    menu.value = await menuApi.today(date, regenerate)
    if (regenerate) toast.success('换了个新组合，看看合不合胃口')
  } catch (error) {
    if (!regenerate) menu.value = null
    loadError.value = true
    if (error instanceof ApiError && error.code === 400) {
      toast.info('先去设置一下用餐偏好，再来生成菜单')
    }
  } finally {
    loading.value = false
    regenerating.value = false
  }
}

/** 单道换菜：只替换指定餐次的下标，返回整份新菜单 */
async function reroll(meal: Meal, index: number): Promise<void> {
  if (!menu.value || rerollingKey.value) return
  const key = `${meal.key}:${index}`
  rerollingKey.value = key
  try {
    menu.value = await menuApi.reroll({
      date: menu.value.date,
      seq: menu.value.seq,
      mealKey: meal.key,
      index,
    })
    saved.value = false
    toast.success(`「${meal.label}」换了一道`)
  } finally {
    rerollingKey.value = null
  }
}

function loadingIndexFor(meal: Meal): number | null {
  if (!rerollingKey.value) return null
  const [mealKey, index] = rerollingKey.value.split(':')
  return mealKey === meal.key ? Number(index) : null
}

function openRegenerate(): void {
  regenerateConfirmOpen.value = true
}

async function confirmRegenerate(): Promise<void> {
  regenerateConfirmOpen.value = false
  await load(true)
}

async function saveMenu(): Promise<void> {
  if (!menu.value || saving.value) return
  saving.value = true
  try {
    await menuApi.save({ date: menu.value.date, plan: menu.value })
    saved.value = true
    toast.success('菜单已记进手账')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load(false)
})
</script>

<template>
  <div class="space-y-4">
    <!-- 标题 -->
    <header class="animate-rise">
      <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">
        TODAY'S TABLE · {{ date }}
      </p>
      <div class="mt-1 flex flex-wrap items-end justify-between gap-2">
        <div class="min-w-0">
          <h1 class="font-display text-[26px] leading-tight text-ink md:text-[30px]">今日菜单</h1>
          <p class="mt-1 text-[12.5px] text-ink-muted">
            {{ dateLabel }}
            <template v-if="menu">
              · {{ menu.peopleCount }} 人 · {{ menu.mealsPerDay }} 餐 · {{ dishCount }} 道
            </template>
          </p>
        </div>

        <div class="flex items-center gap-2">
          <RouterLink
            to="/settings/preferences"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-card px-3.5 text-[12px] text-ink-soft transition hover:border-line-strong hover:text-ink"
          >
            <AppIcon name="sliders" :size="14" />
            口味
          </RouterLink>
          <button
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-card px-3.5 text-[12px] text-ink-soft transition hover:border-line-strong hover:text-ink disabled:opacity-60"
            :disabled="regenerating || loading"
            @click="openRegenerate"
          >            <AppIcon name="refresh" :size="14" :class="regenerating ? 'animate-spin' : ''" />
            重新生成
          </button>
          <button
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full bg-ink px-4 text-[12px] font-medium text-paper transition hover:bg-ink-soft disabled:opacity-60"
            :disabled="saving || !menu"
            @click="saveMenu"
          >
            <AppIcon :name="saved ? 'check' : 'save'" :size="14" />
            {{ saving ? '保存中…' : saved ? '已保存' : '保存菜单' }}
          </button>
        </div>
      </div>
    </header>

    <!-- 提示 -->
    <div
      v-if="menu?.warnings?.length"
      class="animate-fade rounded-xl border border-ginger/50 bg-ginger-soft/60 px-3.5 py-2.5"
    >
      <p class="flex items-start gap-1.5 text-[11.5px] leading-relaxed text-[#7d5a12]">
        <AppIcon name="alert" :size="13" class="mt-0.5 shrink-0" />
        <span>
          <span v-for="(warning, index) in menu.warnings" :key="index" class="block">
            {{ warning }}
          </span>
        </span>
      </p>
    </div>

    <!-- 加载 -->
    <div v-if="loading" class="space-y-3">
      <div
        v-for="i in 2"
        :key="i"
        class="rounded-card border border-line bg-card p-4 shadow-paper"
      >
        <SkeletonCard variant="line" :lines="1" />
        <div class="mt-3 grid gap-2">
          <SkeletonCard variant="line" :lines="2" />
          <SkeletonCard variant="line" :lines="2" />
        </div>
      </div>
    </div>

    <!-- 失败 -->
    <div v-else-if="loadError && !menu" class="rounded-card border border-line bg-card p-6 text-center">
      <p class="font-display text-[18px] text-ink">菜单还没端上来</p>
      <p class="mt-1 text-[12.5px] text-ink-muted">
        可能是后端服务没启动，或者偏好还没设置好。
      </p>
      <div class="mt-4 flex justify-center gap-2">
        <button
          type="button"
          class="inline-flex h-11 items-center gap-1.5 rounded-full bg-ink px-5 text-[13px] text-paper transition hover:bg-ink-soft"
          @click="load(false)"
        >
          <AppIcon name="refresh" :size="14" />
          再试一次
        </button>
        <RouterLink
          to="/settings/preferences"
          class="inline-flex h-11 items-center gap-1.5 rounded-full border border-line bg-card px-5 text-[13px] text-ink-soft transition hover:border-line-strong hover:text-ink"
        >
          设置偏好
        </RouterLink>
      </div>
    </div>

    <!-- 空状态：引导偏好设置 -->
    <EmptyState
      v-else-if="isEmpty"
      icon="bowl"
      title="今天想吃什么？"
      description="设置用餐人数、荤素比例和忌口，按一下就能生成今天的菜单。"
      action-label="去设置用餐偏好"
      @action="$router.push('/settings/preferences')"
    >
      <button
        type="button"
        class="inline-flex h-11 items-center gap-1.5 rounded-full border border-line bg-card px-4 text-[12.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
        @click="load(true)"
      >
        <AppIcon name="sparkle" :size="14" />
        直接用默认口味生成一份
      </button>
    </EmptyState>

    <!-- 菜单 -->
    <div v-else class="space-y-3">
      <MealCard
        v-for="(meal, index) in meals"
        :key="meal.key"
        :meal="meal"
        :people-count="menu?.peopleCount ?? 2"
        :loading-index="loadingIndexFor(meal)"
        class="animate-rise"
        :style="{ animationDelay: `${index * 60}ms` }"
        @reroll="(dishIndex) => reroll(meal, dishIndex)"
      />

      <div
        class="flex items-center justify-between gap-3 rounded-card border border-dashed border-line-strong/70 bg-card/60 px-4 py-3"
      >
        <p class="text-[11.5px] leading-relaxed text-ink-muted">
          同一份菜单当天结果稳定；换一道只动那一个位置，重新生成会整份换组合。
        </p>
        <RouterLink
          to="/recipes"
          class="inline-flex h-11 shrink-0 items-center gap-1 rounded-full border border-line px-3 text-[11.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
        >
          去菜谱库
          <AppIcon name="chevron-right" :size="12" />
        </RouterLink>
      </div>
    </div>

    <ConfirmDialog
      v-model="regenerateConfirmOpen"
      title="重新生成今日菜单？"
      message="当前的菜品组合会被替换，同一道菜不一定还能抽到。保存过的菜单记录不受影响。"
      confirm-text="重新生成"
      :loading="regenerating"
      @confirm="confirmRegenerate"
    />
  </div>
</template>
