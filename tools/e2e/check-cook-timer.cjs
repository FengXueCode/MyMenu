const { chromium } = require('playwright')
;(async () => {
  const login = await (await fetch('http://127.0.0.1:8080/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: '123456' }),
  })).json()
  const token = login.data.token
  const created = await (await fetch('http://127.0.0.1:8080/api/recipes', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      title: '计时器验收菜', description: '验证开火计时与预估更新', categoryId: 1,
      durationMin: 30, difficulty: 1, servings: 2, meatType: 'VEG', spiceLevel: 0,
      tags: ['测试'], ingredients: [{ name: '食材', amount: '1', unit: '份' }],
      steps: [{ content: '测试步骤' }],
    }),
  })).json()
  const id = created.data.id
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.addInitScript((v) => localStorage.setItem('menu.token', v), token)
  const page = await context.newPage()
  await page.goto(`http://localhost:5173/recipes/${id}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  const before = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
  console.log('CARD', before.includes('开火计时'), 'EST', before.includes('预估 30 分钟'))
  await page.getByRole('button', { name: '开始计时' }).click()
  await page.waitForTimeout(1500)
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForTimeout(900)
  const reloaded = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
  console.log('PERSISTED_RUNNING', reloaded.includes('计时中'))
  await page.waitForTimeout(1200)
  await page.getByRole('button', { name: /结束并记录/ }).click()
  await page.waitForTimeout(1500)
  const after = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
  console.log('UPDATED_EST', after.includes('预估 1 分钟'), 'MSG', after.includes('预估时间已更新'))
  await page.screenshot({ path: 'docs/screenshots/desktop/desktop-cook-timer.png', fullPage: true })
  const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
  await mobile.addInitScript((v) => localStorage.setItem('menu.token', v), token)
  const mp = await mobile.newPage()
  await mp.goto(`http://localhost:5173/recipes/${id}`, { waitUntil: 'networkidle' })
  await mp.waitForTimeout(700)
  await mp.screenshot({ path: 'docs/screenshots/mobile/mobile-cook-timer.png', fullPage: false })
  await browser.close()
  await fetch(`http://127.0.0.1:8080/api/recipes/${id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
  console.log('deleted recipe', id)
})().catch((e) => { console.error(e); process.exit(1) })
