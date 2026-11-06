<script setup lang="ts">
import { storeToRefs } from 'pinia'

import AppIcon from '@/components/AppIcon.vue'
import { useToastStore, type ToastKind } from '@/stores/toast'

const toast = useToastStore()
const { items } = storeToRefs(toast)

const TONE: Record<ToastKind, { bar: string; icon: string; iconName: 'check' | 'alert' | 'info' }> = {
  success: { bar: 'bg-herb', icon: 'bg-herb-soft text-herb', iconName: 'check' },
  error: { bar: 'bg-tomato', icon: 'bg-tomato-soft text-tomato-deep', iconName: 'alert' },
  info: { bar: 'bg-ginger', icon: 'bg-ginger-soft text-[#8a6215]', iconName: 'info' },
}
</script>

<template>
  <Teleport to="body">
    <!-- 顶部居中轻提示：桌面与移动端都不遮挡底部 Tab -->
    <div
      class="pointer-events-none fixed inset-x-0 top-0 z-[100] flex flex-col items-center gap-2 px-4 pt-safe md:top-[4.5rem]"
      role="status"
      aria-live="polite"
    >
      <TransitionGroup
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 -translate-y-2 scale-98"
        enter-to-class="opacity-100 translate-y-0 scale-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <div
          v-for="item in items"
          :key="item.id"
          class="pointer-events-auto mt-2 flex w-full max-w-sm cursor-pointer items-start gap-2.5 overflow-hidden rounded-card border border-line bg-card/98 py-2 pl-2.5 pr-1 shadow-lift backdrop-blur"
          @click="toast.dismiss(item.id)"
        >
          <span :class="['h-auto w-1 self-stretch rounded-full', TONE[item.kind].bar]" />
          <span
            :class="['grid h-6 w-6 shrink-0 place-items-center rounded-full', TONE[item.kind].icon]"
          >
            <AppIcon :name="TONE[item.kind].iconName" :size="14" :stroke-width="2.1" />
          </span>
          <p class="min-w-0 flex-1 pt-0.5 text-[13px] leading-snug text-ink">{{ item.message }}</p>
          <button
            type="button"
            class="hidden h-11 w-11 shrink-0 place-items-center rounded-full text-ink-muted transition-colors hover:bg-paper-deep hover:text-ink sm:grid"
            aria-label="关闭提示"
            @click.stop="toast.dismiss(item.id)"
          >
            <AppIcon name="close" :size="14" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
