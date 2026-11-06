<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

import AppIcon from '@/components/AppIcon.vue'

/**
 * 弹层：
 * - 移动端从底部升起的 bottom sheet（圆角上缘 + 安全区内边距）；
 * - ≥768px 变为居中的手账卡片。
 * 自动处理背景滚动锁定、Esc 关闭、点击遮罩关闭。
 */
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    subtitle?: string
    /** 桌面端最大宽度 */
    size?: 'sm' | 'md' | 'lg'
    /** 点击遮罩是否关闭 */
    dismissible?: boolean
  }>(),
  { title: '', subtitle: '', size: 'md', dismissible: true },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
}>()

const SIZE_CLASS: Record<NonNullable<typeof props.size>, string> = {
  sm: 'md:max-w-md',
  md: 'md:max-w-xl',
  lg: 'md:max-w-3xl',
}

function close(): void {
  emit('update:modelValue', false)
  emit('close')
}

function onBackdrop(): void {
  if (props.dismissible) close()
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && props.modelValue) close()
}

let previousOverflow = ''

watch(
  () => props.modelValue,
  (open) => {
    if (typeof document === 'undefined') return
    if (open) {
      previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', onKeydown)
    } else {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', onKeydown)
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') document.body.style.overflow = previousOverflow
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-end justify-center md:items-center md:p-6"
        role="dialog"
        aria-modal="true"
        :aria-label="title || '弹层'"
      >
        <div class="absolute inset-0 bg-ink/45 backdrop-blur-[2px]" @click="onBackdrop" />

        <div
          class="sheet-panel relative flex max-h-[90dvh] w-full flex-col overflow-hidden rounded-t-sheet border border-line bg-card shadow-sheet md:max-h-[86dvh] md:rounded-card md:shadow-lift"
          :class="SIZE_CLASS[size]"
        >
          <!-- 移动端顶部拖拽指示条 -->
          <div class="mx-auto mt-2 h-1 w-10 rounded-full bg-line-strong md:hidden" aria-hidden="true" />

          <header class="flex items-start gap-3 px-5 pb-3 pt-3 md:pt-5">
            <div class="min-w-0 flex-1">
              <h2 v-if="title" class="font-display text-[19px] leading-tight text-ink">
                {{ title }}
              </h2>
              <p v-if="subtitle" class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
                {{ subtitle }}
              </p>
            </div>
            <button
              type="button"
              class="tap-target -mr-2 grid place-items-center rounded-full text-ink-muted transition hover:bg-paper-deep hover:text-ink"
              aria-label="关闭"
              @click="close"
            >
              <AppIcon name="close" :size="18" />
            </button>
          </header>

          <div class="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-4">
            <slot />
          </div>

          <footer
            v-if="$slots.footer"
            class="border-t border-line bg-paper/70 px-5 py-3 pb-[calc(0.75rem+env(safe-area-inset-bottom,0px))]"
          >
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 240ms ease;
}
.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}
.sheet-enter-active .sheet-panel {
  transition:
    transform 280ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 280ms ease;
}
.sheet-enter-from .sheet-panel {
  transform: translateY(28px);
  opacity: 0.6;
}
.sheet-leave-active .sheet-panel {
  transition: transform 180ms ease-in;
}
.sheet-leave-to .sheet-panel {
  transform: translateY(18px);
}
@media (min-width: 768px) {
  .sheet-enter-from .sheet-panel {
    transform: scale(0.97) translateY(8px);
  }
  .sheet-leave-to .sheet-panel {
    transform: scale(0.98);
  }
}
</style>
