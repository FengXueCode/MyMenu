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
    // 允许手机/局域网设备访问开发服务器（http://<电脑IP>:5173）
    host: true,
    port: 5173,
    // 端口被占用时直接失败，避免开发地址漂移（后端 CORS / 代理按 5173 约定）
    strictPort: true,
    proxy: {
      // 开发环境统一把 /api 代理到 Spring Boot 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // 浏览器可能通过局域网 IP / 主机名访问，代理把 Origin 原样转发给后端时，
          // Spring CORS 会认为来源非法并返回 403（前端显示「没有权限执行该操作」）。
          // 这里统一改写为后端白名单内的本机来源。
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('origin', 'http://localhost:5173')
          })
        },
      },
    },
  },
  preview: {
    host: true,
    port: 5173,
  },
})
