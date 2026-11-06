/**
 * 验证 AI 配置页的 OpenCode Zen 预设：选项存在、自动填充 Base URL/模型、提示免费模型限制。
 * 前置：后端 8080 + 前端 npm run dev 已启动。
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const BACKEND = 'http://127.0.0.1:8080'
const FRONTEND = 'http://localhost:5173'

async function login() {
  const response = await fetch(`${BACKEND}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: '123456' }),
  })
  const payload = await response.json()
  return payload.data.token
}

async function main() {
  const token = await login()
  const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || 'chrome', headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.addInitScript((value) => {
    window.localStorage.setItem('menu.token', value)
  }, token)
  const page = await context.newPage()
  await page.goto(`${FRONTEND}/settings/ai`, { waitUntil: 'networkidle' })
  await page.getByRole('button', { name: /新增/ }).click()
  await page.waitForTimeout(400)
  await page.locator('#provider-type').selectOption('opencode')
  await page.waitForTimeout(300)

  const result = {
    options: await page.locator('#provider-type option').allTextContents(),
    baseUrl: await page.locator('#provider-base').inputValue(),
    chatModel: await page.locator('#provider-chat').inputValue(),
    hint: (await page.locator('#provider-type').locator('..').locator('p').innerText()).trim(),
  }
  const dir = path.resolve(__dirname, '../../docs/screenshots/desktop')
  fs.mkdirSync(dir, { recursive: true })
  await page.screenshot({ path: path.join(dir, 'desktop-ai-opencode.png'), fullPage: false })

  console.log(JSON.stringify(result, null, 2))
  await browser.close()

  if (
    !result.options.some((text) => text.includes('OpenCode')) ||
    result.baseUrl !== 'https://opencode.ai/inference/openai/v1' ||
    !result.chatModel ||
    !result.hint.includes('免费模型只允许 OpenCode 客户端')
  ) {
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
