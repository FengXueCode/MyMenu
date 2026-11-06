<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { aiApi } from '@/api/ai'
import AppIcon from '@/components/AppIcon.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
import ModalSheet from '@/components/ModalSheet.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import { useToastStore } from '@/stores/toast'
import type { AiProvider, AiProviderInput, ProviderTestResult, ProviderType } from '@/types/api'

const toast = useToastStore()

/** 厂商预设：选中后自动填 baseUrl 与模型名 */
const PROVIDER_PRESETS: Record<
  ProviderType,
  { label: string; baseUrl: string; chatModel: string; imageModel: string; hint: string }
> = {
  openai: {
    label: 'OpenAI',
    baseUrl: 'https://api.openai.com/v1',
    chatModel: 'gpt-4o-mini',
    imageModel: 'gpt-image-1',
    hint: '官方接口，需要海外网络环境',
  },
  deepseek: {
    label: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com/v1',
    chatModel: 'deepseek-chat',
    imageModel: '',
    hint: '中文理解好，价格便宜，暂无图像模型',
  },
  qwen: {
    label: '通义千问',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    chatModel: 'qwen-plus',
    imageModel: 'wanx2.1-t2i-turbo',
    hint: 'DashScope 兼容模式',
  },
  moonshot: {
    label: 'Moonshot Kimi',
    baseUrl: 'https://api.moonshot.cn/v1',
    chatModel: 'moonshot-v1-8k',
    imageModel: '',
    hint: '长文本友好',
  },
  siliconflow: {
    label: '硅基流动',
    baseUrl: 'https://api.siliconflow.cn/v1',
    chatModel: 'Qwen/Qwen2.5-7B-Instruct',
    imageModel: 'Kwai-Kolors/Kolors',
    hint: '国内多模型聚合，含图像模型',
  },
  opencode: {
    label: 'OpenCode Zen',
    baseUrl: 'https://opencode.ai/inference/openai/v1',
    chatModel: 'deepseek-v4.1-flash',
    imageModel: '',
    hint: 'OpenAI 兼容。Console 里的 Free 只代表 OpenCode 客户端可用，外部 API 可能 403/429；若你的 Key 实测可用，可直接把对话模型改成 big-pickle 等免费模型。',
  },
  ollama: {
    label: 'Ollama（本地）',
    baseUrl: 'http://localhost:11434/v1',
    chatModel: 'qwen2.5:7b',
    imageModel: '',
    hint: '本机运行、免费且数据不出本机，通常无需 API Key；适合先做功能测试',
  },
  custom: {
    label: '自定义（OpenAI 兼容）',
    baseUrl: '',
    chatModel: '',
    imageModel: '',
    hint: '任何兼容 /chat/completions 的服务',
  },
}

const PROVIDER_TYPES = Object.keys(PROVIDER_PRESETS) as ProviderType[]

const providers = ref<AiProvider[]>([])
const loading = ref(true)
const loadError = ref(false)

const sheetOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formError = ref('')
const showKey = ref(false)

const testingId = ref<number | null>(null)
const testResults = reactive<Record<number, ProviderTestResult>>({})

const deleteTarget = ref<AiProvider | null>(null)
const deleteOpen = ref(false)
const deleting = ref(false)

interface ProviderForm {
  name: string
  providerType: ProviderType
  baseUrl: string
  chatModel: string
  imageModel: string
  /** 只写不读：编辑时永远为空，留空表示不修改 */
  apiKey: string
  enabled: boolean
  isDefault: boolean
}

const form = reactive<ProviderForm>(createBlankForm())

const isEditing = computed(() => editingId.value !== null)
const editingProvider = computed(
  () => providers.value.find((item) => item.id === editingId.value) ?? null,
)

const keyPlaceholder = computed(() => {
  const current = editingProvider.value
  if (current?.hasKey) {
    return `已保存 ${current.keyMasked ?? '****'}，留空表示不修改`
  }
  return 'sk-…（只写入后端，不会回显）'
})

function createBlankForm(): ProviderForm {
  return {
    name: '',
    providerType: 'deepseek',
    baseUrl: PROVIDER_PRESETS.deepseek.baseUrl,
    chatModel: PROVIDER_PRESETS.deepseek.chatModel,
    imageModel: PROVIDER_PRESETS.deepseek.imageModel,
    apiKey: '',
    enabled: true,
    isDefault: true,
  }
}

/* --------------------------------- 加载 ---------------------------------- */

async function load(): Promise<void> {
  loading.value = true
  loadError.value = false
  try {
    providers.value = await aiApi.providers()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/* -------------------------------- 表单操作 -------------------------------- */

function applyPreset(type: ProviderType): void {
  form.providerType = type
  const preset = PROVIDER_PRESETS[type]
  if (type !== 'custom') {
    form.baseUrl = preset.baseUrl
    form.chatModel = preset.chatModel
    form.imageModel = preset.imageModel
    if (!form.name.trim() || PROVIDER_TYPES.some((item) => PROVIDER_PRESETS[item].label === form.name)) {
      form.name = preset.label
    }
  }
}

function openCreate(): void {
  editingId.value = null
  applyPreset('deepseek')
  form.name = PROVIDER_PRESETS.deepseek.label
  form.apiKey = ''
  form.enabled = true
  form.isDefault = providers.value.length === 0
  showKey.value = false
  formError.value = ''
  sheetOpen.value = true
}

function openEdit(provider: AiProvider): void {
  editingId.value = provider.id
  form.name = provider.name
  form.providerType = provider.providerType
  form.baseUrl = provider.baseUrl
  form.chatModel = provider.chatModel
  form.imageModel = provider.imageModel ?? ''
  // 安全：Key 永远不从服务端回填，输入框只写不读
  form.apiKey = ''
  form.enabled = provider.enabled
  form.isDefault = provider.isDefault
  showKey.value = false
  formError.value = ''
  sheetOpen.value = true
}

function closeSheet(): void {
  sheetOpen.value = false
  // 立即清空内存中的 Key 输入，避免残留在组件状态里
  form.apiKey = ''
  formError.value = ''
  editingId.value = null
}

function buildInput(): AiProviderInput | null {
  const name = form.name.trim()
  const baseUrl = form.baseUrl.trim().replace(/\/+$/, '')
  const chatModel = form.chatModel.trim()
  const imageModel = form.imageModel.trim()
  const apiKey = form.apiKey.trim()

  if (!name) {
    formError.value = '给这套配置起个名字'
    return null
  }
  if (!/^https?:\/\//i.test(baseUrl)) {
    formError.value = '接口地址需要以 http:// 或 https:// 开头'
    return null
  }
  if (!chatModel) {
    formError.value = '对话模型名不能为空'
    return null
  }
  if (!isEditing.value && !apiKey && form.providerType !== 'ollama' && form.providerType !== 'custom') {
    formError.value = '请填写 API Key（本地 Ollama 或自定义服务可留空）'
    return null
  }

  const input: AiProviderInput = {
    name,
    providerType: form.providerType,
    baseUrl,
    chatModel,
    imageModel: imageModel || null,
    enabled: form.enabled,
    isDefault: form.isDefault,
  }
  // 留空 = 不修改已存 Key；不把空字符串发给后端
  if (apiKey) input.apiKey = apiKey
  return input
}

async function submit(): Promise<void> {
  formError.value = ''
  const input = buildInput()
  if (!input || saving.value) return

  saving.value = true
  try {
    if (editingId.value !== null) {
      await aiApi.updateProvider(editingId.value, input)
      toast.success('配置已更新')
    } else {
      await aiApi.createProvider(input)
      toast.success('配置已保存，可以点「测试连接」验证一下')
    }
    closeSheet()
    await load()
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
    // 无论成功失败都不保留明文 Key
    form.apiKey = ''
  }
}

/* -------------------------------- 列表操作 -------------------------------- */

async function testProvider(provider: AiProvider): Promise<void> {
  if (testingId.value !== null) return
  testingId.value = provider.id
  try {
    const result = await aiApi.testProvider(provider.id)
    testResults[provider.id] = result
    if (result.ok) {
      toast.success(`连接正常 · ${result.latencyMs} ms`)
    } else {
      toast.error(result.message || '连接失败，检查地址与 Key')
    }
    providers.value = providers.value.map((item) =>
      item.id === provider.id ? { ...item, lastTestOk: result.ok } : item,
    )
  } finally {
    testingId.value = null
  }
}

async function setDefault(provider: AiProvider): Promise<void> {
  if (provider.isDefault) return
  try {
    await aiApi.updateProvider(provider.id, {
      name: provider.name,
      providerType: provider.providerType,
      baseUrl: provider.baseUrl,
      chatModel: provider.chatModel,
      imageModel: provider.imageModel,
      enabled: provider.enabled,
      isDefault: true,
    })
    toast.success(`「${provider.name}」已设为默认`)
    await load()
  } catch {
    /* 错误已由 http 层提示 */
  }
}

function askDelete(provider: AiProvider): void {
  deleteTarget.value = provider
  deleteOpen.value = true
}

async function confirmDelete(): Promise<void> {
  const target = deleteTarget.value
  if (!target || deleting.value) return
  deleting.value = true
  try {
    await aiApi.deleteProvider(target.id)
    deleteOpen.value = false
    deleteTarget.value = null
    toast.success('配置已删除')
    await load()
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="mx-auto w-full max-w-2xl space-y-4">
    <header class="animate-rise">
      <p class="font-mono text-[10.5px] tracking-[0.28em] text-ink-muted">AI SETTINGS</p>
      <div class="mt-1 flex items-end justify-between gap-3">
        <div class="min-w-0">
          <h1 class="font-display text-[26px] leading-tight text-ink md:text-[30px]">AI 配置</h1>
          <p class="mt-1 text-[12.5px] leading-relaxed text-ink-muted">
            可以同时配置多套模型，选一套作为默认。Key 只保存到你的后端。
          </p>
        </div>
        <button
          type="button"
          class="inline-flex h-11 shrink-0 items-center gap-1.5 rounded-full bg-tomato px-4 text-[13px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none"
          @click="openCreate"
        >
          <AppIcon name="plus" :size="15" />
          新增
        </button>
      </div>
    </header>

    <!-- 安全提示 -->
    <div class="animate-fade rounded-card border border-herb/30 bg-herb-soft/40 px-3.5 py-2.5">
      <p class="flex items-start gap-2 text-[11.5px] leading-relaxed text-herb">
        <AppIcon name="info" :size="13" class="mt-0.5 shrink-0" />
        <span>
          后端使用 AES-256-GCM 加密保存 Key，接口只返回
          <span class="font-mono">hasKey</span> 与掩码；本页面不会显示完整 Key，也不把 Key
          写进浏览器存储。
        </span>
      </p>
    </div>

    <!-- 加载 -->
    <div v-if="loading" class="space-y-3">
      <div
        v-for="i in 2"
        :key="i"
        class="rounded-card border border-line bg-card p-4 shadow-paper"
      >
        <SkeletonCard variant="line" :lines="3" />
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="rounded-card border border-line bg-card p-6 text-center">
      <p class="font-display text-[17px] text-ink">配置没读出来</p>
      <p class="mt-1 text-[12.5px] text-ink-muted">检查后端服务后重试。</p>
      <button
        type="button"
        class="mt-4 inline-flex h-11 items-center gap-1.5 rounded-full bg-ink px-5 text-[13px] text-paper"
        @click="load"
      >
        <AppIcon name="refresh" :size="14" />
        重试
      </button>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!providers.length"
      icon="sparkle"
      title="还没有配置 AI"
      description="添加一套 OpenAI 兼容的服务商，就能用 AI 提取菜谱、生成摘要和配图。"
      action-label="添加第一套配置"
      @action="openCreate"
    />

    <!-- 配置列表 -->
    <div v-else class="space-y-3">
      <article
        v-for="provider in providers"
        :key="provider.id"
        class="animate-rise rounded-card border bg-card p-4 shadow-paper transition"
        :class="provider.isDefault ? 'border-ginger/60' : 'border-line'"
      >
        <div class="flex items-start gap-3">
          <span
            class="grid h-10 w-10 shrink-0 place-items-center rounded-full"
            :class="provider.isDefault ? 'bg-ginger-soft text-[#8a6215]' : 'bg-paper-deep text-ink-soft'"
          >
            <AppIcon name="sparkle" :size="18" />
          </span>

          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-1.5">
              <h2 class="font-display text-[17px] leading-tight text-ink">{{ provider.name }}</h2>
              <span
                v-if="provider.isDefault"
                class="-rotate-2 rounded-[4px] border border-ginger/60 bg-ginger-soft px-1.5 py-0.5 font-display text-[10px] tracking-[0.12em] text-[#7d5a12]"
              >
                默认
              </span>
              <span
                class="rounded-full px-2 py-0.5 text-[10.5px]"
                :class="
                  provider.enabled
                    ? 'bg-herb-soft text-herb'
                    : 'bg-paper-deep text-ink-muted'
                "
              >
                {{ provider.enabled ? '已启用' : '已停用' }}
              </span>
              <span
                v-if="provider.lastTestOk === true"
                class="inline-flex items-center gap-1 rounded-full bg-herb-soft px-2 py-0.5 text-[10.5px] text-herb"
              >
                <span class="h-1.5 w-1.5 rounded-full bg-herb" /> 上次连通
              </span>
              <span
                v-else-if="provider.lastTestOk === false"
                class="inline-flex items-center gap-1 rounded-full bg-tomato-soft px-2 py-0.5 text-[10.5px] text-tomato-deep"
              >
                <span class="h-1.5 w-1.5 rounded-full bg-tomato" /> 上次失败
              </span>
            </div>

            <p class="mt-0.5 font-mono text-[10.5px] uppercase tracking-wide text-ink-muted">
              {{ PROVIDER_PRESETS[provider.providerType]?.label ?? provider.providerType }}
            </p>

            <dl class="mt-2 grid gap-1 text-[11.5px]">
              <div class="flex items-baseline gap-1.5">
                <dt class="w-14 shrink-0 text-ink-muted">接口</dt>
                <dd class="min-w-0 truncate font-mono text-ink-soft">{{ provider.baseUrl }}</dd>
              </div>
              <div class="flex items-baseline gap-1.5">
                <dt class="w-14 shrink-0 text-ink-muted">对话</dt>
                <dd class="min-w-0 truncate font-mono text-ink-soft">{{ provider.chatModel }}</dd>
              </div>
              <div class="flex items-baseline gap-1.5">
                <dt class="w-14 shrink-0 text-ink-muted">图像</dt>
                <dd class="min-w-0 truncate font-mono text-ink-soft">
                  {{ provider.imageModel || '未配置（用兜底封面）' }}
                </dd>
              </div>
              <div class="flex items-baseline gap-1.5">
                <dt class="w-14 shrink-0 text-ink-muted">Key</dt>
                <dd class="min-w-0 truncate font-mono text-ink-soft">
                  <template v-if="provider.hasKey">
                    {{ provider.keyMasked || '已保存' }}
                  </template>
                  <span v-else class="text-tomato-deep">未配置</span>
                </dd>
              </div>
            </dl>

            <!-- 测试结果 -->
            <p
              v-if="testResults[provider.id]"
              class="mt-2 flex items-start gap-1.5 rounded-lg px-2.5 py-1.5 text-[11px] leading-relaxed"
              :class="
                testResults[provider.id]?.ok
                  ? 'bg-herb-soft/70 text-herb'
                  : 'bg-tomato-soft/70 text-tomato-deep'
              "
            >
              <AppIcon
                :name="testResults[provider.id]?.ok ? 'check' : 'alert'"
                :size="12"
                class="mt-0.5 shrink-0"
              />
              {{ testResults[provider.id]?.message }}
              <span v-if="testResults[provider.id]?.latencyMs != null" class="font-mono">
                · {{ testResults[provider.id]?.latencyMs }} ms
              </span>
            </p>
          </div>
        </div>

        <!-- 操作 -->
        <div class="mt-3 flex flex-wrap items-center gap-1.5 border-t border-line/70 pt-3">
          <button
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-paper px-3 text-[11.5px] text-ink-soft transition hover:border-line-strong hover:text-ink disabled:opacity-60"
            :disabled="testingId !== null"
            @click="testProvider(provider)"
          >
            <AppIcon name="link" :size="13" :class="testingId === provider.id ? 'animate-pulse' : ''" />
            {{ testingId === provider.id ? '测试中…' : '测试连接' }}
          </button>
          <button
            v-if="!provider.isDefault"
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-paper px-3 text-[11.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
            @click="setDefault(provider)"
          >
            <AppIcon name="check" :size="13" />
            设为默认
          </button>
          <button
            type="button"
            class="inline-flex h-11 items-center gap-1 rounded-full border border-line bg-paper px-3 text-[11.5px] text-ink-soft transition hover:border-line-strong hover:text-ink"
            @click="openEdit(provider)"
          >
            <AppIcon name="edit" :size="13" />
            编辑
          </button>
          <button
            type="button"
            class="ml-auto inline-flex h-11 items-center gap-1 rounded-full px-3 text-[11.5px] text-ink-muted transition hover:bg-tomato-soft hover:text-tomato-deep"
            :aria-label="`删除 ${provider.name}`"
            @click="askDelete(provider)"
          >
            <AppIcon name="trash" :size="13" />
            删除
          </button>
        </div>
      </article>
    </div>

    <!-- 新增 / 编辑弹层 -->
    <ModalSheet
      v-model="sheetOpen"
      :title="isEditing ? '编辑 AI 配置' : '新增 AI 配置'"
      :subtitle="isEditing ? 'API Key 留空表示不修改' : '选择厂商预设后会自动填入地址与模型名'"
      size="lg"
      @close="closeSheet"
    >
      <form class="space-y-3" @submit.prevent="submit">
        <div>
          <label for="provider-type" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
            服务商
          </label>
          <select
            id="provider-type"
            :value="form.providerType"
            class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 text-[16px] text-ink focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
            @change="applyPreset(($event.target as HTMLSelectElement).value as ProviderType)"
          >
            <option v-for="type in PROVIDER_TYPES" :key="type" :value="type">
              {{ PROVIDER_PRESETS[type].label }}
            </option>
          </select>
          <p class="mt-1 text-[11px] text-ink-muted">
            {{ PROVIDER_PRESETS[form.providerType].hint }}
          </p>
        </div>

        <div>
          <label for="provider-name" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
            配置名称
          </label>
          <input
            id="provider-name"
            v-model="form.name"
            type="text"
            maxlength="30"
            placeholder="例如：DeepSeek 主力"
            class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
          />
        </div>

        <div>
          <label for="provider-base" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
            接口地址（OpenAI 兼容，通常以 /v1 结尾）
          </label>
          <input
            id="provider-base"
            v-model="form.baseUrl"
            type="url"
            spellcheck="false"
            placeholder="https://api.example.com/v1"
            class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 font-mono text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
          />
        </div>

        <div class="grid gap-3 sm:grid-cols-2">
          <div>
            <label for="provider-chat" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
              对话模型
            </label>
            <input
              id="provider-chat"
              v-model="form.chatModel"
              type="text"
              spellcheck="false"
              placeholder="deepseek-chat"
              class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 font-mono text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
            />
          </div>
          <div>
            <label for="provider-image" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
              图像模型（可选）
            </label>
            <input
              id="provider-image"
              v-model="form.imageModel"
              type="text"
              spellcheck="false"
              placeholder="留空则用兜底封面"
              class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 font-mono text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
            />
          </div>
        </div>

        <div>
          <label for="provider-key" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
            API Key
            <span class="ml-1 font-normal text-ink-muted">（只写不读）</span>
          </label>
          <div class="relative">
            <input
              id="provider-key"
              v-model="form.apiKey"
              :type="showKey ? 'text' : 'password'"
              autocomplete="new-password"
              spellcheck="false"
              :placeholder="keyPlaceholder"
              class="h-12 w-full rounded-xl border border-line bg-paper/55 px-3 pr-12 font-mono text-[16px] text-ink placeholder:text-ink-muted/60 focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
            />
            <button
              type="button"
              class="absolute right-1 top-1 grid h-11 w-11 place-items-center rounded-lg text-ink-muted transition hover:bg-paper-deep hover:text-ink"
              :aria-label="showKey ? '隐藏 Key' : '显示 Key'"
              @click="showKey = !showKey"
            >
              <AppIcon name="eye" :size="17" />
            </button>
          </div>
          <p class="mt-1 text-[11px] text-ink-muted">
            保存后只显示掩码，任何页面与接口都不会回传明文。
          </p>
        </div>

        <div class="grid gap-2 sm:grid-cols-2">
          <label
            class="flex min-h-[44px] cursor-pointer items-center justify-between rounded-xl border border-line bg-paper/50 px-3.5 py-2.5"
          >
            <span class="text-[13px] text-ink-soft">启用这套配置</span>
            <span
              class="relative h-6 w-11 rounded-full transition-colors"
              :class="form.enabled ? 'bg-herb' : 'bg-line-strong'"
            >
              <span
                class="absolute top-0.5 h-5 w-5 rounded-full bg-card shadow transition-transform"
                :class="form.enabled ? 'translate-x-[1.4rem]' : 'translate-x-0.5'"
              />
            </span>
            <input v-model="form.enabled" type="checkbox" class="sr-only" />
          </label>

          <label
            class="flex min-h-[44px] cursor-pointer items-center justify-between rounded-xl border border-line bg-paper/50 px-3.5 py-2.5"
          >
            <span class="text-[13px] text-ink-soft">设为默认</span>
            <span
              class="relative h-6 w-11 rounded-full transition-colors"
              :class="form.isDefault ? 'bg-ginger' : 'bg-line-strong'"
            >
              <span
                class="absolute top-0.5 h-5 w-5 rounded-full bg-card shadow transition-transform"
                :class="form.isDefault ? 'translate-x-[1.4rem]' : 'translate-x-0.5'"
              />
            </span>
            <input v-model="form.isDefault" type="checkbox" class="sr-only" />
          </label>
        </div>

        <p
          v-if="formError"
          class="flex items-start gap-1.5 rounded-xl bg-tomato-soft px-3 py-2 text-[12px] text-tomato-deep"
          role="alert"
        >
          <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
          {{ formError }}
        </p>

        <div class="flex gap-2.5 pt-1">
          <button
            type="button"
            class="h-12 flex-1 rounded-full border border-line bg-card text-[13.5px] text-ink-soft"
            @click="closeSheet"
          >
            取消
          </button>
          <button
            type="submit"
            class="h-12 flex-[1.4] rounded-full bg-tomato text-[13.5px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.5)] transition active:translate-y-px active:shadow-none disabled:opacity-60"
            :disabled="saving"
          >
            {{ saving ? '保存中…' : isEditing ? '保存修改' : '保存配置' }}
          </button>
        </div>
      </form>
    </ModalSheet>

    <ConfirmDialog
      v-model="deleteOpen"
      title="删除这套 AI 配置？"
      :message="`「${deleteTarget?.name ?? ''}」删除后无法恢复，正在使用它的功能会失效。`"
      confirm-text="删除"
      tone="danger"
      :loading="deleting"
      @confirm="confirmDelete"
    />
  </div>
</template>
