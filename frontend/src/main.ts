import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setApiErrorNotifier, setUnauthorizedHandler } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// API 层的两个出口统一在这里接线，避免 http.ts 反向依赖 store / router
setApiErrorNotifier((message) => {
  useToastStore(pinia).error(message)
})

setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  auth.logout()
  const current = router.currentRoute.value
  if (current.name === 'login') return
  void router.replace({ name: 'login', query: { redirect: current.fullPath } })
})

// 已存在 token 时先拉取当前用户（store 初始化逻辑），失败不阻塞首屏挂载
const auth = useAuthStore(pinia)
void auth.bootstrap()

app.mount('#app')
