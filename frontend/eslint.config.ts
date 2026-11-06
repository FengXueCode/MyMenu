import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginVitest from '@vitest/eslint-plugin'
import skipFormatting from 'eslint-config-prettier/flat'

/**
 * ESLint 10 扁平配置（flat config）。
 *
 * - `eslint-plugin-vue`：Vue 单文件组件规则
 * - `@vue/eslint-config-typescript`：Vue + TypeScript 规则与 parser 配置
 * - `@vitest/eslint-plugin`：测试文件专用规则
 * - `eslint-config-prettier/flat`：关闭所有与 Prettier 冲突的格式化规则，
 *   保证 ESLint 只负责代码质量、Prettier 只负责格式化，两者不打架。
 *   必须放在配置数组的最后。
 */
export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores([
    '**/dist/**',
    '**/dist-ssr/**',
    '**/coverage/**',
    // 本地视觉验收临时目录（无头 Chrome profile / 截图脚本），不参与 lint
    '**/.mock/**',
  ]),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,

  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },

  skipFormatting,
)
