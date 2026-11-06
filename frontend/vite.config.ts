import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  // 不用 vite-plugin-vue-devtools：它的悬浮按钮会遮住移动端底部 Tab 与页面操作栏
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      // 路径别名：@ -> ./src，需与 tsconfig.app.json 中的 paths 保持一致
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 端口被占用时直接失败，避免开发地址漂移（后端 CORS / 代理按 5173 约定）
    strictPort: true,
    proxy: {
      // 开发环境统一把 /api 代理到 Spring Boot 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 5173,
  },
})
