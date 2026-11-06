# menu-frontend

Menu · 灶台手账 的前端：Vue 3.5 + TypeScript + Vite 8 + Vue Router + Pinia + Tailwind CSS v4。
桌面与移动端共用一套组件，移动端为底部 Tab + 单列/弹层交互。

完整启动说明见仓库根目录 [README](../README.md)。

## 开发

```bash
npm install
npm run dev       # http://localhost:5173，/api 代理到 :8080
```

## 构建与检查

```bash
npm run build     # vue-tsc + vite build
npm run test      # Vitest（37 个用例）
npm run lint      # ESLint
npm run format    # Prettier
```

## 结构

```
src/
├─ api/           # 统一 fetch 封装，解包 Result、Bearer、401 跳登录
├─ components/    # AppShell / 导航 / 菜谱卡 / 餐次卡 / 弹层 / Toast 等
├─ views/         # 登录、首页、菜谱库、详情、导入、今日菜单、我的、设置
├─ stores/        # auth / toast
├─ composables/   # useAsync / useMediaQuery
├─ utils/         # 份量换算、平台识别、格式化、token
├─ assets/main.css# 设计系统（@theme token + 纸张纹理 + 动效）
└─ types/api.ts   # 与 docs/api-contract.md 对齐的类型
```

## 设计

奶油纸底 + 墨色文字 + 番茄红/香草绿/姜黄点缀，衬线中文标题；`main.css` 的 `@theme`
集中定义颜色、字体、圆角与阴影。移动端 375px 起无横向滚动，触控目标 ≥44px。

验收截图见 `../docs/screenshots/`，可用 `../tools/e2e/screenshots.cjs` 重新生成。
