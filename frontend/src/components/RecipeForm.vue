<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import AppIcon from '@/components/AppIcon.vue'
import type {
  Category,
  Difficulty,
  Ingredient,
  MeatType,
  RecipeUpsertInput,
  SourcePlatform,
  SpiceLevel,
  Step,
} from '@/types/api'
import { MEAT_TYPE_OPTIONS, clamp } from '@/utils/format'

/**
 * 菜谱编辑表单：导入流程第 3 步与详情页「编辑」共用。
 * 只负责表单交互与本地校验，保存请求由父页面发起（emit submit）。
 */
const props = withDefaults(
  defineProps<{
    initial: RecipeUpsertInput
    categories: Category[]
    submitLabel?: string
    submitting?: boolean
    /** 导入时来源信息由上层固定，可隐藏来源编辑 */
    showSource?: boolean
  }>(),
  { submitLabel: '保存菜谱', submitting: false, showSource: false },
)

const emit = defineEmits<{ submit: [payload: RecipeUpsertInput] }>()

const fieldClass =
  'w-full rounded-xl border border-line bg-paper/55 px-3 py-2.5 text-[16px] leading-6 text-ink placeholder:text-ink-muted/70 transition focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15'
const labelClass = 'mb-1.5 block text-[12px] font-medium tracking-wide text-ink-soft'

const DIFFICULTY_OPTIONS: { value: Difficulty; label: string }[] = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '进阶' },
]
const SPICE_OPTIONS: { value: SpiceLevel; label: string }[] = [
  { value: 0, label: '不辣' },
  { value: 1, label: '微辣' },
  { value: 2, label: '中辣' },
  { value: 3, label: '重辣' },
]

function cloneDraft(input: RecipeUpsertInput): RecipeUpsertInput {
  const sourcePlatform: SourcePlatform | null = input.sourcePlatform ?? null
  return {
    title: input.title ?? '',
    description: input.description ?? '',
    categoryId: input.categoryId ?? null,
    coverUrl: input.coverUrl ?? null,
    sourcePlatform,
    sourceUrl: input.sourceUrl ?? null,
    sourceAuthor: input.sourceAuthor ?? null,
    durationMin: Number(input.durationMin) || 30,
    difficulty: (Number(input.difficulty) || 1) as Difficulty,
    servings: Number(input.servings) || 2,
    meatType: input.meatType ?? 'OTHER',
    spiceLevel: (Number(input.spiceLevel) || 0) as SpiceLevel,
    tags: [...(input.tags ?? [])],
    ingredients: (input.ingredients ?? []).map((item) => ({ ...item })),
    steps: (input.steps ?? []).map((item, index) => ({ ...item, stepNo: index + 1 })),
  }
}

const form = reactive<RecipeUpsertInput>(cloneDraft(props.initial))
const tagInput = ref('')
const errors = reactive<{ title?: string; ingredients?: string; steps?: string }>({})

watch(
  () => props.initial,
  (next) => {
    Object.assign(form, cloneDraft(next))
    errors.title = undefined
    errors.ingredients = undefined
    errors.steps = undefined
  },
  { deep: true },
)

/* --------------------------------- 配料 ---------------------------------- */

function addIngredient(): void {
  form.ingredients.push({ name: '', amount: '', unit: '' })
}

function removeIngredient(index: number): void {
  form.ingredients.splice(index, 1)
  if (!form.ingredients.length) addIngredient()
}

/* --------------------------------- 步骤 ---------------------------------- */

function addStep(): void {
  form.steps.push({ stepNo: form.steps.length + 1, content: '' })
}

function removeStep(index: number): void {
  form.steps.splice(index, 1)
  form.steps.forEach((step, i) => {
    step.stepNo = i + 1
  })
  if (!form.steps.length) addStep()
}

/* --------------------------------- 标签 ---------------------------------- */

function addTag(): void {
  const value = tagInput.value.trim().replace(/^#/, '')
  if (!value) return
  if (!form.tags.includes(value) && form.tags.length < 8) form.tags.push(value)
  tagInput.value = ''
}

function onTagKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' || event.key === ',' || event.key === '，') {
    event.preventDefault()
    addTag()
  } else if (event.key === 'Backspace' && !tagInput.value && form.tags.length) {
    form.tags.pop()
  }
}

function removeTag(tag: string): void {
  form.tags = form.tags.filter((item) => item !== tag)
}

/* --------------------------------- 提交 ---------------------------------- */

const coveredIngredients = computed(() =>
  form.ingredients.filter((item: Ingredient) => item.name.trim()),
)
const completedSteps = computed(() => form.steps.filter((step: Step) => step.content.trim()))

function onSubmit(): void {
  errors.title = undefined
  errors.ingredients = undefined
  errors.steps = undefined

  if (!form.title.trim()) {
    errors.title = '给这道菜起个名字吧'
    return
  }
  if (!coveredIngredients.value.length) {
    errors.ingredients = '至少填写一样配料'
    return
  }
  if (!completedSteps.value.length) {
    errors.steps = '至少写一个步骤'
    return
  }

  const payload: RecipeUpsertInput = {
    title: form.title.trim(),
    description: form.description.trim(),
    categoryId: form.categoryId,
    coverUrl: form.coverUrl?.trim() ? form.coverUrl.trim() : null,
    sourcePlatform: form.sourcePlatform ?? null,
    sourceUrl: form.sourceUrl?.trim() ? form.sourceUrl.trim() : null,
    sourceAuthor: form.sourceAuthor?.trim() ? form.sourceAuthor.trim() : null,
    durationMin: Math.round(clamp(Number(form.durationMin) || 30, 1, 600)),
    difficulty: form.difficulty,
    servings: Math.round(clamp(Number(form.servings) || 2, 1, 12)),
    meatType: form.meatType,
    spiceLevel: form.spiceLevel,
    tags: [...form.tags],
    ingredients: coveredIngredients.value.map((item, index) => ({
      name: item.name.trim(),
      amount: (item.amount ?? '').toString().trim(),
      unit: (item.unit ?? '').toString().trim(),
      sort: index + 1,
    })),
    steps: completedSteps.value.map((step, index) => ({
      stepNo: index + 1,
      content: step.content.trim(),
      durationSec: step.durationSec ?? null,
    })),
  }
  emit('submit', payload)
}

const pillClass = (active: boolean): string =>
  active
    ? 'border-ink bg-ink text-paper shadow-[0_2px_0_rgba(42,33,28,0.35)]'
    : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
</script>

<template>
  <form class="space-y-4" @submit.prevent="onSubmit">
    <!-- 基本信息 -->
    <section class="rounded-card border border-line bg-card p-4 shadow-paper">
      <div class="mb-3 flex items-center gap-2">
        <span class="h-3.5 w-1 rounded-full bg-tomato" aria-hidden="true" />
        <h2 class="font-display text-[16px] tracking-wide text-ink">基本信息</h2>
      </div>

      <label :class="labelClass" for="recipe-title">菜名</label>
      <input
        id="recipe-title"
        v-model="form.title"
        type="text"
        maxlength="60"
        placeholder="例如：番茄炖牛腩"
        :class="[fieldClass, 'font-display text-[17px]']"
        :aria-invalid="Boolean(errors.title)"
      />
      <p v-if="errors.title" class="mt-1 text-[12px] text-tomato-deep">{{ errors.title }}</p>

      <label :class="[labelClass, 'mt-3']" for="recipe-desc">一句话简介</label>
      <textarea
        id="recipe-desc"
        v-model="form.description"
        rows="3"
        maxlength="300"
        placeholder="这道菜的关键是什么？比如「先煎后炖，汤汁拌饭一绝」"
        :class="fieldClass"
      />

      <div class="mt-3 grid grid-cols-2 gap-3">
        <div>
          <label :class="labelClass" for="recipe-category">分类</label>
          <select id="recipe-category" v-model="form.categoryId" :class="fieldClass">
            <option :value="null">未分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.icon }} {{ category.name }}
            </option>
          </select>
        </div>
        <div>
          <label :class="labelClass" for="recipe-duration">预计时长（分钟）</label>
          <input
            id="recipe-duration"
            v-model.number="form.durationMin"
            type="number"
            inputmode="numeric"
            min="1"
            max="600"
            :class="fieldClass"
          />
        </div>
      </div>

      <div class="mt-3 grid grid-cols-2 gap-3">
        <div>
          <label :class="labelClass" for="recipe-servings">份量（人份）</label>
          <input
            id="recipe-servings"
            v-model.number="form.servings"
            type="number"
            inputmode="numeric"
            min="1"
            max="12"
            :class="fieldClass"
          />
        </div>
        <div>
          <span :class="labelClass">难度</span>
          <div class="flex gap-1.5">
            <button
              v-for="option in DIFFICULTY_OPTIONS"
              :key="option.value"
              type="button"
              class="h-11 flex-1 rounded-full border text-[12px] transition"
              :class="pillClass(form.difficulty === option.value)"
              @click="form.difficulty = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>

      <div class="mt-3">
        <span :class="labelClass">荤素</span>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="option in MEAT_TYPE_OPTIONS"
            :key="option.value"
            type="button"
            class="inline-flex h-11 items-center rounded-full border px-3 text-[12.5px] transition"
            :class="pillClass(form.meatType === (option.value as MeatType))"
            @click="form.meatType = option.value"
          >
            {{ option.emoji }} {{ option.label }}
          </button>
        </div>
      </div>

      <div class="mt-3">
        <span :class="labelClass">辣度</span>
        <div class="flex gap-1.5">
          <button
            v-for="option in SPICE_OPTIONS"
            :key="option.value"
            type="button"
            class="h-11 flex-1 rounded-full border text-[12.5px] transition"
            :class="pillClass(form.spiceLevel === option.value)"
            @click="form.spiceLevel = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 配料 -->
    <section class="rounded-card border border-line bg-card p-4 shadow-paper">
      <div class="mb-3 flex items-center gap-2">
        <span class="h-3.5 w-1 rounded-full bg-herb" aria-hidden="true" />
        <h2 class="font-display text-[16px] tracking-wide text-ink">配料</h2>
        <span class="text-[11px] text-ink-muted">{{ coveredIngredients.length }} 样</span>
        <button
          type="button"
          class="ml-auto inline-flex h-11 items-center gap-1 rounded-full border border-line px-3 text-[12px] text-ink-soft transition hover:border-line-strong hover:text-ink"
          @click="addIngredient"
        >
          <AppIcon name="plus" :size="14" /> 添加
        </button>
      </div>

      <p v-if="errors.ingredients" class="mb-2 text-[12px] text-tomato-deep">
        {{ errors.ingredients }}
      </p>

      <div class="space-y-2">
        <div
          v-for="(ingredient, index) in form.ingredients"
          :key="index"
          class="rounded-xl border border-line/80 bg-paper/40 p-2.5"
        >
          <div class="flex items-center gap-2">
            <input
              v-model="ingredient.name"
              type="text"
              placeholder="食材名称，如 五花肉"
              class="min-w-0 flex-1 rounded-lg border border-transparent bg-transparent px-1 py-1.5 text-[16px] text-ink placeholder:text-ink-muted/70 focus:border-line focus:bg-card focus:outline-none"
              :aria-label="`第 ${index + 1} 样食材名称`"
            />
            <button
              type="button"
              class="grid h-11 w-11 shrink-0 place-items-center rounded-full text-ink-muted transition hover:bg-tomato-soft hover:text-tomato-deep sm:h-9 sm:w-9"
              :aria-label="`删除第 ${index + 1} 样食材`"
              @click="removeIngredient(index)"
            >
              <AppIcon name="trash" :size="15" />
            </button>
          </div>
          <div class="mt-1.5 flex items-center gap-2">
            <input
              v-model="ingredient.amount"
              type="text"
              placeholder="用量，如 300"
              class="min-w-0 flex-1 rounded-lg border border-line bg-card px-2 py-1.5 text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:outline-none"
              :aria-label="`第 ${index + 1} 样食材用量`"
            />
            <input
              v-model="ingredient.unit"
              type="text"
              placeholder="单位 g"
              class="w-24 rounded-lg border border-line bg-card px-2 py-1.5 text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:outline-none"
              :aria-label="`第 ${index + 1} 样食材单位`"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 步骤 -->
    <section class="rounded-card border border-line bg-card p-4 shadow-paper">
      <div class="mb-3 flex items-center gap-2">
        <span class="h-3.5 w-1 rounded-full bg-ginger" aria-hidden="true" />
        <h2 class="font-display text-[16px] tracking-wide text-ink">步骤</h2>
        <span class="text-[11px] text-ink-muted">{{ completedSteps.length }} 步</span>
        <button
          type="button"
          class="ml-auto inline-flex h-11 items-center gap-1 rounded-full border border-line px-3 text-[12px] text-ink-soft transition hover:border-line-strong hover:text-ink"
          @click="addStep"
        >
          <AppIcon name="plus" :size="14" /> 添加
        </button>
      </div>

      <p v-if="errors.steps" class="mb-2 text-[12px] text-tomato-deep">{{ errors.steps }}</p>

      <div class="space-y-2">
        <div
          v-for="(step, index) in form.steps"
          :key="index"
          class="flex items-start gap-2 rounded-xl border border-line/80 bg-paper/40 p-2.5"
        >
          <span
            class="mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full bg-ink font-mono text-[11px] text-paper"
          >
            {{ index + 1 }}
          </span>
          <textarea
            v-model="step.content"
            rows="2"
            placeholder="这一步做什么？火候、时间、判断标准都可以写"
            class="min-w-0 flex-1 resize-none rounded-lg border border-transparent bg-transparent px-1 py-1 text-[16px] leading-relaxed text-ink placeholder:text-ink-muted/70 focus:border-line focus:bg-card focus:outline-none"
            :aria-label="`第 ${index + 1} 步内容`"
          />
          <button
            type="button"
            class="grid h-11 w-11 shrink-0 place-items-center rounded-full text-ink-muted transition hover:bg-tomato-soft hover:text-tomato-deep sm:h-9 sm:w-9"
            :aria-label="`删除第 ${index + 1} 步`"
            @click="removeStep(index)"
          >
            <AppIcon name="trash" :size="15" />
          </button>
        </div>
      </div>
    </section>

    <!-- 标签与来源 -->
    <section class="rounded-card border border-line bg-card p-4 shadow-paper">
      <div class="mb-3 flex items-center gap-2">
        <span class="h-3.5 w-1 rounded-full bg-tomato" aria-hidden="true" />
        <h2 class="font-display text-[16px] tracking-wide text-ink">标签与来源</h2>
      </div>

      <label :class="labelClass" for="recipe-tags">标签（回车添加，最多 8 个）</label>
      <div
        class="flex flex-wrap items-center gap-1.5 rounded-xl border border-line bg-paper/55 px-2 py-1.5 focus-within:border-tomato/50 focus-within:ring-2 focus-within:ring-tomato/15"
      >
        <span
          v-for="tag in form.tags"
          :key="tag"
          class="inline-flex items-center gap-1 rounded-full bg-paper-deep px-2 py-1 text-[12px] text-ink-soft"
        >
          #{{ tag }}
          <button
            type="button"
            class="grid h-5 w-5 place-items-center rounded-full text-ink-muted hover:text-tomato-deep"
            :aria-label="`移除标签 ${tag}`"
            @click="removeTag(tag)"
          >
            <AppIcon name="close" :size="11" />
          </button>
        </span>
        <input
          id="recipe-tags"
          v-model="tagInput"
          type="text"
          maxlength="12"
          placeholder="下饭、快手、宴客…"
          class="min-w-[6rem] flex-1 border-0 bg-transparent px-1 py-1 text-[16px] text-ink placeholder:text-ink-muted/70 focus:outline-none"
          @keydown="onTagKeydown"
          @blur="addTag"
        />
      </div>

      <template v-if="showSource">
        <label :class="[labelClass, 'mt-3']" for="recipe-cover">封面图片地址</label>
        <input
          id="recipe-cover"
          v-model="form.coverUrl"
          type="url"
          placeholder="https://… 或 /api/files/xxx.png"
          :class="fieldClass"
        />
      </template>
    </section>

    <button
      type="submit"
      class="h-12 w-full rounded-full bg-tomato text-[15px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.55)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none disabled:opacity-60"
      :disabled="submitting"
    >
      {{ submitting ? '保存中…' : submitLabel }}
    </button>
  </form>
</template>
