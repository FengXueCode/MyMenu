<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { integrationsApi } from '@/api/integrations'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import ModalSheet from '@/components/ModalSheet.vue'
import { useToastStore } from '@/stores/toast'
import type {
  IntegrationItem,
  IntegrationSource,
  IntegrationStatus,
  QrPollResult,
} from '@/types/api'

const toast = useToastStore()

const status = ref<IntegrationStatus | null>(null)
const loading = ref(true)
const loadFailed = ref(false)
const busy = ref('')
const folderUrl = ref('')
const queueText = ref('')
const items = ref<IntegrationItem[]>([])
const itemFilter = ref('')
const itemLoading = ref(false)
const itemTotal = ref(0)

const qrOpen = ref(false)
const qrImage = ref('')
const qrStatus = ref('WAITING')
const qrMessage = ref('正在生成二维码…')
const qrKey = ref('')
let qrTimer: ReturnType<typeof setInterval> | null = null

const disconnectOpen = ref(false)
const deleteTarget = ref<IntegrationSource | null>(null)
const sourceBusy = ref<number | null>(null)
const deleteBusy = ref(false)

const bilibili = computed(() => status.value?.bilibili ?? null)
const sources = computed(() => status.value?.sources ?? [])
const bilibiliSources = computed(() =>
  sources.value.filter((source) => source.sourceType === 'BILIBILI_FOLDER'),
)
const queueSource = computed(() =>
  sources.value.find((source) => source.sourceType === 'DOUYIN_QUEUE'),
)
const queueStats = computed(
  () => status.value?.queue ?? { pending: 0, imported: 0, skipped: 0, failed: 0 },
)
const hasRunning = computed(() =>
  sources.value.some((source) => source.lastScanStatus === 'RUNNING'),
)

const FILTERS = [
  { value: '', label: '全部' },
  { value: 'IMPORTED', label: '已生成' },
  { value: 'SKIPPED', label: '已跳过' },
  { value: 'PENDING', label: '待处理' },
  { value: 'FAILED', label: '失败' },
]

let refreshTimer: ReturnType<typeof setInterval> | null = null

async function load(silent = false): Promise<void> {
  if (!silent) loading.value = true
  const wasRunning = hasRunning.value
  try {
    status.value = await integrationsApi.status()
    loadFailed.value = false
    if (wasRunning && !hasRunning.value) {
      void loadItems(true)
      toast.success('扫描完成，菜谱库已更新')
    }
  } catch {
    if (!silent) loadFailed.value = true
  } finally {
    if (!silent) loading.value = false
  }
}

async function loadItems(silent = false): Promise<void> {
  if (!silent) itemLoading.value = true
  try {
    const page = await integrationsApi.items({
      status: itemFilter.value || undefined,
      pageNum: 1,
      pageSize: 20,
    })
    items.value = page.records
    itemTotal.value = page.total
  } catch {
    if (!silent) items.value = []
  } finally {
    if (!silent) itemLoading.value = false
  }
}

function stopQrTimer(): void {
  if (qrTimer) {
    clearInterval(qrTimer)
    qrTimer = null
  }
}

function stopQr(): void {
  stopQrTimer()
  qrKey.value = ''
}

async function startBilibiliQr(): Promise<void> {
  qrOpen.value = true
  qrImage.value = ''
  qrStatus.value = 'WAITING'
  qrMessage.value = '正在生成二维码…'
  stopQrTimer()
  try {
    const result = await integrationsApi.startBilibiliQr()
    qrImage.value = result.qrImage
    qrKey.value = result.key
    qrMessage.value = '请用 B 站 App 扫码，然后在手机上确认登录'
    qrTimer = setInterval(async () => {
      if (!qrKey.value || !qrOpen.value) return
      try {
        const poll: QrPollResult = await integrationsApi.pollBilibiliQr(qrKey.value)
        qrStatus.value = poll.status
        qrMessage.value = poll.message
        if (poll.status === 'CONFIRMED') {
          stopQr()
          qrOpen.value = false
          toast.success('B站连接成功')
          await load(true)
        } else if (poll.status === 'EXPIRED' || poll.status === 'FAILED') {
          stopQrTimer()
        }
      } catch {
        stopQrTimer()
        qrMessage.value = '二维码状态查询失败，请关闭后重试'
      }
    }, 2200)
  } catch {
    qrOpen.value = false
  }
}

async function disconnectBilibili(): Promise<void> {
  busy.value = 'disconnect'
  try {
    await integrationsApi.disconnectBilibili()
    toast.success('已断开 B 站连接')
    disconnectOpen.value = false
    await load(true)
  } finally {
    busy.value = ''
  }
}

async function addFolder(): Promise<void> {
  const url = folderUrl.value.trim()
  if (!url) {
    toast.info('先粘贴 B 站收藏夹链接')
    return
  }
  busy.value = 'folder'
  try {
    const source = await integrationsApi.addBilibiliFolder(url)
    folderUrl.value = ''
    toast.success(`已添加收藏夹「${source.folderName ?? ''}」`)
    await load(true)
  } finally {
    busy.value = ''
  }
}

async function scanSource(source: IntegrationSource): Promise<void> {
  sourceBusy.value = source.id
  try {
    const result = await integrationsApi.scanSource(source.id)
    toast.info(result.message)
    await load(true)
  } finally {
    sourceBusy.value = null
  }
}

async function scanAll(): Promise<void> {
  busy.value = 'scan-all'
  try {
    const result = await integrationsApi.scanAll()
    toast.info(result.message)
    await load(true)
  } finally {
    busy.value = ''
  }
}

async function toggleAuto(source: IntegrationSource): Promise<void> {
  sourceBusy.value = source.id
  try {
    await integrationsApi.updateSource(source.id, { autoScan: !source.autoScan })
    await load(true)
  } finally {
    sourceBusy.value = null
  }
}

async function confirmDelete(): Promise<void> {
  const target = deleteTarget.value
  if (!target) return
  deleteBusy.value = true
  try {
    await integrationsApi.deleteSource(target.id)
    toast.success('已删除收藏源')
    deleteTarget.value = null
    await load(true)
  } finally {
    deleteBusy.value = false
  }
}

async function enqueue(): Promise<void> {
  const text = queueText.value.trim()
  if (!text) {
    toast.info('先粘贴抖音/B站分享链接')
    return
  }
  busy.value = 'queue'
  try {
    const result = await integrationsApi.enqueueDouyin(text)
    queueText.value = ''
    toast.success(`已加入 ${result.added} 条，重复 ${result.duplicates} 条，无效 ${result.invalid} 条`)
    await load(true)
    await loadItems(true)
  } finally {
    busy.value = ''
  }
}

async function changeFilter(value: string): Promise<void> {
  itemFilter.value = value
  await loadItems()
}

function formatTime(value: string | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function scanStatusLabel(value: string | null): string {
  switch (value) {
    case 'RUNNING':
      return '扫描中'
    case 'SUCCESS':
      return '上次成功'
    case 'FAILED':
      return '上次失败'
    default:
      return '未扫描'
  }
}

function itemStatusLabel(value: string): string {
  switch (value) {
    case 'IMPORTED':
      return '已生成菜谱'
    case 'SKIPPED':
      return '不是菜谱'
    case 'PENDING':
      return '待处理'
    case 'PROCESSING':
      return '处理中'
    case 'FAILED':
      return '失败'
    default:
      return value
  }
}

function itemStatusClass(value: string): string {
  switch (value) {
    case 'IMPORTED':
      return 'bg-herb-soft text-herb'
    case 'SKIPPED':
      return 'bg-paper-deep text-ink-muted'
    case 'FAILED':
      return 'bg-tomato-soft text-tomato-deep'
    default:
      return 'bg-ginger-soft text-[#8a6215]'
  }
}

onMounted(async () => {
  await load()
  void loadItems()
  refreshTimer = setInterval(() => {
    if (hasRunning.value) void load(true)
  }, 2800)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  stopQrTimer()
})
</script>

<template>
  <div class="mx-auto w-full max-w-3xl space-y-4 pb-6">
    <!-- 标题 -->
    <header class="animate-rise">
      <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">VIDEO TO RECIPE</p>
      <h1 class="mt-1 font-display text-[26px] leading-tight text-ink">视频收藏 · 自动记菜谱</h1>
      <p class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
        B 站收藏夹每天自动扫描，AI 只挑出菜谱视频并整理成菜谱；抖音先用手动分享链接入队，同样每天自动处理。
      </p>
    </header>

    <div v-if="loading" class="space-y-3">
      <div class="h-32 animate-pulse rounded-card bg-paper-deep" />
      <div class="h-48 animate-pulse rounded-card bg-paper-deep" />
    </div>

    <div
      v-else-if="loadFailed"
      class="rounded-card border border-dashed border-tomato/40 bg-tomato-soft/40 p-6 text-center"
    >
      <p class="font-display text-[16px] text-tomato-deep">加载失败</p>
      <button
        class="mt-3 h-11 rounded-full bg-tomato px-4 text-[13px] text-paper"
        type="button"
        @click="load()"
      >
        重新加载
      </button>
    </div>

    <template v-else>
      <!-- B 站账号 -->
      <section class="animate-rise rounded-card border border-line bg-card p-4 shadow-paper md:p-5">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
          <h2 class="font-display text-[18px] text-ink">B 站账号</h2>
          <span
            v-if="bilibili"
            class="ml-auto rounded-full px-2 py-0.5 text-[10.5px]"
            :class="
              bilibili.status === 'ACTIVE'
                ? 'bg-herb-soft text-herb'
                : 'bg-tomato-soft text-tomato-deep'
            "
          >
            {{ bilibili.status === 'ACTIVE' ? '已连接' : '登录已过期' }}
          </span>
        </div>

        <div v-if="bilibili" class="mt-3 flex items-center gap-3">
          <img
            v-if="bilibili.avatar"
            :src="bilibili.avatar"
            alt=""
            class="h-12 w-12 rounded-full border border-line object-cover"
            referrerpolicy="no-referrer"
            loading="lazy"
          />
          <span
            v-else
            class="grid h-12 w-12 place-items-center rounded-full bg-herb-soft font-display text-lg text-herb"
          >
            灶
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate text-[14px] font-medium text-ink">{{ bilibili.nickname || 'B站用户' }}</p>
            <p class="mt-0.5 text-[11px] text-ink-muted">
              上次登录 {{ formatTime(bilibili.lastLoginAt) }}
            </p>
          </div>
          <button
            v-if="bilibili.status !== 'ACTIVE'"
            type="button"
            class="h-10 shrink-0 rounded-full bg-tomato px-3 text-[12px] text-paper transition hover:bg-tomato-deep"
            @click="startBilibiliQr"
          >
            重新扫码
          </button>
          <button
            type="button"
            class="h-10 shrink-0 rounded-full border border-line px-3 text-[12px] text-ink-soft transition hover:border-tomato/40 hover:text-tomato-deep"
            :disabled="busy === 'disconnect'"
            @click="disconnectOpen = true"
          >
            断开
          </button>
        </div>

        <div v-else class="mt-3 flex flex-col gap-3 rounded-xl bg-paper/60 p-3.5 sm:flex-row sm:items-center">
          <div class="min-w-0 flex-1">
            <p class="text-[13px] font-medium text-ink">扫码登录自己的 B 站</p>
            <p class="mt-1 text-[11.5px] leading-relaxed text-ink-muted">
              不保存账号密码，只把登录 Cookie 加密存在本机后端；Cookie 过期后重新扫码即可。
            </p>
          </div>
          <button
            type="button"
            class="inline-flex h-11 shrink-0 items-center justify-center gap-1.5 rounded-full bg-tomato px-4 text-[13px] font-medium text-paper transition hover:bg-tomato-deep"
            @click="startBilibiliQr"
          >
            <AppIcon name="link" :size="16" />
            连接 B 站
          </button>
        </div>

        <p class="mt-2.5 text-[10.5px] leading-relaxed text-ink-muted">
          安全提示：Cookie 使用 AES-256-GCM 加密保存，任何接口都不会返回；扫码登录不会接触你的密码。
        </p>
      </section>

      <!-- 收藏夹 -->
      <section class="animate-rise rounded-card border border-line bg-card p-4 shadow-paper md:p-5">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-herb" aria-hidden="true" />
          <h2 class="font-display text-[18px] text-ink">B 站收藏夹</h2>
          <button
            v-if="bilibili?.status === 'ACTIVE'"
            type="button"
            class="ml-auto inline-flex h-10 items-center gap-1.5 rounded-full border border-line px-3 text-[12px] text-ink-soft transition hover:border-herb/40 hover:text-herb"
            :disabled="busy === 'scan-all'"
            @click="scanAll"
          >
            <AppIcon name="refresh" :size="14" :class="busy === 'scan-all' ? 'animate-spin' : ''" />
            全部扫描
          </button>
        </div>

        <div v-if="bilibili?.status === 'ACTIVE'" class="mt-3 flex flex-col gap-2 sm:flex-row">
          <input
            v-model="folderUrl"
            type="url"
            placeholder="粘贴收藏夹链接，例如 https://space.bilibili.com/xxx/favlist?fid=123456"
            class="h-12 min-w-0 flex-1 rounded-xl border border-line bg-paper/55 px-3 text-[16px] text-ink outline-none transition focus:border-herb/50 focus:bg-card focus:ring-2 focus:ring-herb/10 md:text-[13px]"
          />
          <button
            type="button"
            class="inline-flex h-12 shrink-0 items-center justify-center gap-1.5 rounded-xl bg-ink px-4 text-[13px] font-medium text-paper transition hover:bg-ink-soft disabled:opacity-50"
            :disabled="busy === 'folder' || !folderUrl.trim()"
            @click="addFolder"
          >
            <AppIcon name="plus" :size="15" />
            添加
          </button>
        </div>

        <div class="mt-3 space-y-2.5">
          <div
            v-for="source in bilibiliSources"
            :key="source.id"
            class="rounded-xl border border-line/80 bg-paper/45 p-3"
          >
            <div class="flex items-start gap-2">
              <div class="min-w-0 flex-1">
                <p class="truncate text-[13.5px] font-medium text-ink">
                  {{ source.folderName || 'B站收藏夹' }}
                </p>
                <p class="mt-0.5 truncate font-mono text-[10.5px] text-ink-muted">
                  fid={{ source.folderId }} · 每轮最多 {{ source.scanLimit }} 条
                </p>
              </div>
              <span
                class="shrink-0 rounded-full px-2 py-0.5 text-[10.5px]"
                :class="
                  source.lastScanStatus === 'RUNNING'
                    ? 'bg-ginger-soft text-[#8a6215]'
                    : source.lastScanStatus === 'FAILED'
                      ? 'bg-tomato-soft text-tomato-deep'
                      : 'bg-herb-soft text-herb'
                "
              >
                {{ scanStatusLabel(source.lastScanStatus) }}
              </span>
            </div>

            <p class="mt-1.5 text-[11px] leading-relaxed text-ink-muted">
              {{ source.lastScanMessage || '每天 06:00 自动扫描；也可以点「立即扫描」先跑一次。' }}
              <span v-if="source.lastScanAt"> · {{ formatTime(source.lastScanAt) }}</span>
            </p>

            <div class="mt-2 flex flex-wrap items-center gap-1.5">
              <button
                type="button"
                class="inline-flex h-9 items-center gap-1 rounded-full border border-line px-2.5 text-[11px] text-ink-soft transition hover:border-tomato/40 hover:text-tomato-deep disabled:opacity-50"
                :disabled="sourceBusy === source.id || source.lastScanStatus === 'RUNNING'"
                @click="scanSource(source)"
              >
                <AppIcon name="refresh" :size="13" />
                {{ source.lastScanStatus === 'RUNNING' ? '扫描中' : '立即扫描' }}
              </button>
              <button
                type="button"
                role="switch"
                :aria-checked="source.autoScan"
                class="inline-flex h-9 items-center gap-1.5 rounded-full border px-2.5 text-[11px] transition"
                :class="
                  source.autoScan
                    ? 'border-herb/30 bg-herb-soft text-herb'
                    : 'border-line text-ink-muted'
                "
                :disabled="sourceBusy === source.id"
                @click="toggleAuto(source)"
              >
                <span
                  class="h-2 w-2 rounded-full"
                  :class="source.autoScan ? 'bg-herb' : 'bg-line-strong'"
                />
                自动扫描{{ source.autoScan ? '开' : '关' }}
              </button>
              <button
                type="button"
                class="ml-auto inline-flex h-9 items-center gap-1 rounded-full px-2 text-[11px] text-ink-muted transition hover:text-tomato-deep"
                @click="deleteTarget = source"
              >
                <AppIcon name="trash" :size="13" />
                删除
              </button>
            </div>
          </div>

          <p
            v-if="bilibiliSources.length === 0"
            class="rounded-xl border border-dashed border-line-strong/70 px-3 py-4 text-center text-[11.5px] text-ink-muted"
          >
            还没有收藏夹。先连接 B 站，把收藏夹链接贴到上面。
          </p>
        </div>
      </section>

      <!-- 抖音队列 -->
      <section class="animate-rise rounded-card border border-line bg-card p-4 shadow-paper md:p-5">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-ginger" aria-hidden="true" />
          <h2 class="font-display text-[18px] text-ink">抖音分享链接队列</h2>
          <span class="ml-auto text-[10.5px] text-ink-muted">每天 06:00 自动处理</span>
        </div>
        <p class="mt-1.5 text-[11.5px] leading-relaxed text-ink-muted">
          抖音收藏夹接口有签名风控，暂不做自动抓取。刷到菜谱视频时复制分享链接丢进来，系统每天自动识别并生成菜谱。
        </p>
        <textarea
          v-model="queueText"
          rows="3"
          placeholder="粘贴抖音分享文案或链接，支持一次多行，例如：&#10;7.85 复制打开抖音，看看【…】 https://v.douyin.com/xxxx/"
          class="mt-3 w-full resize-y rounded-xl border border-line bg-paper/55 p-3 text-[16px] leading-relaxed text-ink outline-none transition focus:border-ginger/60 focus:bg-card focus:ring-2 focus:ring-ginger/10 md:text-[13px]"
        />
        <div class="mt-2 flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="inline-flex h-11 items-center gap-1.5 rounded-full bg-tomato px-4 text-[13px] font-medium text-paper transition hover:bg-tomato-deep disabled:opacity-50"
            :disabled="busy === 'queue' || !queueText.trim()"
            @click="enqueue"
          >
            <AppIcon name="plus" :size="15" />
            加入队列
          </button>
          <button
            v-if="queueSource"
            type="button"
            class="inline-flex h-11 items-center gap-1.5 rounded-full border border-line px-4 text-[12.5px] text-ink-soft transition hover:border-tomato/40 hover:text-tomato-deep disabled:opacity-50"
            :disabled="sourceBusy === queueSource.id || queueSource.lastScanStatus === 'RUNNING'"
            @click="scanSource(queueSource)"
          >
            <AppIcon name="refresh" :size="14" :class="queueSource.lastScanStatus === 'RUNNING' ? 'animate-spin' : ''" />
            {{ queueSource.lastScanStatus === 'RUNNING' ? '处理中' : '立即处理队列' }}
          </button>
          <span class="text-[11px] text-ink-muted">
            待处理 {{ queueStats.pending }} · 已生成 {{ queueStats.imported }} · 跳过
            {{ queueStats.skipped }} · 失败 {{ queueStats.failed }}
          </span>
        </div>
        <p v-if="queueSource?.lastScanMessage" class="mt-1.5 text-[10.5px] text-ink-muted">
          {{ queueSource.lastScanMessage }}
          <span v-if="queueSource.lastScanAt"> · {{ formatTime(queueSource.lastScanAt) }}</span>
        </p>
      </section>

      <!-- 处理记录 -->
      <section class="animate-rise rounded-card border border-line bg-card p-4 shadow-paper md:p-5">
        <div class="flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-tomato" aria-hidden="true" />
          <h2 class="font-display text-[18px] text-ink">处理记录</h2>
          <span class="ml-auto font-mono text-[10.5px] text-ink-muted">{{ itemTotal }} 条</span>
        </div>

        <div class="mt-2 flex flex-wrap gap-1.5">
          <button
            v-for="filter in FILTERS"
            :key="filter.value"
            type="button"
            class="h-8 rounded-full px-3 text-[11px] transition"
            :class="
              itemFilter === filter.value
                ? 'bg-ink text-paper'
                : 'border border-line text-ink-muted hover:text-ink'
            "
            @click="changeFilter(filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>

        <div v-if="itemLoading" class="mt-3 space-y-2">
          <div v-for="i in 3" :key="i" class="h-14 animate-pulse rounded-xl bg-paper-deep" />
        </div>
        <div v-else-if="items.length === 0" class="mt-3 rounded-xl dashed-panel px-3 py-5 text-center text-[11.5px] text-ink-muted">
          还没有处理记录
        </div>
        <ul v-else class="mt-3 space-y-2">
          <li
            v-for="item in items"
            :key="item.id"
            class="flex items-start gap-2.5 rounded-xl border border-line/70 bg-paper/40 p-2.5"
          >
            <img
              v-if="item.coverUrl"
              :src="item.coverUrl"
              alt=""
              class="h-12 w-16 shrink-0 rounded-lg border border-line object-cover"
              referrerpolicy="no-referrer"
              loading="lazy"
            />
            <div class="min-w-0 flex-1">
              <div class="flex items-start gap-1.5">
                <p class="line-clamp-1 min-w-0 flex-1 text-[12.5px] font-medium text-ink">
                  {{ item.title || '（未获取到标题）' }}
                </p>
                <span
                  class="shrink-0 rounded-full px-1.5 py-0.5 text-[10px]"
                  :class="itemStatusClass(item.status)"
                >
                  {{ itemStatusLabel(item.status) }}
                </span>
              </div>
              <p class="mt-0.5 line-clamp-1 text-[10.5px] text-ink-muted">
                {{ item.author || item.platform }} · {{ formatTime(item.processedAt || item.createdAt) }}
                <span v-if="item.reason"> · {{ item.reason }}</span>
              </p>
              <RouterLink
                v-if="item.recipeId"
                :to="`/recipes/${item.recipeId}`"
                class="mt-0.5 inline-flex items-center gap-0.5 text-[10.5px] text-tomato-deep hover:underline"
              >
                查看菜谱
                <AppIcon name="chevron-right" :size="11" />
              </RouterLink>
            </div>
          </li>
        </ul>
      </section>
    </template>

    <!-- 二维码登录 -->
    <ModalSheet v-model="qrOpen" title="扫码登录 B 站" subtitle="登录后只保存加密 Cookie" @close="stopQr">
      <div class="flex flex-col items-center gap-3 py-2">
        <div class="grid h-56 w-56 place-items-center rounded-2xl border border-line bg-white p-3">
          <img v-if="qrImage" :src="qrImage" alt="B站登录二维码" class="h-full w-full" />
          <div v-else class="h-full w-full animate-pulse rounded-xl bg-paper-deep" />
        </div>
        <p class="text-center text-[13px] font-medium text-ink">{{ qrMessage }}</p>
        <p class="text-center text-[11px] leading-relaxed text-ink-muted">
          打开 B 站手机 App → 扫一扫 → 确认登录。二维码 5 分钟内有效。
        </p>
        <button
          v-if="qrStatus === 'EXPIRED' || qrStatus === 'FAILED'"
          type="button"
          class="h-11 rounded-full bg-tomato px-5 text-[13px] text-paper"
          @click="startBilibiliQr"
        >
          重新生成二维码
        </button>
      </div>
    </ModalSheet>

    <ConfirmDialog
      v-model="disconnectOpen"
      title="断开 B 站连接？"
      message="断开后收藏夹不再自动扫描，已生成的菜谱会保留。"
      confirm-text="断开"
      tone="danger"
      :loading="busy === 'disconnect'"
      @confirm="disconnectBilibili"
    />

    <ConfirmDialog
      :model-value="!!deleteTarget"
      title="删除这个收藏源？"
      :message="`「${deleteTarget?.folderName || '收藏夹'}」将不再自动扫描，已生成的菜谱会保留。`"
      confirm-text="删除"
      tone="danger"
      :loading="deleteBusy"
      @update:model-value="(value) => { if (!value) deleteTarget = null }"
      @confirm="confirmDelete"
    />
  </div>
</template>
