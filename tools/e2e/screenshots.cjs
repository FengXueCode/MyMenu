/**
 * Menu 全站视觉验收：桌面 + 移动端截图、横向溢出检查、控制台错误采集。
 *
 * 前置：
 *   1. 后端 http://127.0.0.1:8080 已启动（demo/123456 可用）
 *   2. 前端 http://localhost:5173 已启动（npm run dev）
 * 运行：
 *   cd tools/e2e && node screenshots.cjs
 * 输出：
 *   docs/screenshots/desktop/*.png、docs/screenshots/mobile/*.png + report.json
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const BACKEND = process.env.BACKEND_URL || 'http://127.0.0.1:8080'
const FRONTEND = process.env.FRONTEND_URL || 'http://localhost:5173'
const OUT = path.resolve(__dirname, '../../docs/screenshots')

async function login() {
  const response = await fetch(`${BACKEND}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: '123456' }),
  })
  const payload = await response.json()
  if (payload.code !== 200) throw new Error(`登录失败：${JSON.stringify(payload)}`)
  return payload.data.token
}

async function firstRecipeId(token) {
  const response = await fetch(`${BACKEND}/api/recipes?pageSize=1`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const payload = await response.json()
  return payload.data.records[0].id
}

async function autoScroll(page) {
  await page.evaluate(async () => {
    await new Promise((resolve) => {
      let total = 0
      const step = 360
      const timer = setInterval(() => {
        window.scrollBy(0, step)
        total += step
        if (total >= document.body.scrollHeight) {
          clearInterval(timer)
          resolve()
        }
      }, 40)
    })
  })
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(400)
}

async function shoot(context, pages, targetDir, prefix) {
  fs.mkdirSync(targetDir, { recursive: true })
  const report = []
  for (const item of pages) {
    const page = await context.newPage()
    const consoleErrors = []
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text())
    })
    page.on('pageerror', (error) => consoleErrors.push(String(error)))
    await page.goto(`${FRONTEND}${item.path}`, { waitUntil: 'networkidle', timeout: 45000 }).catch((error) => {
      consoleErrors.push(`goto: ${error.message}`)
    })
    await page.waitForTimeout(item.wait ?? 900)
    if (item.action) await item.action(page).catch((error) => consoleErrors.push(`action: ${error.message}`))
    // 全页截图前先滚到底触发懒加载，避免图片空白
    if (item.fullPage !== false) await autoScroll(page)
    await page.waitForTimeout(300)
    const metrics = await page.evaluate(() => {
      const root = document.documentElement
      return {
        scrollWidth: root.scrollWidth,
        clientWidth: root.clientWidth,
        scrollHeight: root.scrollHeight,
        bodyScrollWidth: document.body.scrollWidth,
      }
    })
    const overflowX = metrics.scrollWidth > metrics.clientWidth + 1 || metrics.bodyScrollWidth > metrics.clientWidth + 1
    await page.screenshot({
      path: path.join(targetDir, `${prefix}-${item.name}.png`),
      fullPage: item.fullPage !== false,
    })
    report.push({
      page: item.name,
      path: item.path,
      overflowX,
      metrics,
      consoleErrors: consoleErrors.filter((text) => !text.includes('favicon')),
    })
    await page.close()
  }
  return report
}

async function main() {
  const token = await login()
  const recipeId = await firstRecipeId(token)
  const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || 'chrome', headless: true })

  const authInit = (context) =>
    context.addInitScript((value) => {
      window.localStorage.setItem('menu.token', value)
    }, token)

  const report = { desktop: [], mobile: [], consoleErrors: [] }

  // ---------- 桌面端 ----------
  const desktop = await browser.newContext({ viewport: { width: 1280, height: 900 }, deviceScaleFactor: 1 })
  await authInit(desktop)
  report.desktop = await shoot(
    desktop,
    [
      { name: 'home', path: '/' },
      { name: 'recipes', path: '/recipes' },
      { name: 'detail', path: `/recipes/${recipeId}` },
      { name: 'today', path: '/today' },
      { name: 'import', path: '/import' },
      { name: 'me', path: '/me' },
      { name: 'preferences', path: '/settings/preferences' },
      { name: 'ai-settings', path: '/settings/ai' },
    ],
    path.join(OUT, 'desktop'),
    'desktop',
  )
  await desktop.close()

  // 登录页（无 token）
  const loginContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  report.desktop.push(
    ...(await shoot(loginContext, [{ name: 'login', path: '/login', fullPage: false }], path.join(OUT, 'desktop'), 'desktop')),
  )
  await loginContext.close()

  // ---------- 移动端 ----------
  const mobile = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
  })
  await authInit(mobile)
  report.mobile = await shoot(
    mobile,
    [
      { name: 'home', path: '/', fullPage: false },
      { name: 'home-full', path: '/', fullPage: true },
      { name: 'recipes', path: '/recipes', fullPage: false },
      { name: 'detail', path: `/recipes/${recipeId}`, fullPage: false },
      {
        name: 'detail-ingredients',
        path: `/recipes/${recipeId}`,
        fullPage: false,
        action: async (page) => {
          await page.evaluate(() => window.scrollTo(0, 760))
          await page.waitForTimeout(500)
        },
      },
      { name: 'today', path: '/today', fullPage: false },
      { name: 'import', path: '/import', fullPage: false },
      { name: 'me', path: '/me', fullPage: false },
      { name: 'preferences', path: '/settings/preferences', fullPage: false },
      { name: 'ai-settings', path: '/settings/ai', fullPage: false },
      {
        name: 'recipes-filter',
        path: '/recipes',
        fullPage: false,
        action: async (page) => {
          const trigger = page.getByRole('button', { name: /筛选|口味/ }).first()
          if (await trigger.count()) {
            await trigger.click()
            await page.waitForTimeout(500)
          }
        },
      },
    ],
    path.join(OUT, 'mobile'),
    'mobile',
  )
  await mobile.close()

  const loginMobile = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
  })
  report.mobile.push(
    ...(await shoot(loginMobile, [{ name: 'login', path: '/login', fullPage: false }], path.join(OUT, 'mobile'), 'mobile')),
  )
  await loginMobile.close()

  await browser.close()

  const all = [...report.desktop, ...report.mobile]
  report.consoleErrors = all.filter((item) => item.consoleErrors.length > 0)
  fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify(report, null, 2), 'utf-8')

  const overflowPages = all.filter((item) => item.overflowX)
  console.log(`截图完成：${all.length} 张`)
  console.log(`横向溢出页面：${overflowPages.length ? overflowPages.map((p) => p.page).join(', ') : '无'}`)
  console.log(`控制台错误页面：${report.consoleErrors.length ? report.consoleErrors.map((p) => `${p.page}: ${p.consoleErrors[0]}`).join('\n') : '无'}`)
  if (overflowPages.length) process.exitCode = 1
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
