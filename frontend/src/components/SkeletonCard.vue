<script setup lang="ts">
withDefaults(
  defineProps<{
    /** card：菜谱卡骨架；line：单行文本骨架 */
    variant?: 'card' | 'line'
    lines?: number
  }>(),
  { variant: 'card', lines: 3 },
)
</script>

<template>
  <div
    v-if="variant === 'card'"
    class="overflow-hidden rounded-card border border-line bg-card shadow-paper"
    aria-hidden="true"
  >
    <div class="skeleton-shimmer aspect-[4/3] w-full" />
    <div class="space-y-2.5 p-4">
      <div class="skeleton-shimmer h-4 w-3/4 rounded-full" />
      <div class="skeleton-shimmer h-3 w-full rounded-full" />
      <div class="flex gap-2 pt-1">
        <div class="skeleton-shimmer h-5 w-14 rounded-full" />
        <div class="skeleton-shimmer h-5 w-12 rounded-full" />
      </div>
    </div>
  </div>

  <div v-else class="space-y-2.5" aria-hidden="true">
    <div
      v-for="index in lines"
      :key="index"
      class="skeleton-shimmer h-3.5 rounded-full"
      :class="index === lines ? 'w-2/3' : 'w-full'"
    />
  </div>
</template>

<style scoped>
.skeleton-shimmer {
  background-image: linear-gradient(
    100deg,
    rgba(228, 216, 195, 0.55) 20%,
    rgba(250, 246, 238, 0.95) 42%,
    rgba(228, 216, 195, 0.55) 64%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s linear infinite;
}

@keyframes skeleton-shimmer {
  from {
    background-position: 180% 0;
  }
  to {
    background-position: -80% 0;
  }
}
</style>
