<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onBeforeRouteLeave, RouterLink, useRoute, useRouter } from 'vue-router'

import { aiApi } from '@/api/ai'
import { categoriesApi } from '@/api/categories'
import { ApiError } from '@/api/http'
import { recipesApi } from '@/api/recipes'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import ModalSheet from '@/components/ModalSheet.vue'
import RecipeForm from '@/components/RecipeForm.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import { useCookTimerStore } from '@/stores/cookTimer'
import { useToastStore } from '@/stores/toast'
import type { Category, RecipeDetail, RecipeUpsertInput } from '@/types/api'
import {
  difficultyLabel,
  formatDuration,
  formatSeconds,
  meatTypeEmoji,
  meatTypeLabel,
  platformLabel,
  spiceEmoji,
  spiceLabel,
} from '@/utils/format'
import { bilibiliEmbedUrl, canEmbedVideo } from '@/utils/platform'
import { scaleIngredients } from '@/utils/scale'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const recipe = ref<RecipeDetail | null>(null)
const loading = ref(true)
const loadError = ref(false)

const servings = ref(2)
const maxServings = 12
const summaryLoading = ref(false)
const imageLoading = ref(false)
const summaryExtra = ref<{ ingredientsBrief: string[]; pairing: string } | null>(null)
const imageWarning = ref('')
const playerOpen = ref(false)

const editOpen = ref(false)
const saving = ref(false)
const deleteOpen = ref(false)
const deleting = ref(false)

const recipeId = computed(() => Number(route.params.id))
const SERVINGS_KEY = computed(() => `menu:recipe-servings:${recipeId.value}`)

const scaledIngredients = computed(() =>
  recipe.value ? scaleIngredients(recipe.value.ingredients, recipe.value.servings, servings.value) : [],
)

const canEmbed = computed(() => (recipe.value ? canEmbedVideo(recipe.value.sourceUrl) : false))

const embedUrl = computed(() => bilibiliEmbedUrl(recipe.value?.sourceUrl ?? ''))

const hasSummary = computed(() => Boolean(recipe.value?.aiSummary || summaryExtra.value))

const isMine = computed(() => recipe.value?.owner === 'mine')

const editInitial = computed<RecipeUpsertInput>(() => {
  const data = recipe.value
  return {
    title: data?.title ?? '',
    description: data?.description ?? '',
    categoryId: data?.categoryId ?? null,
    coverUrl: data?.coverUrl ?? null,
    sourcePlatform: data?.sourcePlatform ?? null,
    sourceUrl: data?.sourceUrl ?? null,
    sourceAuthor: data?.sourceAuthor ?? null,
    durationMin: data?.durationMin ?? 30,
    difficulty: data?.difficulty ?? 1,
    servings: data?.servings ?? 2,
    meatType: data?.meatType ?? 'OTHER',
    spiceLevel: data?.spiceLevel ?? 0,
    tags: data?.tags ?? [],
    ingredients: data?.ingredients ?? [],
    steps: data?.steps ?? [],
  }
})

const categories = ref<Category[]>([])

/* -------------------------------- 数据加载 -------------------------------- */

function readSavedServings(): number | null {
  try {
    const raw = window.localStorage.getItem(SERVINGS_KEY.value)
    const value = Number(raw)
    return Number.isFinite(value) && value >= 1 && value <= maxServings ? value : null
  } catch {
    return null
  }
}

function saveServings(value: number): void {
  try {
    window.localStorage.setItem(SERVINGS_KEY.value, String(value))
  } catch {
    /* 忽略隐私模式下的写入失败 */
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await categoriesApi.list()
  } catch {
    categories.value = []
  }
}

async function load(): Promise<void> {
  const id = recipeId.value
  if (!Number.isFinite(id) || id <= 0) {
    loadError.value = true
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = false
  playerOpen.value = false
  summaryExtra.value = null
  imageWarning.value = ''
  try {
    recipe.value = await recipesApi.detail(id)
    cookTimer.adoptLegacy(id, recipe.value.title)
    const saved = readSavedServings()
    servings.value = saved ?? recipe.value.servings ?? 2
  } catch (error) {
    recipe.value = null
    loadError.value = true
    // 404 之外的错误已由 http 层提示
    if (error instanceof ApiError && error.code === 404) {
      toast.info('这道菜可能已经被删除了')
    }
  } finally {
    loading.value = false
  }
}

/* -------------------------------- 份量缩放 -------------------------------- */

function changeServings(delta: number): void {
  const next = Math.min(Math.max(servings.value + delta, 1), maxServings)
  if (next === servings.value) return
  servings.value = next
  saveServings(next)
}

function resetServings(): void {
  const base = recipe.value?.servings ?? 2
  servings.value = base
  saveServings(base)
}

/* ---------------------------------- AI ---------------------------------- */

async function generateSummary(): Promise<void> {
  if (!recipe.value || summaryLoading.value) return
  summaryLoading.value = true
  try {
    const result = await aiApi.recipeSummary(recipe.value.id)
    summaryExtra.value = {
      ingredientsBrief: result.ingredientsBrief ?? [],
      pairing: result.pairing ?? '',
    }
    recipe.value.aiSummary = result.aiSummary
    toast.success('AI 摘要写好了')
  } catch (error) {
    if (error instanceof ApiError && error.message.includes('配置')) {
      toast.info('先去设置里配置一套 AI 模型吧')
    }
  } finally {
    summaryLoading.value = false
  }
}

async function generateImage(): Promise<void> {
  if (!recipe.value || imageLoading.value) return
  imageLoading.value = true
  imageWarning.value = ''
  try {
    const result = await aiApi.recipeImage(recipe.value.id)
    recipe.value.coverUrl = result.coverUrl
    if (result.aiGenerated) {
      toast.success('AI 配图已经画好')
    } else {
      imageWarning.value = result.warning || '当前未配置图像模型，已用本地生成的兜底封面'
      toast.info(imageWarning.value)
    }
  } finally {
    imageLoading.value = false
  }
}

/* --------------------------------- 编辑 ---------------------------------- */

function openEdit(): void {
  editOpen.value = true
}

async function submitEdit(payload: RecipeUpsertInput): Promise<void> {
  if (!recipe.value || saving.value) return
  saving.value = true
  try {
    const updated = await recipesApi.update(recipe.value.id, payload)
    recipe.value = updated
    servings.value = Math.min(Math.max(servings.value, 1), maxServings)
    editOpen.value = false
    toast.success('已经改好了')
  } finally {
    saving.value = false
  }
}

/* --------------------------------- 删除 ---------------------------------- */

async function confirmDelete(): Promise<void> {
  if (!recipe.value || deleting.value) return
  deleting.value = true
  try {
    await recipesApi.remove(recipe.value.id)
    deleteOpen.value = false
    toast.success('菜谱已删除')
    await router.replace({ name: 'recipes', query: { mine: 'true' } })
  } finally {
    deleting.value = false
  }
}

/* ------------------------------- 开火计时 ------------------------------- */

const cookTimer = useCookTimerStore()
const timerRecording = ref(false)
const timerMessage = ref('')
const switchTimerOpen = ref(false)

/** 全局只保留一个计时器；本页只展示属于当前菜谱的计时状态 */
const timerActiveHere = computed(() => cookTimer.recipeId === recipeId.value && cookTimer.isActive)
const timerRunning = computed(() => timerActiveHere.value && cookTimer.running)
const timerElapsedMs = computed(() => (timerActiveHere.value ? cookTimer.elapsedMs : 0))
const timerClock = computed(() => (timerActiveHere.value ? cookTimer.clock : '00:00:00'))
const timerStatusText = computed(() => {
  if (!timerActiveHere.value) return '未开始'
  return cookTimer.running ? '计时中' : '已暂停'
})
const timerCanRecord = computed(() => timerElapsedMs.value >= 1000 && !timerRecording.value)
const otherTimerActive = computed(() => cookTimer.isActive && cookTimer.recipeId !== recipeId.value)
const otherTimerTitle = computed(() => cookTimer.recipeTitle || '另一道菜')

function startTimer(): void {
  if (!recipe.value) return
  if (otherTimerActive.value) {
    switchTimerOpen.value = true
    return
  }
  cookTimer.start(recipe.value.id, recipe.value.title)
  timerMessage.value = ''
}

function confirmSwitchTimer(): void {
  if (!recipe.value) return
  cookTimer.reset()
  cookTimer.start(recipe.value.id, recipe.value.title)
  switchTimerOpen.value = false
  timerMessage.value = ''
}

function pauseTimer(): void {
  cookTimer.pause()
}

function resetTimer(): void {
  cookTimer.reset()
  timerMessage.value = ''
}

async function finishTimer(): Promise<void> {
  if (!recipe.value || !timerCanRecord.value) return
  cookTimer.sync()
  const seconds = Math.max(1, Math.round(timerElapsedMs.value / 1000))
  const actualText = formatSeconds(seconds)
  timerRecording.value = true
  try {
    const result = await recipesApi.recordCookSession(recipe.value.id, seconds)
    if (result.updatedEstimate) {
      recipe.value.durationMin = result.estimateMinutes
      timerMessage.value = `本次实际用时 ${actualText}，预估时间已更新为 ${result.estimateMinutes} 分钟（暂停时间不计入）`
      toast.success('计时已记录，预估时间已更新')
    } else {
      timerMessage.value = `本次实际用时 ${actualText}；示例菜谱的公共预估时间保持不变（暂停时间不计入）`
      toast.info('计时已记录')
    }
    cookTimer.reset()
  } catch {
    // 记录失败时暂停计时，保留本次用时方便重试
    cookTimer.pause()
  } finally {
    timerRecording.value = false
  }
}

onBeforeRouteLeave(() => {
  if (timerRunning.value) {
    toast.info('计时还在继续，顶部悬浮条可以随时回来')
  }
})

watch(
  recipeId,
  () => {
    timerMessage.value = ''
    switchTimerOpen.value = false
    void load()
  },
  { immediate: true },
)

void loadCategories()
</script>

<template>
  <div class="pb-16 md:pb-0">
    <!-- 返回 -->
    <div class="mb-3 flex items-center justify-between">
      <button
        type="button"
        class="inline-flex h-11 items-center gap-1.5 rounded-full pr-3 text-[13px] text-ink-soft transition hover:text-tomato-deep"
        @click="router.back()"
      >
        <AppIcon name="arrow-left" :size="16" />
        返回
      </button>
      <RouterLink
        to="/recipes"
        class="inline-flex h-11 items-center gap-1 rounded-full px-3 text-[12px] text-ink-muted transition hover:text-ink"
      >
        菜谱库
        <AppIcon name="chevron-right" :size="13" />
      </RouterLink>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="space-y-4">
      <div class="skeleton-hero aspect-[16/10] w-full rounded-card md:aspect-[21/9]" />
      <SkeletonCard variant="line" :lines="3" />
      <SkeletonCard variant="line" :lines="5" />
    </div>

    <!-- 加载失败 -->
    <EmptyState
      v-else-if="loadError || !recipe"
      icon="alert"
      title="这道菜没找到"
      description="可能已经被删除，或者链接不对。回菜谱库看看别的。"
      action-label="回菜谱库"
      @action="$router.push('/recipes')"
    />

    <article v-else class="animate-rise space-y-4 md:space-y-5">
      <!-- 封面：只压角标，标题写在纸面上（避免与各种封面自带的文字打架） -->
      <div
        class="relative aspect-[16/10] w-full overflow-hidden rounded-card border border-line bg-paper-deep shadow-paper md:aspect-[21/8]"
      >
        <img
          v-if="recipe.coverUrl"
          :src="recipe.coverUrl"
          :alt="recipe.title"
          class="h-full w-full object-cover"
        />
        <div
          v-else
          class="paper-grain grid h-full w-full place-items-center text-4xl"
          aria-hidden="true"
        >
          {{ meatTypeEmoji(recipe.meatType) }}
        </div>
        <div
          class="absolute inset-0 bg-gradient-to-t from-ink/20 via-transparent to-transparent"
          aria-hidden="true"
        />

        <div class="absolute inset-x-0 top-0 flex flex-wrap items-center gap-1.5 p-3">
          <span
            class="rounded-full border border-white/40 bg-card/92 px-2.5 py-0.5 text-[10.5px] font-medium text-ink backdrop-blur"
          >
            {{ meatTypeEmoji(recipe.meatType) }} {{ meatTypeLabel(recipe.meatType) }}
          </span>
          <span
            v-if="recipe.categoryName && recipe.categoryName !== meatTypeLabel(recipe.meatType)"
            class="rounded-full border border-white/40 bg-card/92 px-2.5 py-0.5 text-[10.5px] text-ink-soft backdrop-blur"
          >
            {{ recipe.categoryName }}
          </span>
          <span
            v-if="recipe.sourcePlatform && recipe.sourcePlatform !== 'unknown'"
            class="rounded-full bg-ink/70 px-2.5 py-0.5 text-[10.5px] text-paper backdrop-blur"
          >
            {{ platformLabel(recipe.sourcePlatform) }}
          </span>
          <span
            v-if="recipe.owner === 'mine'"
            class="-rotate-3 rounded-[4px] border border-tomato/60 bg-tomato px-1.5 py-0.5 font-display text-[10px] tracking-[0.14em] text-paper md:hidden"
          >
            我的手记
          </span>
        </div>
      </div>

      <!-- 标题 + 元信息 + 桌面操作 -->
      <div class="rounded-card border border-line bg-card px-4 py-3.5 shadow-paper">
        <div class="flex items-start gap-3">
          <div class="min-w-0 flex-1">
            <h1 class="font-display text-[24px] leading-tight text-ink md:text-[30px]">
              {{ recipe.title }}
            </h1>
            <p
              v-if="recipe.description"
              class="mt-1.5 text-[12.5px] leading-relaxed text-ink-muted md:text-[13px]"
            >
              {{ recipe.description }}
            </p>
          </div>
          <span
            v-if="recipe.owner === 'mine'"
            class="hidden shrink-0 -rotate-3 rounded-[4px] border border-tomato/60 bg-tomato px-2 py-1 font-display text-[10px] tracking-[0.14em] text-paper md:inline-block"
          >
            我的手记
          </span>
        </div>

        <div
          class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-line/70 pt-3"
        >
          <span class="inline-flex items-center gap-1.5 text-[12.5px] text-ink-soft">
            <AppIcon name="clock" :size="14" class="text-ink-muted" />
            {{ formatDuration(recipe.durationMin) }}
          </span>
          <span class="inline-flex items-center gap-1.5 text-[12.5px] text-ink-soft">
            <AppIcon name="user" :size="14" class="text-ink-muted" />
            {{ recipe.servings }} 人份基准
          </span>
          <span class="inline-flex items-center gap-1.5 text-[12.5px] text-ink-soft">
            <AppIcon name="flame" :size="14" class="text-ink-muted" />
            {{ difficultyLabel(recipe.difficulty) }}
          </span>
          <span class="inline-flex items-center gap-1.5 text-[12.5px] text-ink-soft">
            <span class="text-ink-muted">辣度</span>
            {{ spiceLabel(recipe.spiceLevel) }}
            <span v-if="recipe.spiceLevel > 0" class="tracking-[-0.15em]">
              {{ spiceEmoji(recipe.spiceLevel) }}
            </span>
          </span>

          <div class="ml-auto flex items-center gap-2">
            <button
              type="button"
              class="hidden h-11 items-center gap-1.5 rounded-full border border-line bg-paper px-3.5 text-[12.5px] text-ink-soft transition hover:border-line-strong hover:text-ink md:inline-flex"
              :disabled="summaryLoading"
              @click="generateSummary"
            >
              <AppIcon name="sparkle" :size="14" />
              {{ summaryLoading ? '思考中…' : 'AI 摘要' }}
            </button>
            <button
              type="button"
              class="hidden h-11 items-center gap-1.5 rounded-full border border-line bg-paper px-3.5 text-[12.5px] text-ink-soft transition hover:border-line-strong hover:text-ink md:inline-flex"
              :disabled="imageLoading"
              @click="generateImage"
            >
              <AppIcon name="camera" :size="14" />
              {{ imageLoading ? '画图中…' : 'AI 配图' }}
            </button>
            <button
              v-if="isMine"
              type="button"
              class="hidden h-11 items-center gap-1.5 rounded-full bg-ink px-4 text-[12.5px] font-medium text-paper transition hover:bg-ink-soft md:inline-flex"
              @click="openEdit"
            >
              <AppIcon name="edit" :size="14" />
              编辑
            </button>
            <button
              v-if="isMine"
              type="button"
              class="hidden h-11 items-center gap-1.5 rounded-full border border-tomato/35 bg-card px-3.5 text-[12.5px] text-tomato-deep transition hover:bg-tomato-soft md:inline-flex"
              @click="deleteOpen = true"
            >
              <AppIcon name="trash" :size="14" />
              删除
            </button>
          </div>
        </div>
      </div>

      <!-- 开火计时 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper md:p-5">
        <div class="flex items-start gap-3">
          <span
            class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-tomato-soft text-tomato-deep"
          >
            <AppIcon name="clock" :size="17" />
          </span>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="font-display text-[16px] text-ink">开火计时</h2>
              <span class="rounded-full bg-paper-deep px-2 py-0.5 text-[10.5px] text-ink-muted">
                预估 {{ formatDuration(recipe.durationMin) }}
              </span>
              <span
                class="ml-auto text-[10.5px]"
                :class="
                  timerRunning
                    ? 'text-tomato-deep'
                    : timerElapsedMs > 0
                      ? 'text-ink-soft'
                      : 'text-ink-muted'
                "
              >
                {{ timerStatusText }}
              </span>
            </div>

            <div class="mt-2 flex items-end gap-2">
              <p
                class="font-mono text-[34px] leading-none tracking-[0.04em] tabular-nums"
                :class="timerRunning ? 'text-tomato' : 'text-ink'"
              >
                {{ timerClock }}
              </p>
              <span class="pb-1 text-[10.5px] text-ink-muted">本次实际用时</span>
            </div>

            <p class="mt-1.5 text-[11px] leading-relaxed text-ink-muted">
              {{
                timerMessage ||
                (otherTimerActive
                  ? `「${otherTimerTitle}」还在计时（${cookTimer.clock}），开始本菜前会先确认是否放弃它。`
                  : isMine
                    ? '做菜开始时点开始，暂停的时间不计入实际用时；结束后点「结束并记录」，会把预估时间更新为本次实际用时。'
                    : '示例菜谱也可以计时，暂停的时间不计入；结束后只记录本次用时，不修改公共预估时间。')
              }}
            </p>

            <div class="mt-2.5 flex flex-wrap items-center gap-2">
              <button
                v-if="!timerRunning && timerElapsedMs === 0"
                type="button"
                class="inline-flex h-11 items-center gap-1.5 rounded-full bg-tomato px-4 text-[12.5px] font-medium text-paper transition hover:bg-tomato-deep"
                @click="startTimer"
              >
                <AppIcon name="play" :size="14" />
                开始计时
              </button>
              <button
                v-else-if="timerRunning"
                type="button"
                class="inline-flex h-11 items-center gap-1.5 rounded-full border border-line bg-paper px-4 text-[12.5px] text-ink-soft transition hover:border-line-strong"
                @click="pauseTimer"
              >
                <AppIcon name="pause" :size="14" />
                暂停
              </button>
              <button
                v-else
                type="button"
                class="inline-flex h-11 items-center gap-1.5 rounded-full bg-tomato px-4 text-[12.5px] font-medium text-paper transition hover:bg-tomato-deep"
                @click="startTimer"
              >
                <AppIcon name="play" :size="14" />
                继续
              </button>
              <button
                v-if="timerElapsedMs > 0"
                type="button"
                class="inline-flex h-11 items-center gap-1.5 rounded-full border border-herb/40 bg-herb-soft px-3.5 text-[12.5px] text-herb transition hover:bg-herb/15 disabled:opacity-50"
                :disabled="!timerCanRecord"
                @click="finishTimer"
              >
                <AppIcon name="check" :size="14" />
                {{ timerRecording ? '记录中…' : '结束并记录' }}
              </button>
              <button
                v-if="timerElapsedMs > 0 && !timerRecording"
                type="button"
                class="h-11 rounded-full px-3 text-[12px] text-ink-muted transition hover:text-tomato-deep"
                @click="resetTimer"
              >
                放弃
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 兜底封面提示 -->
      <p
        v-if="imageWarning"
        class="flex items-start gap-2 rounded-xl border border-ginger/50 bg-ginger-soft/60 px-3.5 py-2.5 text-[12px] leading-relaxed text-[#7d5a12]"
      >
        <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
        {{ imageWarning }}
      </p>

      <!-- AI 摘要 -->
      <section class="overflow-hidden rounded-card border border-ginger/40 bg-gradient-to-br from-ginger-soft/70 via-card to-card shadow-paper">
        <div class="flex items-start gap-3 p-4">
          <span class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-ginger/25 text-[#8a6215]">
            <AppIcon name="sparkle" :size="17" />
          </span>
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <h2 class="font-display text-[16px] text-ink">灶台速记</h2>
              <span class="font-mono text-[9px] tracking-[0.2em] text-ink-muted">AI SUMMARY</span>
            </div>

            <template v-if="hasSummary">
              <p v-if="recipe.aiSummary" class="mt-1.5 text-[13px] leading-relaxed text-ink-soft">
                {{ recipe.aiSummary }}
              </p>
              <div v-if="summaryExtra?.ingredientsBrief.length" class="mt-2 flex flex-wrap gap-1.5">
                <span
                  v-for="item in summaryExtra.ingredientsBrief"
                  :key="item"
                  class="rounded-full bg-card/80 px-2 py-0.5 text-[11px] text-ink-soft"
                >
                  {{ item }}
                </span>
              </div>
              <p
                v-if="summaryExtra?.pairing"
                class="mt-2 border-l-2 border-ginger/60 pl-2 font-display text-[12.5px] leading-relaxed text-ink-soft"
              >
                搭配建议：{{ summaryExtra.pairing }}
              </p>
            </template>

            <p v-else class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
              还没有 AI 摘要。让它帮你提炼火候要点和搭配建议。
            </p>

            <button
              type="button"
              class="mt-2.5 inline-flex h-11 items-center gap-1.5 rounded-full border border-line-strong/70 bg-card px-3.5 text-[12px] font-medium text-ink-soft transition hover:border-ink hover:text-ink disabled:opacity-60"
              :disabled="summaryLoading"
              @click="generateSummary"
            >
              <AppIcon name="sparkle" :size="13" />
              {{ summaryLoading ? 'AI 正在写…' : hasSummary ? '重新生成摘要' : '生成 AI 摘要' }}
            </button>
          </div>
        </div>
      </section>

      <!-- 配料（按人数缩放） -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex flex-wrap items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
          <h2 class="font-display text-[18px] tracking-wide text-ink">配料</h2>
          <span class="font-mono text-[10.5px] text-ink-muted">
            {{ scaledIngredients.length }} 样
          </span>

          <div class="ml-auto flex items-center gap-1 rounded-full border border-line bg-paper/60 p-0.5">
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full text-ink-soft transition hover:bg-card hover:text-ink disabled:opacity-40"
              aria-label="减少人数"
              :disabled="servings <= 1"
              @click="changeServings(-1)"
            >
              <AppIcon name="minus" :size="15" />
            </button>
            <span class="min-w-[3.2rem] text-center font-mono text-[12.5px] text-ink">
              {{ servings }} 人份
            </span>
            <button
              type="button"
              class="grid h-11 w-11 place-items-center rounded-full text-ink-soft transition hover:bg-card hover:text-ink disabled:opacity-40"
              aria-label="增加人数"
              :disabled="servings >= maxServings"
              @click="changeServings(+1)"
            >
              <AppIcon name="plus" :size="15" />
            </button>
          </div>
        </div>

        <p class="mt-1.5 text-[11.5px] text-ink-muted">
          按
          <span class="font-medium text-ink-soft">{{ servings }}</span>
          人份换算（原谱 {{ recipe.servings }} 人份）
          <button
            v-if="servings !== recipe.servings"
            type="button"
            class="ml-1 text-tomato-deep underline-offset-2 hover:underline"
            @click="resetServings"
          >
            恢复原谱
          </button>
        </p>

        <ul class="mt-3 space-y-2">
          <li
            v-for="(ingredient, index) in scaledIngredients"
            :key="index"
            class="flex items-baseline gap-2 text-[13.5px]"
          >
            <span
              :class="[
                'h-1.5 w-1.5 shrink-0 translate-y-[-2px] rounded-full',
                ingredient.category === '调料' ? 'bg-ginger' : 'bg-herb',
              ]"
              aria-hidden="true"
            />
            <span class="shrink-0 text-ink">{{ ingredient.name }}</span>
            <span class="mx-1 min-w-4 flex-1 border-b border-dotted border-line-strong/70" />
            <span class="shrink-0 font-mono text-[12.5px] text-ink-soft">
              {{ ingredient.amount }}<span class="ml-0.5 text-ink-muted">{{ ingredient.unit }}</span>
            </span>
          </li>
          <li v-if="!scaledIngredients.length" class="text-[12.5px] text-ink-muted">
            这道菜还没记录配料。
          </li>
        </ul>
      </section>

      <!-- 步骤 -->
      <section class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-herb" aria-hidden="true" />
          <h2 class="font-display text-[18px] tracking-wide text-ink">做法</h2>
          <span class="font-mono text-[10.5px] text-ink-muted">{{ recipe.steps.length }} 步</span>
        </div>

        <ol class="mt-3 space-y-3.5">
          <li v-for="step in recipe.steps" :key="step.stepNo" class="flex gap-3">
            <span
              class="mt-0.5 grid h-7 w-7 shrink-0 place-items-center rounded-full border border-ink/15 bg-paper-deep font-mono text-[11.5px] text-ink"
            >
              {{ step.stepNo }}
            </span>
            <div class="min-w-0 flex-1 border-b border-dashed border-line/80 pb-3 last:border-0 last:pb-0">
              <p class="text-[13.5px] leading-relaxed text-ink-soft">{{ step.content }}</p>
              <p v-if="step.durationSec" class="mt-1 inline-flex items-center gap-1 text-[11px] text-ink-muted">
                <AppIcon name="clock" :size="12" />
                {{ formatSeconds(step.durationSec) }}
              </p>
            </div>
          </li>
          <li v-if="!recipe.steps.length" class="text-[12.5px] text-ink-muted">
            这道菜还没记录步骤。
          </li>
        </ol>
      </section>

      <!-- 视频来源 -->
      <section v-if="recipe.sourceUrl" class="overflow-hidden rounded-card border border-line bg-card shadow-paper">
        <div class="flex items-center gap-2 px-4 pt-3.5">
          <span class="h-4 w-1 rounded-full bg-ginger" aria-hidden="true" />
          <h2 class="font-display text-[18px] tracking-wide text-ink">原视频</h2>
          <span class="rounded-full bg-paper-deep px-2 py-0.5 text-[10.5px] text-ink-muted">
            {{ platformLabel(recipe.sourcePlatform) }}
          </span>
          <a
            v-if="recipe.sourceAuthor"
            class="ml-auto truncate text-[11.5px] text-ink-muted"
            :href="recipe.sourceUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            @{{ recipe.sourceAuthor }}
          </a>
        </div>

        <!-- 可嵌入：点击后再加载 iframe，避免一进详情页就拉播放器 -->
        <div v-if="canEmbed && embedUrl" class="p-4">
          <div
            v-if="!playerOpen"
            class="relative aspect-video w-full overflow-hidden rounded-xl border border-line/80 bg-paper-deep"
          >
            <img
              v-if="recipe.coverUrl"
              :src="recipe.coverUrl"
              :alt="`${recipe.title} 视频封面`"
              class="h-full w-full object-cover"
            />
            <div class="absolute inset-0 bg-ink/35" aria-hidden="true" />
            <button
              type="button"
              class="absolute inset-0 grid place-items-center"
              aria-label="播放原视频"
              @click="playerOpen = true"
            >
              <span
                class="grid h-14 w-14 place-items-center rounded-full border border-white/50 bg-card/90 text-tomato-deep shadow-lift transition hover:scale-105"
              >
                <AppIcon name="play" :size="22" />
              </span>
            </button>
            <p
              class="absolute inset-x-0 bottom-2 text-center font-mono text-[10px] tracking-widest text-paper/90"
            >
              点击加载 B 站播放器
            </p>
          </div>

          <iframe
            v-else
            :src="embedUrl"
            :title="`${recipe.title} 原视频`"
            class="aspect-video w-full rounded-xl border border-line/80"
            scrolling="no"
            frameborder="0"
            allow="fullscreen; autoplay; encrypted-media"
            allowfullscreen
            loading="lazy"
          />
        </div>

        <div class="flex flex-wrap items-center gap-2 p-4">
          <a
            :href="recipe.sourceUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex h-11 items-center gap-1.5 rounded-full border border-line bg-paper px-4 text-[12.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
          >
            <AppIcon name="external" :size="14" />
            去原站看完整视频
          </a>
          <p v-if="!canEmbed" class="text-[11.5px] text-ink-muted">
            该来源不支持站内播放，跳转原站观看即可。
          </p>
        </div>
      </section>

      <!-- 标签 -->
      <div v-if="recipe.tags.length" class="flex flex-wrap items-center gap-1.5">
        <span class="text-[11.5px] text-ink-muted">标签</span>
        <span
          v-for="tag in recipe.tags"
          :key="tag"
          class="rounded-full border border-line bg-card px-2.5 py-1 text-[11.5px] text-ink-soft"
        >
          #{{ tag }}
        </span>
      </div>
    </article>

    <!-- 移动端吸底操作（位于底部 Tab 之上） -->
    <div
      v-if="recipe"
      class="fixed inset-x-0 bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] z-30 border-t border-line bg-paper/95 px-4 py-2.5 backdrop-blur-md md:hidden"
    >
      <div class="flex gap-2.5">
        <button
          type="button"
          class="inline-flex h-12 flex-1 items-center justify-center gap-1.5 rounded-full border border-line bg-card text-[13px] font-medium text-ink-soft transition active:scale-[0.99] disabled:opacity-60"
          :disabled="imageLoading"
          @click="generateImage"
        >
          <AppIcon name="camera" :size="16" />
          {{ imageLoading ? '画图中…' : 'AI 配图' }}
        </button>
        <button
          v-if="isMine"
          type="button"
          class="inline-flex h-12 flex-1 items-center justify-center gap-1.5 rounded-full bg-tomato text-[13px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] active:translate-y-px"
          @click="openEdit"
        >
          <AppIcon name="edit" :size="16" />
          编辑
        </button>
        <button
          v-if="isMine"
          type="button"
          class="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-full border border-tomato/40 bg-card text-tomato-deep active:scale-[0.98]"
          aria-label="删除菜谱"
          @click="deleteOpen = true"
        >
          <AppIcon name="trash" :size="16" />
        </button>
        <button
          v-else
          type="button"
          class="inline-flex h-12 flex-1 items-center justify-center gap-1.5 rounded-full bg-tomato text-[13px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] active:translate-y-px disabled:opacity-60"
          :disabled="summaryLoading"
          @click="generateSummary"
        >
          <AppIcon name="sparkle" :size="16" />
          {{ summaryLoading ? '生成中…' : 'AI 摘要' }}
        </button>
      </div>
    </div>

    <!-- 编辑弹层 -->
    <ModalSheet
      v-model="editOpen"
      title="编辑这道菜"
      subtitle="保存后会更新你的菜谱记录"
      size="lg"
    >
      <RecipeForm
        v-if="editOpen"
        :initial="editInitial"
        :categories="categories"
        :submitting="saving"
        submit-label="保存修改"
        show-source
        @submit="submitEdit"
      />
    </ModalSheet>

    <ConfirmDialog
      v-model="deleteOpen"
      title="删除这道菜？"
      :message="`「${recipe?.title ?? '这道菜'}」删除后不可恢复，但它不会影响已保存的今日菜单记录。`"
      confirm-text="删除"
      tone="danger"
      :loading="deleting"
      @confirm="confirmDelete"
    />

    <ConfirmDialog
      v-model="switchTimerOpen"
      title="另一道菜还在计时"
      :message="`「${otherTimerTitle}」已经计时 ${cookTimer.clock}，开始本菜的计时会放弃它。`"
      confirm-text="放弃并开始"
      cancel-text="继续原来的"
      tone="danger"
      @confirm="confirmSwitchTimer"
    />
  </div>
</template>

<style scoped>
.skeleton-hero {
  background-image: linear-gradient(
    100deg,
    rgba(228, 216, 195, 0.55) 20%,
    rgba(250, 246, 238, 0.95) 42%,
    rgba(228, 216, 195, 0.55) 64%
  );
  background-size: 200% 100%;
  animation: hero-shimmer 1.5s linear infinite;
}
@keyframes hero-shimmer {
  from {
    background-position: 180% 0;
  }
  to {
    background-position: -80% 0;
  }
}
</style>
