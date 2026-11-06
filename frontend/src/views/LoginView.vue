<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { ApiError } from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const submitting = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

const USERNAME_RE = /^[A-Za-z0-9_]{3,20}$/
const isRegister = computed(() => mode.value === 'register')

const canSubmit = computed(
  () =>
    !submitting.value &&
    USERNAME_RE.test(form.username.trim()) &&
    form.password.length >= 6 &&
    form.password.length <= 32,
)

function switchMode(next: Mode): void {
  if (mode.value === next) return
  mode.value = next
  errorMessage.value = ''
}

function fillDemo(): void {
  mode.value = 'login'
  form.username = 'demo'
  form.password = '123456'
  errorMessage.value = ''
}

function redirectTarget(): string {
  const redirect = route.query.redirect
  const value = typeof redirect === 'string' ? redirect : ''
  // 只允许站内相对路径，避免开放重定向
  return value.startsWith('/') && !value.startsWith('//') ? value : '/'
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  if (!USERNAME_RE.test(form.username.trim())) {
    errorMessage.value = '用户名需为 3–20 位字母、数字或下划线'
    return
  }
  if (form.password.length < 6 || form.password.length > 32) {
    errorMessage.value = '密码长度需为 6–32 位'
    return
  }

  submitting.value = true
  try {
    if (isRegister.value) {
      await auth.register(
        {
          username: form.username.trim(),
          password: form.password,
          nickname: form.nickname.trim() || undefined,
        },
        true,
      )
      toast.success('注册成功，灶台已经为你热好了')
    } else {
      await auth.login({ username: form.username.trim(), password: form.password }, true)
      toast.success('欢迎回来，开火吧')
    }
    await router.replace(redirectTarget())
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : '操作失败，请检查后端服务后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="relative flex min-h-dvh w-full flex-col bg-paper lg:flex-row">
    <!-- 左：手绘装饰面板（桌面） -->
    <aside
      class="relative hidden w-[46%] shrink-0 flex-col justify-between overflow-hidden border-r border-line/80 bg-paper-deep/50 px-10 py-10 lg:flex xl:px-14"
    >
      <!-- 纸张纹理与暖色光晕 -->
      <div class="paper-grain pointer-events-none absolute inset-0 opacity-80" aria-hidden="true" />
      <div
        class="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-ginger/15 blur-2xl"
        aria-hidden="true"
      />
      <div
        class="pointer-events-none absolute -bottom-28 -right-16 h-80 w-80 rounded-full bg-tomato/10 blur-2xl"
        aria-hidden="true"
      />

      <div class="relative flex items-center gap-3">
        <span
          class="grid h-10 w-10 -rotate-6 place-items-center rounded-[11px] bg-tomato font-display text-xl leading-none text-paper shadow-[0_2px_0_rgba(176,59,41,0.9)]"
        >
          灶
        </span>
        <div class="flex flex-col leading-tight">
          <span class="font-display text-[20px] tracking-[0.16em] text-ink">灶台手账</span>
          <span class="font-mono text-[9px] tracking-[0.34em] text-ink-muted">STOVE NOTES</span>
        </div>
      </div>

      <!-- 手绘灶台插画 -->
      <div class="relative mx-auto w-full max-w-[22rem]">
        <svg viewBox="0 0 320 300" fill="none" class="w-full" aria-hidden="true">
          <!-- 背景圆 -->
          <circle cx="160" cy="150" r="118" stroke="#CDBC9E" stroke-dasharray="3 7" />
          <circle cx="160" cy="150" r="92" fill="#FFFCF4" stroke="#E4D8C3" />
          <!-- 太阳 -->
          <circle cx="248" cy="62" r="22" fill="#E7B24A" fill-opacity=".28" stroke="#E7B24A" />
          <path d="M248 28v-8M248 104v-8M282 62h8M206 62h8M272 38l6-6M218 92l6-6M272 86l6 6M218 32l6 6" stroke="#E7B24A" stroke-width="2" stroke-linecap="round" />
          <!-- 蒸汽 -->
          <path class="steam steam-1" d="M136 104c-8-10 8-14 0-26s6-16 0-24" stroke="#8D7C6C" stroke-width="3" stroke-linecap="round" opacity=".7" />
          <path class="steam steam-2" d="M160 100c-8-10 8-14 0-26s6-16 0-24" stroke="#8D7C6C" stroke-width="3" stroke-linecap="round" opacity=".55" />
          <path class="steam steam-3" d="M184 104c-8-10 8-14 0-26s6-16 0-24" stroke="#8D7C6C" stroke-width="3" stroke-linecap="round" opacity=".4" />
          <!-- 锅 -->
          <path d="M84 148h152c0 40-30 68-76 68s-76-28-76-68Z" fill="#D8503C" fill-opacity=".14" stroke="#D8503C" stroke-width="3.5" stroke-linejoin="round" />
          <path d="M84 148h152" stroke="#D8503C" stroke-width="3.5" stroke-linecap="round" />
          <path d="M84 156c-12 2-20 8-20 14s8 10 18 8M236 156c12 2 20 8 20 14s-8 10-18 8" stroke="#D8503C" stroke-width="3.5" stroke-linecap="round" />
          <path d="M112 216h96" stroke="#2A211C" stroke-width="3.5" stroke-linecap="round" />
          <path d="M124 216v14M196 216v14" stroke="#2A211C" stroke-width="3.5" stroke-linecap="round" />
          <!-- 砧板与菜刀 -->
          <rect x="44" y="238" width="96" height="26" rx="9" fill="#FFFCF4" stroke="#4C7A4F" stroke-width="2.5" />
          <path d="M56 246h30M56 255h44" stroke="#4C7A4F" stroke-width="2" stroke-linecap="round" opacity=".55" />
          <path d="M206 236h56a6 6 0 0 1 0 12h-44l-16-6 4-6Z" fill="#FAF6EE" stroke="#2A211C" stroke-width="2.5" stroke-linejoin="round" />
          <path d="M216 236v12" stroke="#2A211C" stroke-width="2" />
          <!-- 香料点缀 -->
          <circle cx="72" cy="118" r="4" fill="#E7B24A" />
          <circle cx="252" cy="150" r="5" fill="#4C7A4F" fill-opacity=".7" />
          <circle cx="64" cy="186" r="3" fill="#D8503C" fill-opacity=".8" />
        </svg>

        <!-- 手写便签 -->
        <div
          class="absolute -bottom-4 right-2 -rotate-3 rounded-[6px] border border-line-strong/70 bg-card px-3 py-1.5 font-display text-[12px] tracking-[0.16em] text-ink-soft shadow-paper"
        >
          今日 · 好好吃饭
        </div>
      </div>

      <div class="relative">
        <h1 class="font-display text-[34px] leading-[1.25] text-ink xl:text-[38px]">
          一餐一饭，<br />皆是手记。
        </h1>
        <p class="mt-3 max-w-[19rem] text-[13px] leading-relaxed text-ink-soft">
          把 B 站、抖音里收藏的做法，变成灶台边的一页页手账；按家人的口味，自动生成今日菜单。
        </p>
        <ul class="mt-5 space-y-2.5">
          <li class="flex items-center gap-2 text-[12.5px] text-ink-soft">
            <span class="grid h-6 w-6 place-items-center rounded-full bg-herb-soft text-herb">
              <AppIcon name="link" :size="13" />
            </span>
            粘贴分享链接，AI 自动整理成菜谱
          </li>
          <li class="flex items-center gap-2 text-[12.5px] text-ink-soft">
            <span class="grid h-6 w-6 place-items-center rounded-full bg-ginger-soft text-[#8a6215]">
              <AppIcon name="bowl" :size="13" />
            </span>
            按人数、荤素、辣度随机今日菜单
          </li>
          <li class="flex items-center gap-2 text-[12.5px] text-ink-soft">
            <span class="grid h-6 w-6 place-items-center rounded-full bg-tomato-soft text-tomato-deep">
              <AppIcon name="camera" :size="13" />
            </span>
            AI 摘要与配图，旧菜谱也能翻新
          </li>
        </ul>
      </div>

      <p class="relative font-mono text-[9.5px] tracking-[0.32em] text-ink-muted">
        MENU · STOVE NOTES · 2026
      </p>
    </aside>

    <!-- 右：登录 / 注册表单 -->
    <main
      class="relative flex flex-1 flex-col items-center justify-center px-4 pb-8 pt-[calc(2rem+env(safe-area-inset-top,0px))] sm:px-8"
    >
      <div class="pointer-events-none absolute inset-0 paper-grain opacity-60 lg:hidden" aria-hidden="true" />

      <!-- 移动端品牌头 -->
      <div class="relative mb-5 flex flex-col items-center gap-2 lg:hidden">
        <span
          class="grid h-12 w-12 -rotate-6 place-items-center rounded-[13px] bg-tomato font-display text-2xl leading-none text-paper shadow-[0_3px_0_rgba(176,59,41,0.9)]"
        >
          灶
        </span>
        <h1 class="font-display text-[24px] tracking-[0.16em] text-ink">灶台手账</h1>
        <p class="text-[12px] text-ink-muted">一餐一饭，皆是手记。</p>
      </div>

      <div class="relative w-full max-w-[26rem]">
        <!-- 和纸胶带装饰 -->
        <span
          class="absolute -top-2.5 left-1/2 z-10 h-5 w-24 -translate-x-1/2 -rotate-2 rounded-[2px] border border-ginger/50 bg-ginger/55"
          aria-hidden="true"
        />

        <div class="rounded-card border border-line bg-card p-5 shadow-lift sm:p-6">
          <!-- 登录 / 注册切换 -->
          <div class="flex rounded-full bg-paper-deep/80 p-1" role="tablist" aria-label="登录或注册">
            <button
              type="button"
              role="tab"
              :aria-selected="!isRegister"
              class="h-11 flex-1 rounded-full text-[13.5px] transition"
              :class="!isRegister ? 'bg-card text-ink shadow-paper' : 'text-ink-muted hover:text-ink-soft'"
              @click="switchMode('login')"
            >
              登录
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="isRegister"
              class="h-11 flex-1 rounded-full text-[13.5px] transition"
              :class="isRegister ? 'bg-card text-ink shadow-paper' : 'text-ink-muted hover:text-ink-soft'"
              @click="switchMode('register')"
            >
              注册
            </button>
          </div>

          <div class="mt-5">
            <h2 class="font-display text-[20px] text-ink">
              {{ isRegister ? '新开一本手账' : '回到你的灶台' }}
            </h2>
            <p class="mt-1 text-[12px] text-ink-muted">
              {{ isRegister ? '注册后可以收藏菜谱、配置口味与 AI。' : '登录后查看今日菜单与你的菜谱。' }}
            </p>
          </div>

          <form class="mt-5 space-y-3.5" @submit.prevent="submit">
            <div v-if="isRegister">
              <label for="login-nickname" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
                昵称（可选）
              </label>
              <input
                id="login-nickname"
                v-model="form.nickname"
                type="text"
                maxlength="20"
                autocomplete="nickname"
                placeholder="家里人怎么叫你"
                class="h-12 w-full rounded-xl border border-line bg-paper/60 px-3.5 text-[16px] text-ink placeholder:text-ink-muted/70 transition focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
              />
            </div>

            <div>
              <label for="login-username" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
                用户名
              </label>
              <input
                id="login-username"
                v-model="form.username"
                type="text"
                maxlength="20"
                autocomplete="username"
                autocapitalize="none"
                spellcheck="false"
                placeholder="3–20 位字母、数字或下划线"
                class="h-12 w-full rounded-xl border border-line bg-paper/60 px-3.5 text-[16px] text-ink placeholder:text-ink-muted/70 transition focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
              />
            </div>

            <div>
              <label for="login-password" class="mb-1.5 block text-[12px] font-medium text-ink-soft">
                密码
              </label>
              <div class="relative">
                <input
                  id="login-password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  maxlength="32"
                  :autocomplete="isRegister ? 'new-password' : 'current-password'"
                  placeholder="6–32 位"
                  class="h-12 w-full rounded-xl border border-line bg-paper/60 px-3.5 pr-12 text-[16px] text-ink placeholder:text-ink-muted/70 transition focus:border-tomato/50 focus:bg-card focus:outline-none focus:ring-2 focus:ring-tomato/15"
                />
                <button
                  type="button"
                  class="absolute right-1 top-1 grid h-11 w-11 place-items-center rounded-lg text-ink-muted transition hover:bg-paper-deep hover:text-ink"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  @click="showPassword = !showPassword"
                >
                  <AppIcon name="eye" :size="17" />
                </button>
              </div>
            </div>

            <p
              v-if="errorMessage"
              class="flex items-start gap-1.5 rounded-xl bg-tomato-soft px-3 py-2.5 text-[12.5px] leading-relaxed text-tomato-deep"
              role="alert"
            >
              <AppIcon name="alert" :size="14" class="mt-0.5 shrink-0" />
              {{ errorMessage }}
            </p>

            <button
              type="submit"
              class="h-12 w-full rounded-full bg-tomato text-[15px] font-medium text-paper shadow-[0_3px_0_rgba(176,59,41,0.55)] transition hover:bg-tomato-deep active:translate-y-px active:shadow-none disabled:cursor-not-allowed disabled:opacity-55"
              :disabled="!canSubmit"
            >
              {{ submitting ? '开火中…' : isRegister ? '注册并开始记录' : '登录' }}
            </button>
          </form>

          <!-- 演示账号一键填充 -->
          <button
            type="button"
            class="mt-3 flex w-full items-center gap-2 rounded-xl border border-dashed border-line-strong bg-paper/60 px-3 py-2.5 text-left transition hover:border-ginger hover:bg-ginger-soft/50"
            @click="fillDemo"
          >
            <span class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-ginger-soft text-[#8a6215]">
              <AppIcon name="sparkle" :size="15" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block text-[12.5px] font-medium text-ink">用演示账号看看</span>
              <span class="block font-mono text-[11px] text-ink-muted">demo / 123456</span>
            </span>
            <span class="text-[11.5px] text-tomato-deep">一键填充</span>
          </button>

          <p class="mt-4 text-center text-[11.5px] text-ink-muted">
            {{ isRegister ? '已有账号？' : '还没账号？' }}
            <button
              type="button"
              class="font-medium text-tomato-deep underline-offset-4 hover:underline"
              @click="switchMode(isRegister ? 'login' : 'register')"
            >
              {{ isRegister ? '去登录' : '注册一个' }}
            </button>
          </p>
        </div>

        <p class="mt-4 flex items-center justify-center gap-1.5 text-center text-[10.5px] leading-relaxed text-ink-muted">
          <AppIcon name="info" :size="12" />
          API Key 仅加密保存在你自己的后端，前端只显示掩码
        </p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.steam {
  animation: login-steam 4.6s ease-in-out infinite;
}
.steam-2 {
  animation-delay: 0.9s;
}
.steam-3 {
  animation-delay: 1.8s;
}
@keyframes login-steam {
  0% {
    opacity: 0;
    transform: translateY(6px);
  }
  35% {
    opacity: 0.7;
  }
  100% {
    opacity: 0;
    transform: translateY(-12px);
  }
}
</style>
