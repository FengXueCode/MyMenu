<script setup lang="ts">
import { computed } from 'vue'

/**
 * 内联 SVG 图标集（不引入任何图标库）。
 * 统一 24×24、描边 1.7、圆头圆角，风格贴合「手账细线」。
 */
type IconName =
  | 'home'
  | 'book'
  | 'bowl'
  | 'user'
  | 'search'
  | 'plus'
  | 'minus'
  | 'sparkle'
  | 'camera'
  | 'clock'
  | 'flame'
  | 'chevron-right'
  | 'chevron-left'
  | 'arrow-left'
  | 'close'
  | 'check'
  | 'trash'
  | 'edit'
  | 'refresh'
  | 'swap'
  | 'save'
  | 'link'
  | 'sliders'
  | 'logout'
  | 'filter'
  | 'play'
  | 'pause'
  | 'alert'
  | 'info'
  | 'external'
  | 'calendar'
  | 'tag'
  | 'image'
  | 'eye'
  | 'upload'
  | 'seal'
  | 'pen'

const props = withDefaults(
  defineProps<{
    name: IconName
    size?: number | string
    strokeWidth?: number
  }>(),
  { size: 22, strokeWidth: 1.7 },
)

const PATHS: Record<IconName, string[]> = {
  home: ['M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1v-9.5Z'],
  book: [
    'M6 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z',
    'M8 7h6M8 11h6M8 15h4',
  ],
  bowl: ['M4 11h16a8 8 0 0 1-8 8 8 8 0 0 1-8-8Z', 'M8 7.5c0-1.6 1-2.1 1-3.5M12 7.5c0-1.6 1-2.1 1-3.5M16 7.5c0-1.6 1-2.1 1-3.5'],
  user: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M4 21a8 8 0 0 1 16 0'],
  search: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z', 'M20 20l-4-4'],
  plus: ['M12 5v14M5 12h14'],
  minus: ['M5 12h14'],
  sparkle: ['M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3Z', 'M18.5 15.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9.9-2.1Z'],
  camera: ['M4 8h3l1.5-2.2h7L17 8h3v11H4V8Z', 'M12 16.6a3.6 3.6 0 1 0 0-7.2 3.6 3.6 0 0 0 0 7.2Z'],
  clock: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M12 7v5l3.2 2'],
  flame: ['M12 22a6.2 6.2 0 0 0 6.2-6.2c0-4.1-4.1-6.3-4.1-10.1-1.9 1.9-3.1 3.9-3.1 6.1-1-1-2-2.1-2-4.1-2 2.1-3.2 4.2-3.2 8.1A6.2 6.2 0 0 0 12 22Z'],
  'chevron-right': ['M9.5 6l6 6-6 6'],
  'chevron-left': ['M14.5 6l-6 6 6 6'],
  'arrow-left': ['M20 12H4M10 6l-6 6 6 6'],
  close: ['M6 6l12 12M18 6 6 18'],
  check: ['M5 12.5l4.5 4.5L19 7'],
  trash: ['M4 7h16M9.5 7V4.5h5V7M6 7l1 13h10l1-13M10 11v5M14 11v5'],
  edit: ['M4 20h4L20 8l-4-4L4 16v4Z', 'M14 6l4 4'],
  refresh: ['M20 11a8 8 0 1 0-.9 4.7', 'M20.5 5v6h-6'],
  swap: ['M4 8h13l-3.2-3.2M20 16H7l3.2 3.2'],
  save: ['M5 4h10l4 4v12H5V4Z', 'M9 4v5h6V4M8 20v-6h8v6'],
  link: ['M10.5 13.5a3.6 3.6 0 0 1 0-5.1l2-2a3.6 3.6 0 0 1 5.1 5.1l-2 2', 'M13.5 10.5a3.6 3.6 0 0 1 0 5.1l-2 2a3.6 3.6 0 0 1-5.1-5.1l2-2'],
  sliders: ['M4 7h8M17 7h3M4 17h3M12 17h8', 'M12 7a2 2 0 1 0 4 0 2 2 0 1 0-4 0', 'M7 17a2 2 0 1 0 4 0 2 2 0 1 0-4 0'],
  logout: ['M15 4h4v16h-4', 'M10.5 8 6.5 12l4 4M6.5 12H16'],
  filter: ['M4 6h16M7 12h10M10 18h4'],
  play: ['M8.5 5.5 19 12 8.5 18.5v-13Z'],
  pause: ['M9.5 5v14M14.5 5v14'],
  alert: ['M12 4 21 20H3L12 4Z', 'M12 10.5v4M12 17.2v.3'],
  info: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M12 11v6M12 8v.4'],
  external: ['M14 4h6v6M20 4l-7.5 7.5', 'M18 14v5.5H4.5V6H10'],
  calendar: ['M5 6h14v14H5V6Z', 'M8 3v5M16 3v5M5 11h14'],
  tag: ['M4 4h7.6L20 12.4 12.4 20 4 11.6V4Z', 'M8 8h.4'],
  image: ['M4 5h16v14H4V5Z', 'M4 16l5-5 4 4 3-3 4 4', 'M9 9.6h.4'],
  eye: ['M2.5 12S6 6 12 6s9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'],
  upload: ['M12 16V4M7 9l5-5 5 5M4 20h16'],
  seal: ['M12 3l2.4 1.8 3-.2 1 2.8 2.4 1.8-1 2.8 1 2.8-2.4 1.8-1 2.8-3-.2L12 21l-2.4-1.8-3 .2-1-2.8L3.2 15l1-2.8-1-2.8L5.6 7.6l1-2.8 3 .2L12 3Z'],
  pen: ['M15 3.5 20.5 9 8 21.5H2.5V16L15 3.5Z', 'M12.5 6l5.5 5.5'],
}

const paths = computed(() => PATHS[props.name] ?? [])
const dimension = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
</script>

<template>
  <svg
    :width="dimension"
    :height="dimension"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    class="shrink-0"
  >
    <path v-for="(d, index) in paths" :key="index" :d="d" />
  </svg>
</template>
