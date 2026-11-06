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
      title: '开火计时示例', description: '从开始做菜到出锅，记录真实用时', categoryId: 1,
      durationMin: 30, difficulty: 1, servings: 2, meatType: 'VEG', spiceLevel: 0,
      tags: ['计时'], ingredients: [{ name: '食材', amount: '1', unit: '份' }],
      steps: [{ content: '按步骤烹饪，完成后结束计时。' }],
    }),
  })).json()
  const id = created.data.id
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.addInitScript((v) => localStorage.setItem('menu.token', v), token)
  const page = await context.newPage()
  await page.goto(`http://localhost:5173/recipes/${id}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  await page.getByText('开火计时', { exact: true }).scrollIntoViewIfNeeded()
  await page.waitForTimeout(500)
  await page.screenshot({ path: 'docs/screenshots/desktop/desktop-cook-timer.png', fullPage: false })
  await browser.close()
  await fetch(`http://127.0.0.1:8080/api/recipes/${id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
  console.log('done', id)
})().catch((e) => { console.error(e); process.exit(1) })
