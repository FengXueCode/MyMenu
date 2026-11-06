<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

import AppIcon from '@/components/AppIcon.vue'

/**
 * 二次确认弹窗：删除菜谱 / 退出登录 / 重新生成菜单等破坏性或不可逆操作前使用。
 */
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    message?: string
    confirmText?: string
    cancelText?: string
    /** danger：番茄红确认按钮（删除类）；default：香草绿确认按钮 */
    tone?: 'default' | 'danger'
    loading?: boolean
  }>(),
  {
    message: '',
    confirmText: '确认',
    cancelText: '取消',
    tone: 'default',
    loading: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

function close(): void {
  emit('update:modelValue', false)
  emit('cancel')
}

function confirm(): void {
  if (props.loading) return
  emit('confirm')
}

function onKeydown(event: KeyboardEvent): void {
  if (!props.modelValue) return
  if (event.key === 'Escape') close()
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
    <Transition name="confirm">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[60] flex items-center justify-center p-6"
        role="alertdialog"
        aria-modal="true"
        :aria-label="title"
      >
        <div class="absolute inset-0 bg-ink/45 backdrop-blur-[2px]" @click="close" />

        <div
          class="confirm-panel relative w-full max-w-[22rem] overflow-hidden rounded-card border border-line bg-card p-5 shadow-lift"
        >
          <div class="flex items-start gap-3">
            <span
              class="grid h-9 w-9 shrink-0 place-items-center rounded-full"
              :class="
                tone === 'danger' ? 'bg-tomato-soft text-tomato-deep' : 'bg-herb-soft text-herb'
              "
            >
              <AppIcon :name="tone === 'danger' ? 'alert' : 'info'" :size="18" />
            </span>
            <div class="min-w-0 flex-1 pt-0.5">
              <h2 class="font-display text-[17px] leading-snug text-ink">{{ title }}</h2>
              <p v-if="message" class="mt-1.5 text-[13px] leading-relaxed text-ink-muted">
                {{ message }}
              </p>
            </div>
          </div>

          <div class="mt-5 flex gap-2.5">
            <button
              type="button"
              class="h-11 flex-1 rounded-full border border-line bg-paper text-[13.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
              @click="close"
            >
              {{ cancelText }}
            </button>
            <button
              type="button"
              class="h-11 flex-1 rounded-full text-[13.5px] font-medium text-paper transition active:scale-[0.99] disabled:opacity-60"
              :class="tone === 'danger' ? 'bg-tomato hover:bg-tomato-deep' : 'bg-herb hover:bg-herb/90'"
              :disabled="loading"
              @click="confirm"
            >
              {{ loading ? '处理中…' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-enter-active,
.confirm-leave-active {
  transition: opacity 200ms ease;
}
.confirm-enter-from,
.confirm-leave-to {
  opacity: 0;
}
.confirm-enter-active .confirm-panel {
  transition: transform 240ms cubic-bezier(0.22, 1, 0.36, 1);
}
.confirm-enter-from .confirm-panel {
  transform: translateY(8px) scale(0.97);
}
</style>
