<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { aiApi } from '@/api/ai'
import { categoriesApi } from '@/api/categories'
import { ApiError } from '@/api/http'
import { importApi } from '@/api/import'
import { recipesApi } from '@/api/recipes'
import AppIcon from '@/components/AppIcon.vue'
import RecipeForm from '@/components/RecipeForm.vue'
import VideoSourceCard from '@/components/VideoSourceCard.vue'
import { useToastStore } from '@/stores/toast'
import type {
  Category,
  ImportResolveResult,
  RecipeDraft,
  RecipeUpsertInput,
} from '@/types/api'
import { extractUrl } from '@/utils/platform'

const router = useRouter()
const toast = useToastStore()

const STEPS = [
  { index: 1, label: '粘贴链接', hint: '分享链接或整段文案' },
  { index: 2, label: '确认视频', hint: '补充字幕 / 文案' },
  { index: 3, label: '校对保存', hint: '改一改再入库' },
] as const

const step = ref<1 | 2 | 3>(1)

const rawInput = ref('')
const resolveError = ref('')
const resolving = ref(false)
const resolved = ref<ImportResolveResult | null>(null)
const sourceUrl = ref<string | null>(null)

const extraText = ref('')
const extractError = ref('')
const extracting = ref(false)
const aiMissing = ref(false)

const draft = ref<RecipeUpsertInput | null>(null)
const draftTips = ref('')
const saving = ref(false)
const categories = ref<Category[]>([])

const canResolve = computed(() => rawInput.value.trim().length > 0 && !resolving.value)
const transcriptPrefilled = ref(false)

/* -------------------------------- 分类加载 -------------------------------- */

async function loadCategories(): Promise<void> {
  try {
    categories.value = await categoriesApi.list()
  } catch {
    categories.value = []
  }
}

onMounted(() => {
  void loadCategories()
})

/* -------------------------------- 第 1 步 -------------------------------- */

async function resolve(): Promise<void> {
  resolveError.value = ''
  const text = rawInput.value.trim()
  if (!text) {
    resolveError.value = '先粘贴一条分享链接或整段分享文案'
    return
  }

  const url = extractUrl(text)
  const shareText = url ? text.replace(url, '').trim() : text
  if (!url && !shareText) {
    resolveError.value = '没有识别到链接，粘贴整段分享文案也可以'
    return
  }

  resolving.value = true
  try {
    const result = await importApi.resolve({
      url: url ?? undefined,
      shareText: shareText || undefined,
    })
    resolved.value = result
    sourceUrl.value = url ?? (result.bvid ? `https://www.bilibili.com/video/${result.bvid}` : null)
    extraText.value = result.transcript ?? ''
    transcriptPrefilled.value = Boolean(result.transcript)
    step.value = 2
    if (result.warnings.length) {
      toast.info('视频信息没抓全，可以手动补充文案继续')
    }
  } catch (error) {
    resolveError.value =
      error instanceof ApiError ? error.message : '解析失败，稍后再试或手动录入'
  } finally {
    resolving.value = false
  }
}

/* -------------------------------- 第 2 步 -------------------------------- */

function goManual(): void {
  const seedTitle = resolved.value?.title ?? ''
  draftTips.value = ''
  draft.value = createEmptyDraft(seedTitle)
  step.value = 3
}

function backToStep1(): void {
  step.value = 1
}

async function extract(): Promise<void> {
  extractError.value = ''
  aiMissing.value = false
  const meta = resolved.value
  const extra = extraText.value.trim()
  const fallbackText = [meta?.title, meta?.description].filter(Boolean).join('\n')
  const text = extra || fallbackText
  if (!text) {
    extractError.value = '补充一些视频文案或字幕，AI 才能整理出菜谱；也可以手动填写'
    return
  }

  extracting.value = true
  try {
    const result = await aiApi.extract({
      text,
      title: meta?.title,
      platform: meta?.platform,
      extraText: extra || undefined,
    })
    applyDraft(result)
    step.value = 3
    toast.success('AI 已经整理成草稿，检查一下再保存')
  } catch (error) {
    if (error instanceof ApiError && (error.code === 400 || error.message.includes('配置 AI'))) {
      aiMissing.value = true
    } else if (error instanceof ApiError) {
      extractError.value = error.message
    } else {
      extractError.value = 'AI 提取失败，换个文案或手动填写吧'
    }
  } finally {
    extracting.value = false
  }
}

/* -------------------------------- 草稿映射 -------------------------------- */

function createEmptyDraft(title: string): RecipeUpsertInput {
  const meta = resolved.value
  return {
    title,
    description: '',
    categoryId: null,
    coverUrl: meta?.coverUrl ?? null,
    sourcePlatform: meta?.platform ?? null,
    sourceUrl: sourceUrl.value,
    sourceAuthor: meta?.author ?? null,
    durationMin: 30,
    difficulty: 1,
    servings: 2,
    meatType: 'OTHER',
    spiceLevel: 0,
    tags: [],
    ingredients: [{ name: '', amount: '', unit: '' }],
    steps: [{ stepNo: 1, content: '' }],
  }
}

function matchCategoryId(name: string | null | undefined): number | null {
  const target = (name ?? '').trim()
  if (!target) return null
  return categories.value.find((item) => item.name === target)?.id ?? null
}

function applyDraft(result: RecipeDraft): void {
  const meta = resolved.value
  draftTips.value = result.tips ?? ''
  const description = [result.description, result.tips ? `小贴士：${result.tips}` : '']
    .filter(Boolean)
    .join('\n\n')

  draft.value = {
    title: result.title || meta?.title || '',
    description,
    categoryId: matchCategoryId(result.categoryName),
    coverUrl: meta?.coverUrl ?? null,
    sourcePlatform: meta?.platform ?? null,
    sourceUrl: sourceUrl.value,
    sourceAuthor: meta?.author ?? null,
    durationMin: Number(result.cookMinutes) || 30,
    difficulty: (Number(result.difficulty) || 1) as 1 | 2 | 3,
    servings: Number(result.servings) || 2,
    meatType: result.meatType ?? 'OTHER',
    spiceLevel: (Number(result.spiceLevel) || 0) as 0 | 1 | 2 | 3,
    tags: result.tags ?? [],
    ingredients: (result.ingredients ?? []).map((item) => ({
      name: item.name ?? '',
      amount: item.amount ?? '',
      unit: item.unit ?? '',
    })),
    steps: (result.steps ?? []).map((content, index) => ({ stepNo: index + 1, content })),
  }

  if (!draft.value.ingredients.length) {
    draft.value.ingredients = [{ name: '', amount: '', unit: '' }]
  }
  if (!draft.value.steps.length) {
    draft.value.steps = [{ stepNo: 1, content: '' }]
  }
}

/* -------------------------------- 第 3 步 -------------------------------- */

async function save(payload: RecipeUpsertInput): Promise<void> {
  if (saving.value) return
  saving.value = true
  try {
    const created = await recipesApi.create(payload)
    toast.success('收进菜谱库了，今天又有新菜')
    await router.push({ name: 'recipe-detail', params: { id: created.id } })
  } finally {
    saving.value = false
  }
}

function restart(): void {
  step.value = 1
  rawInput.value = ''
  resolved.value = null
  sourceUrl.value = null
  extraText.value = ''
  draft.value = null
  draftTips.value = ''
  resolveError.value = ''
  extractError.value = ''
  aiMissing.value = false
}
</script>

<template>
  <div class="mx-auto w-full max-w-2xl space-y-4">
    <!-- 标题 -->
    <header class="animate-rise">
      <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">NEW ENTRY</p>
      <h1 class="mt-1 font-display text-[26px] leading-tight text-ink md:text-[30px]">
        记一道新菜
      </h1>
      <p class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
        把 B 站 / 抖音的分享链接或文案贴进来，AI 帮你整理成菜谱。
      </p>
    </header>

    <!-- 步骤条 -->
    <ol class="flex items-stretch gap-1.5 rounded-card border border-line bg-card p-2 shadow-paper">
      <li
        v-for="item in STEPS"
        :key="item.index"
        class="flex flex-1 items-center gap-1.5 rounded-xl px-2 py-2 transition-colors"
        :class="step === item.index ? 'bg-paper-deep/70' : ''"
      >
        <span
          class="grid h-7 w-7 shrink-0 place-items-center rounded-full border font-mono text-[11px] transition-colors"
          :class="
            step > item.index
              ? 'border-herb bg-herb text-paper'
              : step === item.index
                ? 'border-tomato bg-tomato text-paper'
                : 'border-line bg-card text-ink-muted'
          "
        >
          <AppIcon v-if="step > item.index" name="check" :size="13" :stroke-width="2.4" />
          <template v-else>{{ item.index }}</template>
        </span>
        <span class="min-w-0">
          <span
            class="block truncate text-[11.5px] font-medium"
            :class="step >= item.index ? 'text-ink' : 'text-ink-muted'"
          >
            {{ item.label }}
          </span>
          <span class="hidden truncate text-[10px] text-ink-muted sm:block">{{ item.hint }}</span>
        </span>
      </li>
    </ol>

    <!-- 第 1 步：粘贴 -->
    <section v-if="step === 1" class="animate-rise space-y-3">
      <div class="rounded-card border border-line bg-card p-4 shadow-paper">
        <label for="import-raw" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
          分享链接 / 整段分享文案
        </label>
        <textarea
          id="import-raw"
          v-model="rawInput"
          rows="5"
          placeholder="例如：&#10;【标题】红烧肉这样做太香了 https://b23.tv/xxxxxx&#10;（直接粘贴 B 站 / 抖音的分享内容就行）"
          class="w-full resize-none rounded-xl border border-line bg-paper/55 px-3 py-2.5 text-[16px] leading-relaxed text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
        />

        <p
          v-if="resolveError"
          class="mt-2 flex items-start gap-1.5 rounded-xl bg-tomato-soft px-3 py-2 text-[12px] text-tomato-deep"
          role="alert"
        >
          <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
          {{ resolveError }}
        </p>

        <div class="mt-3 flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            class="inline-flex h-12 min-h-12 flex-1 items-center justify-center gap-1.5 rounded-full bg-tomato text-[14px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none disabled:opacity-60"
            :disabled="!canResolve"
            @click="resolve"
          >
            <AppIcon name="link" :size="16" />
            {{ resolving ? '解析中…' : '解析这条链接' }}
          </button>
          <button
            type="button"
            class="inline-flex h-12 min-h-12 items-center justify-center gap-1.5 rounded-full border border-line bg-card px-4 text-[13px] text-ink-soft transition hover:border-line-strong hover:text-ink"
            @click="goManual"
          >
            <AppIcon name="edit" :size="15" />
            手动填写
          </button>
        </div>
      </div>

      <div class="rounded-card border border-dashed border-line-strong/80 bg-card/60 p-3.5">
        <p class="flex items-start gap-2 text-[11.5px] leading-relaxed text-ink-muted">
          <AppIcon name="info" :size="13" class="mt-0.5 shrink-0" />
          抖音的页面经常需要登录，可能只能拿到部分信息——这不影响继续，补充一段视频文案就能让 AI 识别得更准。
        </p>
      </div>
    </section>

    <!-- 第 2 步：确认视频 + 补充文案 -->
    <section v-else-if="step === 2 && resolved" class="animate-rise space-y-3">
      <VideoSourceCard :result="resolved" />

      <div class="rounded-card border border-line bg-card p-4 shadow-paper">
        <div class="flex items-center justify-between gap-2">
          <label for="import-extra" class="block text-[12px] font-medium text-ink-soft">
            视频文案 / 字幕
            <span class="ml-1 font-normal text-ink-muted">（可选，越全 AI 越准）</span>
          </label>
          <span class="font-mono text-[10px] text-ink-muted">{{ extraText.length }} 字</span>
        </div>

        <textarea
          id="import-extra"
          v-model="extraText"
          rows="6"
          placeholder="把视频简介、口播文案或字幕粘进来。哪怕只记得大概步骤，也可以先写下来。"
          class="mt-1.5 w-full resize-none rounded-xl border border-line bg-paper/55 px-3 py-2.5 text-[16px] leading-relaxed text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
        />

        <p v-if="transcriptPrefilled" class="mt-1.5 flex items-center gap-1 text-[11px] text-herb">
          <AppIcon name="check" :size="12" />
          已经从 B 站抓到了字幕，可直接使用
        </p>

        <p
          v-if="extractError"
          class="mt-2 flex items-start gap-1.5 rounded-xl bg-tomato-soft px-3 py-2 text-[12px] text-tomato-deep"
          role="alert"
        >
          <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
          {{ extractError }}
        </p>

        <!-- 未配置 AI 的可操作提示 -->
        <div
          v-if="aiMissing"
          class="mt-2 rounded-xl border border-ginger/60 bg-ginger-soft/70 p-3 text-[12px] leading-relaxed text-[#7d5a12]"
        >
          <p class="flex items-start gap-1.5">
            <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
            <span>
              还没有配置 AI 模型，无法自动整理菜谱。
              <RouterLink
                to="/settings/ai"
                class="font-medium underline underline-offset-2 hover:text-tomato-deep"
              >
                去设置 AI 模型
              </RouterLink>
              后再回来；也可以选择手动填写。
            </span>
          </p>
        </div>

        <div class="mt-3 flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            class="inline-flex h-12 min-h-12 flex-1 items-center justify-center gap-1.5 rounded-full bg-tomato text-[14px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none disabled:opacity-60"
            :disabled="extracting"
            @click="extract"
          >
            <AppIcon name="sparkle" :size="16" />
            {{ extracting ? 'AI 正在整理…' : 'AI 整理成菜谱' }}
          </button>
          <button
            type="button"
            class="inline-flex h-12 min-h-12 items-center justify-center gap-1.5 rounded-full border border-line bg-card px-4 text-[13px] text-ink-soft transition hover:border-line-strong hover:text-ink"
            @click="goManual"
          >
            跳过 AI，手动填写
          </button>
        </div>

        <button
          type="button"
          class="mt-2 inline-flex h-11 items-center gap-1 rounded-full px-2 text-[12px] text-ink-muted transition hover:text-ink"
          @click="backToStep1"
        >
          <AppIcon name="chevron-left" :size="14" />
          重新粘贴链接
        </button>
      </div>
    </section>

    <!-- 第 3 步：校对保存 -->
    <section v-else-if="step === 3 && draft" class="animate-rise space-y-3">
      <div class="rounded-card border border-herb/30 bg-herb-soft/40 p-3.5">
        <p class="flex items-start gap-2 text-[12px] leading-relaxed text-herb">
          <AppIcon name="check" :size="14" class="mt-0.5 shrink-0" />
          <span>
            草稿已经整理好了，检查一下分类、份量、配料和步骤，确认无误再保存。
            <span v-if="draftTips" class="mt-1 block text-[11.5px] text-herb/90">
              小贴士：{{ draftTips }}
            </span>
          </span>
        </p>
      </div>

      <RecipeForm
        :initial="draft"
        :categories="categories"
        :submitting="saving"
        submit-label="保存到菜谱库"
        show-source
        @submit="save"
      />

      <div class="flex gap-2">
        <button
          type="button"
          class="inline-flex h-11 items-center gap-1 rounded-full px-3 text-[12px] text-ink-muted transition hover:text-ink"
          @click="step = 2"
        >
          <AppIcon name="chevron-left" :size="14" />
          回到上一步
        </button>
        <button
          type="button"
          class="ml-auto inline-flex h-11 items-center gap-1 rounded-full px-3 text-[12px] text-ink-muted transition hover:text-ink"
          @click="restart"
        >
          重新开始一笔
        </button>
      </div>
    </section>
  </div>
</template>
