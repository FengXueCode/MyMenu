<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'

withDefaults(
  defineProps<{
    /** 图标名（与 AppIcon 一致），与 emoji 二选一 */
    icon?:
      | 'book'
      | 'bowl'
      | 'search'
      | 'sparkle'
      | 'link'
      | 'sliders'
      | 'alert'
      | 'calendar'
      | 'camera'
    emoji?: string
    title: string
    description?: string
    actionLabel?: string
    /** 次要说明（如提示文案） */
    hint?: string
  }>(),
  { icon: 'bowl', emoji: '', title: '', description: '', actionLabel: '', hint: '' },
)

defineEmits<{ action: [] }>()
</script>

<template>
  <div
    class="flex flex-col items-center justify-center rounded-card border border-dashed border-line-strong/80 bg-card/60 px-6 py-10 text-center"
  >
    <!-- 印章式图标圈：虚线外圈 + 实心内圈 -->
    <div class="relative grid h-16 w-16 place-items-center">
      <span class="absolute inset-0 rounded-full border border-dashed border-line-strong" />
      <span class="grid h-12 w-12 place-items-center rounded-full bg-paper-deep text-ink-soft">
        <span v-if="emoji" class="text-xl leading-none">{{ emoji }}</span>
        <AppIcon v-else :name="icon" :size="22" />
      </span>
    </div>

    <h3 class="mt-4 font-display text-lg text-ink">{{ title }}</h3>
    <p v-if="description" class="mt-1.5 max-w-sm text-[13px] leading-relaxed text-ink-muted">
      {{ description }}
    </p>
    <p v-if="hint" class="mt-2 rounded-full bg-ginger-soft px-3 py-1 text-[11.5px] text-[#7d5a12]">
      {{ hint }}
    </p>

    <button
      v-if="actionLabel"
      type="button"
      class="tap-target mt-5 inline-flex items-center gap-1.5 rounded-full bg-ink px-5 text-[13.5px] font-medium text-paper transition hover:bg-ink-soft active:scale-[0.99]"
      @click="$emit('action')"
    >
      {{ actionLabel }}
      <AppIcon name="chevron-right" :size="15" />
    </button>

    <div v-if="$slots.default" class="mt-4 w-full">
      <slot />
    </div>
  </div>
</template>
