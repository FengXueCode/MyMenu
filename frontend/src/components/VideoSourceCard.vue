<script setup lang="ts">
import { computed, ref } from 'vue'

import AppIcon from '@/components/AppIcon.vue'
import type { ImportResolveResult } from '@/types/api'
import { formatSeconds, platformLabel } from '@/utils/format'
import { sourcePageUrl } from '@/utils/platform'

const props = withDefaults(
  defineProps<{
    result: ImportResolveResult
    /** 是否展示「在原站打开」按钮 */
    showSourceLink?: boolean
  }>(),
  { showSourceLink: true },
)

const coverError = ref(false)
const hasCover = computed(() => Boolean(props.result.coverUrl) && !coverError.value)
const sourceUrl = computed(() => sourcePageUrl(props.result))
const duration = computed(() => formatSeconds(props.result.durationSec))
</script>

<template>
  <article class="overflow-hidden rounded-card border border-line bg-card shadow-paper">
    <div class="flex gap-3 p-3">
      <!-- 视频封面 -->
      <div class="relative h-[4.5rem] w-28 shrink-0 overflow-hidden rounded-xl border border-line/70 bg-paper-deep">
        <img
          v-if="hasCover"
          :src="result.coverUrl"
          :alt="result.title || '视频封面'"
          loading="lazy"
          decoding="async"
          class="h-full w-full object-cover"
          @error="coverError = true"
        />
        <span v-else class="grid h-full w-full place-items-center text-xl">🎬</span>
      </div>

      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center gap-1.5 text-[10.5px]">
          <span class="rounded-full bg-tomato-soft px-2 py-0.5 font-medium text-tomato-deep">
            {{ platformLabel(result.platform) }}
          </span>
          <span v-if="result.author" class="truncate text-ink-muted">@{{ result.author }}</span>
          <span v-if="duration" class="text-ink-muted">{{ duration }}</span>
        </div>

        <h3 class="mt-1 line-clamp-2 font-display text-[15px] leading-snug text-ink">
          {{ result.title || '未获取到视频标题' }}
        </h3>

        <p class="mt-1 flex items-center gap-1 text-[11px] text-ink-muted">
          <AppIcon :name="result.canEmbed ? 'play' : 'external'" :size="12" />
          {{ result.canEmbed ? '支持站内播放' : '仅可跳转原站观看' }}
        </p>
      </div>

      <a
        v-if="showSourceLink && sourceUrl"
        :href="sourceUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="tap-target grid shrink-0 place-items-center self-start rounded-full text-ink-muted transition hover:bg-paper-deep hover:text-ink"
        aria-label="在原站打开视频"
      >
        <AppIcon name="external" :size="16" />
      </a>
    </div>

    <!-- warnings：可操作的提示，而不是报错阻断 -->
    <ul
      v-if="result.warnings.length"
      class="space-y-1.5 border-t border-dashed border-line-strong/70 bg-ginger-soft/45 px-3.5 py-2.5"
    >
      <li
        v-for="(warning, index) in result.warnings"
        :key="index"
        class="flex items-start gap-1.5 text-[11.5px] leading-relaxed text-[#7d5a12]"
      >
        <AppIcon name="alert" :size="13" class="mt-0.5 shrink-0" />
        <span>{{ warning }}</span>
      </li>
    </ul>
  </article>
</template>
