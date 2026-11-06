<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { categoriesApi } from '@/api/categories'
import { preferencesApi } from '@/api/preferences'
import AppIcon from '@/components/AppIcon.vue'
import { useToastStore } from '@/stores/toast'
import type { Category, Preferences } from '@/types/api'
import { clamp } from '@/utils/format'

const toast = useToastStore()

const DEFAULT_PREFS: Preferences = {
  peopleCount: 2,
  mealsPerDay: 3,
  dishesPerMeal: 3,
  meatRatio: 1,
  vegRatio: 1,
  maxCookMinutes: 60,
  spiceLevel: 1,
  avoidIngredients: [],
  preferredCategories: [],
}

const form = reactive<Preferences>({ ...DEFAULT_PREFS })
const categories = ref<Category[]>([])
const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)
const savedSnapshot = ref('')
const tagInput = ref('')

const MEAL_OPTIONS = [
  { value: 1, label: '一餐', hint: '简单点' },
  { value: 2, label: '两餐', hint: '早午 / 午晚' },
  { value: 3, label: '三餐', hint: '规律吃饭' },
]

const DISH_OPTIONS = [1, 2, 3, 4, 5, 6]

const RATIO_OPTIONS = [
  { label: '素多一点', hint: '1 荤 : 2 素', meatRatio: 1, vegRatio: 2 },
  { label: '荤素均衡', hint: '1 荤 : 1 素', meatRatio: 1, vegRatio: 1 },
  { label: '荤多一点', hint: '2 荤 : 1 素', meatRatio: 2, vegRatio: 1 },
]

const SPICE_OPTIONS = [
  { value: 0, label: '不吃辣' },
  { value: 1, label: '微辣' },
  { value: 2, label: '中辣' },
  { value: 3, label: '重辣' },
]

const dirty = computed(() => JSON.stringify(form) !== savedSnapshot.value)

const ratioLabel = computed(() => {
  const total = form.meatRatio + form.vegRatio
  if (!total) return '未设置'
  return `荤 ${Math.round((form.meatRatio / total) * 100)}% · 素 ${Math.round(
    (form.vegRatio / total) * 100,
  )}%`
})

const selectedRatioIndex = computed(() =>
  RATIO_OPTIONS.findIndex(
    (option) => option.meatRatio === form.meatRatio && option.vegRatio === form.vegRatio,
  ),
)

/* -------------------------------- 加载/保存 ------------------------------- */

async function load(): Promise<void> {
  loading.value = true
  loadError.value = false
  try {
    const prefs = await preferencesApi.get()
    Object.assign(form, DEFAULT_PREFS, prefs, {
      avoidIngredients: [...(prefs.avoidIngredients ?? [])],
      preferredCategories: [...(prefs.preferredCategories ?? [])],
    })
    savedSnapshot.value = JSON.stringify(form)
  } catch {
    loadError.value = true
    Object.assign(form, DEFAULT_PREFS)
  } finally {
    loading.value = false
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await categoriesApi.list()
  } catch {
    categories.value = []
  }
}

function validate(): string | null {
  if (form.peopleCount < 1 || form.peopleCount > 10) return '用餐人数需在 1–10 之间'
  if (form.mealsPerDay < 1 || form.mealsPerDay > 3) return '每日餐数需在 1–3 之间'
  if (form.dishesPerMeal < 1 || form.dishesPerMeal > 6) return '每餐菜品数需在 1–6 之间'
  if (form.meatRatio < 0 || form.vegRatio < 0 || form.meatRatio + form.vegRatio <= 0)
    return '荤素比不能同时为 0'
  if (form.maxCookMinutes < 15 || form.maxCookMinutes > 240) return '最长时长需在 15–240 分钟之间'
  if (form.spiceLevel < 0 || form.spiceLevel > 3) return '辣度需在 0–3 之间'
  return null
}

async function save(): Promise<void> {
  const error = validate()
  if (error) {
    toast.error(error)
    return
  }
  saving.value = true
  try {
    const payload: Preferences = {
      peopleCount: Math.round(form.peopleCount),
      mealsPerDay: Math.round(form.mealsPerDay),
      dishesPerMeal: Math.round(form.dishesPerMeal),
      meatRatio: form.meatRatio,
      vegRatio: form.vegRatio,
      maxCookMinutes: Math.round(form.maxCookMinutes),
      spiceLevel: Math.round(form.spiceLevel),
      avoidIngredients: form.avoidIngredients,
      preferredCategories: form.preferredCategories,
    }
    const updated = await preferencesApi.update(payload)
    Object.assign(form, payload, updated)
    savedSnapshot.value = JSON.stringify(form)
    toast.success('口味记好了，今日菜单会照着来')
  } finally {
    saving.value = false
  }
}

/* --------------------------------- 交互 ---------------------------------- */

function changePeople(delta: number): void {
  form.peopleCount = clamp(form.peopleCount + delta, 1, 10)
}

function addAvoidIngredient(): void {
  const value = tagInput.value.trim()
  if (!value) return
  if (!form.avoidIngredients.includes(value) && form.avoidIngredients.length < 20) {
    form.avoidIngredients.push(value)
  }
  tagInput.value = ''
}

function onTagKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' || event.key === ',' || event.key === '，') {
    event.preventDefault()
    addAvoidIngredient()
  } else if (event.key === 'Backspace' && !tagInput.value && form.avoidIngredients.length) {
    form.avoidIngredients.pop()
  }
}

function removeAvoidIngredient(name: string): void {
  form.avoidIngredients = form.avoidIngredients.filter((item) => item !== name)
}

function toggleCategory(id: number): void {
  form.preferredCategories = form.preferredCategories.includes(id)
    ? form.preferredCategories.filter((item) => item !== id)
    : [...form.preferredCategories, id]
}

onMounted(() => {
  void load()
  void loadCategories()
})
</script>

<template>
  <div class="mx-auto w-full max-w-2xl space-y-4">
    <header class="animate-rise flex items-end justify-between gap-3">
      <div class="min-w-0">
        <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">TASTE NOTES</p>
        <h1 class="mt-1 font-display text-[26px] leading-tight text-ink md:text-[30px]">
          用餐偏好
        </h1>
        <p class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
          这些偏好会用于生成今日菜单，随时可以改。
        </p>
      </div>
      <span
        v-if="dirty && !loading"
        class="shrink-0 rounded-full bg-ginger-soft px-2.5 py-1 text-[10.5px] text-[#7d5a12]"
      >
        未保存
      </span>
    </header>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="i in 3"
        :key="i"
        class="rounded-card border border-line bg-card p-4 shadow-paper"
      >
        <div class="h-4 w-24 animate-pulse rounded-full bg-paper-deep" />
        <div class="mt-3 h-11 w-full animate-pulse rounded-xl bg-paper-deep" />
      </div>
    </div>

    <div v-else-if="loadError" class="rounded-card border border-line bg-card p-6 text-center">
      <p class="font-display text-[17px] text-ink">偏好没读出来</p>
      <p class="mt-1 text-[12.5px] text-ink-muted">后端可能还没启动，重试一下。</p>
      <button
        type="button"
        class="mt-4 inline-flex h-11 items-center gap-1.5 rounded-full bg-ink px-5 text-[13px] text-paper"
        @click="load"
      >
        <AppIcon name="refresh" :size="14" />
        重试
      </button>
    </div>

    <form v-else class="space-y-3" @submit.prevent="save">
      <!-- 人数 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-2">
            <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
            <h2 class="font-display text-[17px] text-ink">家里几口人</h2>
          </div>

          <div class="flex items-center gap-1 rounded-full border border-line bg-paper/60 p-0.5">
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full text-ink-soft transition hover:bg-card hover:text-ink disabled:opacity-40"
              aria-label="减少人数"
              :disabled="form.peopleCount <= 1"
              @click="changePeople(-1)"
            >
              <AppIcon name="minus" :size="16" />
            </button>
            <span class="min-w-[4rem] text-center">
              <span class="font-display text-[22px] leading-none text-ink">
                {{ form.peopleCount }}
              </span>
              <span class="ml-1 text-[11px] text-ink-muted">人</span>
            </span>
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full text-ink-soft transition hover:bg-card hover:text-ink disabled:opacity-40"
              aria-label="增加人数"
              :disabled="form.peopleCount >= 10"
              @click="changePeople(1)"
            >
              <AppIcon name="plus" :size="16" />
            </button>
          </div>
        </div>
        <p class="mt-2 text-[11.5px] text-ink-muted">
          菜单里的配料用量会按这个人数等比换算。
        </p>
      </section>

      <!-- 餐次与菜品数 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-herb" aria-hidden="true" />
          <h2 class="font-display text-[17px] text-ink">每天吃几餐</h2>
        </div>
        <div class="mt-2.5 grid grid-cols-3 gap-2">
          <button
            v-for="option in MEAL_OPTIONS"
            :key="option.value"
            type="button"
            class="flex min-h-[4rem] flex-col items-center justify-center gap-0.5 rounded-xl border px-2 py-2 transition"
            :class="
              form.mealsPerDay === option.value
                ? 'border-ink bg-ink text-paper'
                : 'border-line bg-paper/50 text-ink-soft hover:border-line-strong'
            "
            @click="form.mealsPerDay = option.value"
          >
            <span class="font-display text-[15px]">{{ option.label }}</span>
            <span
              class="text-[10.5px]"
              :class="form.mealsPerDay === option.value ? 'text-paper/75' : 'text-ink-muted'"
            >
              {{ option.hint }}
            </span>
          </button>
        </div>

        <div class="mt-4 flex items-center justify-between">
          <h3 class="text-[13px] font-medium text-ink-soft">每餐几道菜</h3>
          <span class="font-mono text-[11px] text-ink-muted">{{ form.dishesPerMeal }} 道</span>
        </div>
        <div class="mt-2 flex gap-1.5">
          <button
            v-for="value in DISH_OPTIONS"
            :key="value"
            type="button"
            class="h-11 flex-1 rounded-full border text-[13px] transition"
            :class="
              form.dishesPerMeal === value
                ? 'border-tomato bg-tomato text-paper'
                : 'border-line bg-paper/50 text-ink-soft hover:border-line-strong'
            "
            @click="form.dishesPerMeal = value"
          >
            {{ value }}
          </button>
        </div>
      </section>

      <!-- 荤素比 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-ginger" aria-hidden="true" />
          <h2 class="font-display text-[17px] text-ink">荤素比例</h2>
          <span class="ml-auto font-mono text-[10.5px] text-ink-muted">{{ ratioLabel }}</span>
        </div>
        <p
          v-if="selectedRatioIndex === -1"
          class="mt-2 rounded-lg bg-paper-deep/60 px-2.5 py-1.5 text-[11px] leading-relaxed text-ink-muted"
        >
          当前是自定义比例，选下面任一预设可以快速切换。
        </p>
        <div class="mt-2.5 grid grid-cols-3 gap-2">
          <button
            v-for="(option, index) in RATIO_OPTIONS"
            :key="option.label"
            type="button"
            class="flex min-h-[4rem] flex-col items-center justify-center gap-0.5 rounded-xl border px-1 py-2 transition"
            :class="
              selectedRatioIndex === index
                ? 'border-ink bg-ink text-paper'
                : 'border-line bg-paper/50 text-ink-soft hover:border-line-strong'
            "
            @click="
              () => {
                form.meatRatio = option.meatRatio
                form.vegRatio = option.vegRatio
              }
            "
          >
            <span class="font-display text-[14px]">{{ option.label }}</span>
            <span
              class="font-mono text-[10px]"
              :class="selectedRatioIndex === index ? 'text-paper/70' : 'text-ink-muted'"
            >
              {{ option.hint }}
            </span>
          </button>
        </div>
      </section>

      <!-- 时长与辣度 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
          <h2 class="font-display text-[17px] text-ink">做饭时间与辣度</h2>
        </div>

        <div class="mt-3 flex items-center justify-between">
          <span class="text-[13px] text-ink-soft">单道菜最长用时</span>
          <span class="font-mono text-[12.5px] text-ink">≤ {{ form.maxCookMinutes }} 分钟</span>
        </div>
        <input
          v-model.number="form.maxCookMinutes"
          type="range"
          min="15"
          max="240"
          step="15"
          aria-label="单道菜最长用时"
          class="mt-1.5 h-11 w-full accent-tomato"
        />
        <div class="flex justify-between font-mono text-[9.5px] text-ink-muted">
          <span>15 分钟</span>
          <span>240 分钟</span>
        </div>

        <div class="mt-3 flex items-center justify-between">
          <span class="text-[13px] text-ink-soft">辣度</span>
          <span class="text-[11px] text-ink-muted">今日菜单会尽量避开过辣</span>
        </div>
        <div class="mt-2 flex gap-1.5">
          <button
            v-for="option in SPICE_OPTIONS"
            :key="option.value"
            type="button"
            class="h-11 flex-1 rounded-full border text-[12.5px] transition"
            :class="
              form.spiceLevel === option.value
                ? 'border-tomato bg-tomato text-paper'
                : 'border-line bg-paper/50 text-ink-soft hover:border-line-strong'
            "
            @click="form.spiceLevel = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </section>

      <!-- 忌口 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-herb" aria-hidden="true" />
          <h2 class="font-display text-[17px] text-ink">忌口食材</h2>
          <span class="ml-auto font-mono text-[10.5px] text-ink-muted">
            {{ form.avoidIngredients.length }} 项
          </span>
        </div>
        <p class="mt-1 text-[11.5px] text-ink-muted">回车添加，菜单会尽量避开这些食材。</p>

        <div
          class="mt-2 flex min-h-12 flex-wrap items-center gap-1.5 rounded-xl border border-line bg-paper/55 px-2 py-1.5 focus-within:border-tomato/50 focus-within:ring-2 focus-within:ring-tomato/15"
        >
          <span
            v-for="item in form.avoidIngredients"
            :key="item"
            class="inline-flex items-center gap-1 rounded-full bg-tomato-soft px-2 py-1 text-[12px] text-tomato-deep"
          >
            {{ item }}
            <button
              type="button"
              class="grid h-5 w-5 place-items-center rounded-full hover:text-ink"
              :aria-label="`移除忌口 ${item}`"
              @click="removeAvoidIngredient(item)"
            >
              <AppIcon name="close" :size="11" />
            </button>
          </span>
          <input
            v-model="tagInput"
            type="text"
            maxlength="10"
            placeholder="例如：香菜、海鲜"
            aria-label="添加忌口食材"
            class="min-w-[7rem] flex-1 border-0 bg-transparent px-1 py-1.5 text-[16px] text-ink placeholder:text-ink-muted/60 focus:outline-none"
            @keydown="onTagKeydown"
            @blur="addAvoidIngredient"
          />
        </div>
      </section>

      <!-- 偏爱分类 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-ginger" aria-hidden="true" />
          <h2 class="font-display text-[17px] text-ink">偏爱的菜系</h2>
          <span class="ml-auto font-mono text-[10.5px] text-ink-muted">
            {{ form.preferredCategories.length }} 个
          </span>
        </div>
        <p class="mt-1 text-[11.5px] text-ink-muted">选中的分类会更容易被抽到今日菜单里。</p>

        <div class="mt-2.5 flex flex-wrap gap-1.5">
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="inline-flex h-11 items-center gap-1.5 rounded-full border px-3.5 text-[13px] transition"
            :class="
              form.preferredCategories.includes(category.id)
                ? 'border-herb bg-herb-soft text-herb'
                : 'border-line bg-paper/50 text-ink-soft hover:border-line-strong'
            "
            @click="toggleCategory(category.id)"
          >
            <span v-if="category.icon">{{ category.icon }}</span>
            {{ category.name }}
            <AppIcon
              v-if="form.preferredCategories.includes(category.id)"
              name="check"
              :size="13"
              :stroke-width="2.2"
            />
          </button>
          <span v-if="!categories.length" class="text-[12px] text-ink-muted">分类还在加载…</span>
        </div>
      </section>

      <!-- 保存 -->
      <div class="sticky bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] z-10 -mx-4 bg-gradient-to-t from-paper via-paper to-transparent px-4 pb-2 pt-4 md:bottom-0 md:mx-0 md:px-0">
        <button
          type="submit"
          class="h-12 w-full rounded-full bg-tomato text-[15px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none disabled:opacity-60"
          :disabled="saving"
        >
          {{ saving ? '保存中…' : '保存偏好' }}
        </button>
      </div>
    </form>
  </div>
</template>
