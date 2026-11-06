<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { categoriesApi } from '@/api/categories'
import { recipesApi } from '@/api/recipes'
import AppIcon from '@/components/AppIcon.vue'
import CategoryChips from '@/components/CategoryChips.vue'
import EmptyState from '@/components/EmptyState.vue'
import ModalSheet from '@/components/ModalSheet.vue'
import RecipeCardItem from '@/components/RecipeCard.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import { MEAT_TYPE_OPTIONS } from '@/utils/format'
import type { Category, MeatType, RecipeCard } from '@/types/api'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 12

/* ------------------------------- 筛选状态 -------------------------------- */

function readNumber(value: unknown): number | null {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

function readMeatType(value: unknown): MeatType | null {
  const text = typeof value === 'string' ? value : ''
  return MEAT_TYPE_OPTIONS.some((option) => option.value === text) ? (text as MeatType) : null
}

const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const categoryId = ref<number | null>(readNumber(route.query.categoryId))
const meatType = ref<MeatType | null>(readMeatType(route.query.meatType))
const mine = ref(route.query.mine === 'true')

const categories = ref<Category[]>([])

/* ------------------------------- 列表状态 -------------------------------- */

const records = ref<RecipeCard[]>([])
const total = ref(0)
const pages = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)
const loadedOnce = ref(false)

const hasMore = computed(() => pageNum.value < pages.value)
const activeFilterCount = computed(() => (meatType.value ? 1 : 0) + (mine.value ? 1 : 0))
const pageTitle = computed(() => (mine.value ? '我的菜谱' : '菜谱库'))
const shownCount = computed(() => records.value.length)

/* --------------------------------- 搜索 ---------------------------------- */

const searchInput = ref(keyword.value)
let searchTimer: ReturnType<typeof setTimeout> | null = null

function scheduleSearch(): void {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    keyword.value = searchInput.value.trim()
    void applyFilters()
  }, 300)
}

function submitSearch(): void {
  if (searchTimer) clearTimeout(searchTimer)
  keyword.value = searchInput.value.trim()
  void applyFilters()
}

function clearSearch(): void {
  searchInput.value = ''
  keyword.value = ''
  void applyFilters()
}

/* -------------------------------- 请求 ---------------------------------- */

async function load(reset = false): Promise<void> {
  if (reset) {
    pageNum.value = 1
    loading.value = true
    loadError.value = false
  } else {
    loadingMore.value = true
  }

  try {
    const page = await recipesApi.list({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value ?? undefined,
      meatType: meatType.value ?? undefined,
      mine: mine.value || undefined,
    })
    total.value = page.total ?? 0
    pages.value = page.pages ?? 0
    const incoming = page.records ?? []
    records.value = reset ? incoming : [...records.value, ...incoming]
    loadedOnce.value = true
  } catch {
    if (reset) {
      records.value = []
      loadError.value = true
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore(): Promise<void> {
  if (!hasMore.value || loadingMore.value) return
  pageNum.value += 1
  await load(false)
}

function retry(): void {
  void load(true)
}

/* ----------------------- 筛选变更 → 同步 URL + 拉取 ----------------------- */

function buildQuery(): Record<string, string> {
  const query: Record<string, string> = {}
  if (keyword.value.trim()) query.keyword = keyword.value.trim()
  if (categoryId.value) query.categoryId = String(categoryId.value)
  if (meatType.value) query.meatType = meatType.value
  if (mine.value) query.mine = 'true'
  return query
}

/** 当前 URL 与筛选状态是否一致（用于避免自己改 URL 又触发一次重复请求） */
function queryMatchesState(): boolean {
  const q = route.query
  const qKeyword = typeof q.keyword === 'string' ? q.keyword : ''
  const qCategory = readNumber(q.categoryId)
  const qMeat = readMeatType(q.meatType)
  const qMine = q.mine === 'true'
  return (
    qKeyword === keyword.value.trim() &&
    qCategory === categoryId.value &&
    qMeat === meatType.value &&
    qMine === mine.value
  )
}

async function applyFilters(): Promise<void> {
  if (!queryMatchesState()) {
    await router.replace({ query: buildQuery() })
  }
  await load(true)
}

/* ------------------------------- 移动端筛选 ------------------------------- */

const sheetOpen = ref(false)
const draftMeat = ref<MeatType | null>(meatType.value)
const draftMine = ref(mine.value)

function openSheet(): void {
  draftMeat.value = meatType.value
  draftMine.value = mine.value
  sheetOpen.value = true
}

function applySheet(): void {
  meatType.value = draftMeat.value
  mine.value = draftMine.value
  sheetOpen.value = false
  void applyFilters()
}

function resetSheet(): void {
  draftMeat.value = null
  draftMine.value = false
}

function resetAllFilters(): void {
  searchInput.value = ''
  keyword.value = ''
  categoryId.value = null
  meatType.value = null
  mine.value = false
  void applyFilters()
}

/* -------------------------------- 初始化 --------------------------------- */

async function loadCategories(): Promise<void> {
  try {
    categories.value = await categoriesApi.list()
  } catch {
    categories.value = []
  }
}

onMounted(async () => {
  await nextTick()
  void loadCategories()
  void load(true)
})

// 外部导航（例如从「我的」进入 /recipes?mine=true）时同步筛选并重新加载
watch(
  () => route.query,
  (query) => {
    if (route.name !== 'recipes') return
    if (queryMatchesState()) return
    keyword.value = typeof query.keyword === 'string' ? query.keyword : ''
    searchInput.value = keyword.value
    categoryId.value = readNumber(query.categoryId)
    meatType.value = readMeatType(query.meatType)
    mine.value = query.mine === 'true'
    void load(true)
  },
)

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<template>
  <div class="space-y-4 md:space-y-5">
    <!-- 标题 + 搜索 -->
    <header class="animate-rise">
      <div class="flex items-end justify-between gap-3">
        <div class="min-w-0">
          <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">
            RECIPE LIBRARY
          </p>
          <h1 class="mt-1 font-display text-[26px] leading-tight text-ink md:text-[30px]">
            {{ pageTitle }}
          </h1>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <span class="font-mono text-[11px] text-ink-muted">
            {{ total > 0 ? `${total} 道` : '—' }}
          </span>
          <button
            v-if="mine"
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-card px-3 text-[11.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
            @click="
              () => {
                mine = false
                void applyFilters()
              }
            "
          >
            看全部
          </button>
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="mt-3 flex gap-2">
        <div class="relative min-w-0 flex-1">
          <span class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-ink-muted">
            <AppIcon name="search" :size="17" />
          </span>
          <input
            v-model="searchInput"
            type="search"
            enterkeyhint="search"
            placeholder="搜菜名、食材或描述…"
            aria-label="搜索菜谱"
            class="h-12 w-full rounded-full border border-line bg-card pl-10 pr-10 text-[16px] text-ink shadow-paper placeholder:text-ink-muted/70 transition focus:border-tomato/50 focus:outline-none focus:ring-2 focus:ring-tomato/15"
            @input="scheduleSearch"
            @keydown.enter.prevent="submitSearch"
          />
          <button
            v-if="searchInput"
            type="button"
            class="absolute right-1 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center rounded-full text-ink-muted transition hover:bg-paper-deep hover:text-ink"
            aria-label="清空搜索"
            @click="clearSearch"
          >
            <AppIcon name="close" :size="15" />
          </button>
        </div>

        <!-- 移动端筛选入口 -->
        <button
          type="button"
          class="relative inline-flex h-12 shrink-0 items-center gap-1.5 rounded-full border border-line bg-card px-4 text-[13px] text-ink-soft shadow-paper transition hover:border-line-strong md:hidden"
          @click="openSheet"
        >
          <AppIcon name="filter" :size="16" />
          筛选
          <span
            v-if="activeFilterCount"
            class="grid h-4 min-w-4 place-items-center rounded-full bg-tomato px-1 font-mono text-[10px] text-paper"
          >
            {{ activeFilterCount }}
          </span>
        </button>
      </div>

      <!-- 桌面端：分类 + 荤素 + 我的 -->
      <div class="mt-3 hidden md:block">
        <CategoryChips v-model="categoryId" :categories="categories" @update:model-value="applyFilters" />
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="inline-flex h-11 items-center rounded-full border px-3.5 text-[13px] transition"
            :class="
              meatType === null
                ? 'border-ink bg-ink text-paper'
                : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
            "
            @click="
              () => {
                meatType = null
                void applyFilters()
              }
            "
          >
            全部荤素
          </button>
          <button
            v-for="option in MEAT_TYPE_OPTIONS"
            :key="option.value"
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border px-3.5 text-[13px] transition"
            :class="
              meatType === option.value
                ? 'border-ink bg-ink text-paper'
                : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
            "
            @click="
              () => {
                meatType = meatType === option.value ? null : option.value
                void applyFilters()
              }
            "
          >
            {{ option.emoji }} {{ option.label }}
          </button>

          <span class="mx-1 h-5 w-px bg-line" aria-hidden="true" />

          <button
            type="button"
            class="inline-flex h-11 items-center gap-1.5 rounded-full border px-3.5 text-[13px] transition"
            :class="
              mine
                ? 'border-herb bg-herb-soft text-herb'
                : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
            "
            @click="
              () => {
                mine = !mine
                void applyFilters()
              }
            "
          >
            <AppIcon name="user" :size="14" />
            只看我的
          </button>

          <button
            v-if="keyword || categoryId || meatType || mine"
            type="button"
            class="ml-auto inline-flex h-11 items-center gap-1 rounded-full px-3 text-[12.5px] text-ink-muted transition hover:text-tomato-deep"
            @click="resetAllFilters"
          >
            <AppIcon name="refresh" :size="13" />
            重置
          </button>
        </div>
      </div>

      <!-- 移动端：分类胶囊 -->
      <div class="mt-3 md:hidden">
        <CategoryChips v-model="categoryId" :categories="categories" @update:model-value="applyFilters" />
      </div>
    </header>

    <!-- 列表 -->
    <section>
      <div v-if="loading" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <SkeletonCard v-for="i in 6" :key="i" />
      </div>

      <div v-else-if="loadError" class="rounded-card border border-line bg-card p-6 text-center">
        <p class="font-display text-[17px] text-ink">菜谱没端上来</p>
        <p class="mt-1 text-[12.5px] text-ink-muted">可能是网络或后端服务的问题，重试一下？</p>
        <button
          type="button"
          class="mt-4 inline-flex h-11 items-center gap-1.5 rounded-full bg-ink px-5 text-[13px] font-medium text-paper transition hover:bg-ink-soft"
          @click="retry"
        >
          <AppIcon name="refresh" :size="14" />
          重新加载
        </button>
      </div>

      <div v-else-if="records.length" class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <RecipeCardItem
          v-for="(recipe, index) in records"
          :key="recipe.id"
          :recipe="recipe"
          class="animate-rise"
          :style="{ animationDelay: `${Math.min(index, 8) * 40}ms` }"
        />
      </div>

      <EmptyState
        v-else-if="loadedOnce"
        :icon="keyword ? 'search' : 'book'"
        :title="keyword ? '没有搜到这道菜' : mine ? '你还没有自己的菜谱' : '菜谱库是空的'"
        :description="
          keyword
            ? '换个关键词试试，或者清空筛选看看全部菜谱。'
            : '导入第一条菜谱，把喜欢的做法留下来。'
        "
        :action-label="keyword ? '清空筛选' : '去导入菜谱'"
        @action="keyword ? resetAllFilters() : $router.push('/import')"
      />

      <!-- 加载更多 / 分页 -->
      <div v-if="!loading && records.length" class="mt-4 flex flex-col items-center gap-2">
        <button
          v-if="hasMore"
          type="button"
          class="inline-flex h-12 w-full max-w-xs items-center justify-center gap-2 rounded-full border border-line bg-card text-[13.5px] font-medium text-ink shadow-paper transition hover:border-line-strong hover:shadow-lift disabled:opacity-60"
          :disabled="loadingMore"
          @click="loadMore"
        >
          <AppIcon name="book" :size="15" />
          {{ loadingMore ? '翻页中…' : `再翻一页（还剩 ${total - shownCount} 道）` }}
        </button>
        <p class="font-mono text-[10.5px] tracking-wider text-ink-muted">
          已显示 {{ shownCount }} / {{ total }} 道 · 第 {{ pageNum }} / {{ pages || 1 }} 页
        </p>
      </div>
    </section>

    <!-- 移动端筛选底部弹层 -->
    <ModalSheet v-model="sheetOpen" title="筛选菜谱" subtitle="选完之后点「看这些菜」">
      <div class="space-y-5">
        <div>
          <p class="mb-2 text-[12px] font-medium text-ink-soft">荤素 / 类型</p>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="inline-flex h-11 items-center rounded-full border px-3.5 text-[13px] transition"
              :class="
                draftMeat === null
                  ? 'border-tomato bg-tomato text-paper'
                  : 'border-line bg-card text-ink-soft'
              "
              @click="draftMeat = null"
            >
              全部
            </button>
            <button
              v-for="option in MEAT_TYPE_OPTIONS"
              :key="option.value"
              type="button"
              class="inline-flex h-11 items-center gap-1 rounded-full border px-3.5 text-[13px] transition"
              :class="
                draftMeat === option.value
                  ? 'border-tomato bg-tomato text-paper'
                  : 'border-line bg-card text-ink-soft'
              "
              @click="draftMeat = option.value"
            >
              {{ option.emoji }} {{ option.label }}
            </button>
          </div>
        </div>

        <div>
          <p class="mb-2 text-[12px] font-medium text-ink-soft">分类</p>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="inline-flex h-11 items-center rounded-full border px-3.5 text-[13px] transition"
              :class="
                categoryId === null
                  ? 'border-tomato bg-tomato text-paper'
                  : 'border-line bg-card text-ink-soft'
              "
              @click="categoryId = null"
            >
              全部
            </button>
            <button
              v-for="category in categories"
              :key="category.id"
              type="button"
              class="inline-flex h-11 items-center gap-1 rounded-full border px-3.5 text-[13px] transition"
              :class="
                categoryId === category.id
                  ? 'border-tomato bg-tomato text-paper'
                  : 'border-line bg-card text-ink-soft'
              "
              @click="categoryId = categoryId === category.id ? null : category.id"
            >
              <span v-if="category.icon">{{ category.icon }}</span>
              {{ category.name }}
            </button>
          </div>
        </div>

        <label
          class="flex min-h-[44px] cursor-pointer items-center justify-between rounded-xl border border-line bg-paper/50 px-3.5 py-2.5"
        >
          <span class="flex items-center gap-2 text-[13px] text-ink-soft">
            <AppIcon name="user" :size="15" />
            只看我导入的菜谱
          </span>
          <span
            class="relative h-6 w-11 rounded-full transition-colors"
            :class="draftMine ? 'bg-herb' : 'bg-line-strong'"
          >
            <span
              class="absolute top-0.5 h-5 w-5 rounded-full bg-card shadow transition-transform"
              :class="draftMine ? 'translate-x-[1.4rem]' : 'translate-x-0.5'"
            />
          </span>
          <input v-model="draftMine" type="checkbox" class="sr-only" />
        </label>
      </div>

      <!-- 底部操作放在 footer：自带 safe-area 内边距，不被底部 Tab 遮挡 -->
      <template #footer>
        <div class="flex gap-2.5">
          <button
            type="button"
            class="h-12 flex-1 rounded-full border border-line bg-card text-[13.5px] text-ink-soft"
            @click="resetSheet"
          >
            重置
          </button>
          <button
            type="button"
            class="h-12 flex-[1.4] rounded-full bg-tomato text-[13.5px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] active:translate-y-px"
            @click="applySheet"
          >
            看这些菜
          </button>
        </div>
      </template>
    </ModalSheet>
  </div>
</template>
