# Vue 3 现代技术栈模板

基于 **Vite + Vue 3 + TypeScript** 的中后台 / Web 应用通用模板，已集成 Vue Router、Pinia、
Tailwind CSS v4、Vitest、ESLint + Prettier。所有代码使用 `<script setup>` 与 Composition API，
样式全部通过 Tailwind 工具类完成。

> 由官方脚手架 `npm create vue@latest`（create-vue 3.24.0）初始化，勾选 TypeScript、Vue Router、
> Pinia、Vitest、ESLint、Prettier，再按需接入 Tailwind CSS v4。

## 关键版本

| 依赖         | 版本    | 说明                                       |
| ------------ | ------- | ------------------------------------------ |
| Vue          | 3.5.44  | `<script setup>` + Composition API         |
| Vue Router   | 5.4.0   | `createRouter` / `createWebHistory`        |
| Pinia        | 4.1.0   | setup store 写法（未使用 Vuex）            |
| Vite         | 8.3.5   | 开发服务器与构建                           |
| TypeScript   | 6.0.3   | `vue-tsc --build` 类型检查                 |
| Tailwind CSS | 4.3.4   | `@tailwindcss/vite` 官方 Vite 集成         |
| Vitest       | 4.1.12  | 单元测试（jsdom + @vue/test-utils）        |
| ESLint       | 10.14.0 | flat config                                |
| Prettier     | 3.9.6   | 与 ESLint 通过 eslint-config-prettier 隔离 |

> 以上为本次安装的实际版本；精确锁定版本见 `package-lock.json`。
> 运行要求：Node.js `^22.18.0 || >=24.12.0`（见 `package.json` 的 `engines`）。
> 路由按需求使用 **Vue Router 5.x**：基础路由 / 导航 API 与 v4 一致，未使用 v5 新增的文件路由能力。

## 快速开始

```bash
npm install
npm run dev
```

开发服务器默认运行在 <http://localhost:5173>。导航栏可在「首页 / 关于」之间切换，
用于验证 Vue Router 与 `<script setup>` 工作正常。

## 常用命令

| 命令                     | 说明                                           |
| ------------------------ | ---------------------------------------------- |
| `npm run dev`            | 启动开发服务器（HMR）                          |
| `npm run build`          | 类型检查（vue-tsc）+ 生产构建，输出到 `dist/`  |
| `npm run preview`        | 本地预览构建产物                               |
| `npm run type-check`     | 仅执行 `vue-tsc` 类型检查                      |
| `npm run test`           | 运行 Vitest 单元测试（单次）                   |
| `npm run test:watch`     | Vitest 监听模式                                |
| `npm run lint`           | ESLint 检查（flat config）                     |
| `npm run lint:fix`       | ESLint 自动修复                                |
| `npm run format`         | Prettier 格式化 `src/`                         |
| `npm run format:check`   | Prettier 校验（可用于 CI）                     |

`npm run build` 使用 `npm-run-all2` 并行执行 `type-check` 与 `vite build`。

## 目录结构

```
.
├─ public/                  # 不经过构建的静态资源（favicon 等）
├─ src/
│  ├─ assets/               # 静态资源与全局样式入口（Tailwind 入口 main.css、logo）
│  ├─ components/           # 可复用组件（AppHeader、StatCard）
│  ├─ composables/          # 组合式函数，复用与视图无关的逻辑（useCounter）
│  ├─ router/               # 路由表与路由实例（Vue Router 5）
│  ├─ stores/               # Pinia store（counter）
│  ├─ views/                # 页面级组件：HomeView、AboutView
│  ├─ App.vue               # 根组件 / 整体布局壳
│  └─ main.ts               # 应用入口：注册 Pinia、Router，挂载 App
├─ env.d.ts                 # Vite 客户端类型声明
├─ eslint.config.ts         # ESLint 10 flat config
├─ .prettierrc.json         # Prettier 配置
├─ vite.config.ts           # Vite 配置：Vue、Tailwind、DevTools 插件与 @ 别名
├─ vitest.config.ts         # Vitest 配置（复用 vite.config.ts）
├─ tsconfig.json            # TS 项目引用（app / node / vitest）
├─ tsconfig.app.json        # 应用代码 TS 配置（含 @ 别名 paths）
├─ tsconfig.node.json       # 构建工具链 TS 配置
├─ tsconfig.vitest.json     # 测试代码 TS 配置
└─ index.html               # HTML 入口
```

### 各目录用途

| 目录                 | 用途                                                                 |
| -------------------- | -------------------------------------------------------------------- |
| `src/views`          | 页面级组件，作为路由目标；每个路由对应一个视图文件                   |
| `src/components`     | 可复用的展示 / 业务组件，被页面组合使用                              |
| `src/stores`         | Pinia store，集中管理跨组件、跨页面共享的全局状态                    |
| `src/router`         | Vue Router 路由表、路由实例、`meta` 与导航守卫                       |
| `src/composables`    | 组合式函数（`useXxx`），封装可复用的响应式逻辑，便于单测             |
| `src/assets`         | 图片、字体等静态资源，以及 Tailwind 的唯一 CSS 入口 `main.css`        |

## 关键配置说明

### 路径别名 `@`

`@` 指向 `src/`，两处同步配置：

- `vite.config.ts`：`resolve.alias` 使用 `fileURLToPath(new URL('./src', import.meta.url))`
- `tsconfig.app.json`：`compilerOptions.paths` 中 `"@/*": ["./src/*"]`

新增别名时请同时修改这两处，避免「能运行但编辑器报错」或反之。

### Tailwind CSS v4

采用官方 Vite 集成方式，不需要 `tailwind.config.js`、`postcss.config.js`：

1. 安装 `tailwindcss` 与 `@tailwindcss/vite`（均为 4.3.4）
2. 在 `vite.config.ts` 的 `plugins` 中加入 `tailwindcss()`
3. 在 `src/assets/main.css` 中只写 `@import 'tailwindcss';`，并在 `src/main.ts` 引入

页面样式一律使用 Tailwind 工具类，未编写任何额外的 CSS 文件，也未使用 Sass / Less 等预处理器。

### ESLint + Prettier 不冲突

- ESLint 10 扁平配置（`eslint.config.ts`）：`eslint-plugin-vue` + `@vue/eslint-config-typescript`
- 数组最后一项是 `eslint-config-prettier/flat`（`skipFormatting`），它关闭所有与 Prettier
  冲突的格式化规则：**ESLint 负责代码质量，Prettier 负责格式化**
- Prettier 配置位于 `.prettierrc.json`（无分号、单引号、行宽 100）
- 二者分别通过 `npm run lint` / `npm run format` 执行，互不覆盖

### Vue Router 5

- `createRouter` + `createWebHistory(import.meta.env.BASE_URL)`
- `/about` 使用路由级懒加载（`() => import(...)`）做代码分割
- 通过 `RouteMeta` 类型扩展声明 `title`，`router.afterEach` 同步 `document.title`
- 导航使用 `<RouterLink custom v-slot>` 自行渲染 `<a>`，便于用 Tailwind 控制激活态样式

### Pinia 与 composable 示例

- `src/stores/counter.ts`：setup store，包含 `count`、派生状态 `doubled` 及增减 / 重置操作；
  组件中通过 `storeToRefs` 解构以保持响应式
- `src/composables/useCounter.ts`：与视图解耦的计数器逻辑，支持初始值、步长与上下界
- 首页同时演示两者：Pinia 全局状态跨页面共享，composable 每个组件实例持有独立状态

### 单元测试

测试文件放在各目录下的 `__tests__/`：

- `src/stores/__tests__/counter.spec.ts`：Pinia store 行为
- `src/composables/__tests__/useCounter.spec.ts`：composable 边界与重置
- `src/components/__tests__/StatCard.spec.ts`：组件渲染
- `src/views/__tests__/routing.spec.ts`：挂载 App，验证路由切换（RouterView）

## 约定与注意事项

- 仅使用 `<script setup>` + Composition API，不使用 Options API
- 不引入 Vuex、Sass/Less、UnoCSS、Vue CLI
- 页面样式只用 Tailwind 工具类，不新增独立 CSS 文件
- 新增页面：在 `src/views` 建视图 → 在 `src/router/index.ts` 注册路由（含 `meta.title`）

## 参考文档

- [Vue 3](https://cn.vuejs.org/) · [Vue Router](https://router.vuejs.org/zh/) · [Pinia](https://pinia.vuejs.org/zh/)
- [Vite](https://cn.vitejs.dev/) · [Tailwind CSS v4 + Vite](https://tailwindcss.com/docs/installation/using-vite)
- [Vitest](https://vitest.dev/) · [ESLint](https://eslint.org/) · [Prettier](https://prettier.io/)
