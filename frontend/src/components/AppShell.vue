<script setup lang="ts">
import { RouterView } from 'vue-router'

import BottomTabs from '@/components/BottomTabs.vue'
import TopNav from '@/components/TopNav.vue'
</script>

<template>
  <div class="flex min-h-screen w-full flex-col overflow-x-clip">
    <!-- 桌面端顶部导航（移动端隐藏） -->
    <TopNav class="hidden md:block" />

    <!-- 内容区：单列手机 → 桌面宽版，统一 1180px 最大宽度 -->
    <main
      class="content-bottom-space mx-auto w-full min-w-0 max-w-[1180px] flex-1 px-4 pt-[calc(1rem+env(safe-area-inset-top,0px))] md:px-8 md:pt-8"
    >
      <RouterView v-slot="{ Component }">
        <component :is="Component" />
      </RouterView>
    </main>

    <!-- 桌面端页脚：手账落款 -->
    <footer class="mx-auto hidden w-full max-w-[1180px] px-8 pb-8 pt-4 md:block">
      <div
        class="flex items-center justify-between gap-4 border-t border-dashed border-line-strong/70 pt-4 text-[11.5px] text-ink-muted"
      >
        <span class="font-display tracking-[0.18em]">一餐一饭，皆是手记。</span>
        <span class="font-mono tracking-[0.18em]">MENU · STOVE NOTES</span>
      </div>
    </footer>

    <!-- 移动端底部 Tab（桌面端隐藏） -->
    <BottomTabs class="md:hidden" />
  </div>
</template>
