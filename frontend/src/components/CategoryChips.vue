<script setup lang="ts">
import type { Category } from '@/types/api'

/** 分类胶囊：移动端横向滚动、不换行；选中态用番茄红实心 */
const props = withDefaults(
  defineProps<{
    categories: Category[]
    modelValue: number | null
    /** 是否显示「全部」 */
    showAll?: boolean
    allLabel?: string
  }>(),
  { showAll: true, allLabel: '全部' },
)

const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()

function select(id: number | null): void {
  emit('update:modelValue', props.modelValue === id ? null : id)
}
</script>

<template>
  <div
    class="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 no-scrollbar md:mx-0 md:flex-wrap md:px-0"
    role="tablist"
    aria-label="菜谱分类"
  >
    <button
      v-if="showAll"
      type="button"
      role="tab"
      :aria-selected="modelValue === null"
      class="inline-flex h-11 shrink-0 items-center gap-1.5 rounded-full border px-3.5 text-[13px] transition"
      :class="
        modelValue === null
          ? 'border-tomato bg-tomato text-paper shadow-[0_2px_0_rgba(176,59,41,0.35)]'
          : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
      "
      @click="select(null)"
    >
      {{ allLabel }}
    </button>

    <button
      v-for="category in categories"
      :key="category.id"
      type="button"
      role="tab"
      :aria-selected="modelValue === category.id"
      class="inline-flex h-11 shrink-0 items-center gap-1.5 rounded-full border px-3.5 text-[13px] transition"
      :class="
        modelValue === category.id
          ? 'border-tomato bg-tomato text-paper shadow-[0_2px_0_rgba(176,59,41,0.35)]'
          : 'border-line bg-card text-ink-soft hover:border-line-strong hover:text-ink'
      "
      @click="select(category.id)"
    >
      <span v-if="category.icon" class="text-[13px] leading-none">{{ category.icon }}</span>
      {{ category.name }}
    </button>
  </div>
</template>
