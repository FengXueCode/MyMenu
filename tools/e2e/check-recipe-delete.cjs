const { chromium } = require('playwright')
const fs = require('fs')
;(async () => {
  const login = await (await fetch('http://127.0.0.1:8080/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: '123456' }),
  })).json()
  const token = login.data.token
  const created = await (await fetch('http://127.0.0.1:8080/api/recipes', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      title: '删除按钮验收菜', description: '用于验证详情页删除入口', categoryId: 1,
      durationMin: 15, difficulty: 1, servings: 2, meatType: 'VEG', spiceLevel: 0,
      tags: ['测试'], ingredients: [{ name: '青椒', amount: '200', unit: 'g' }],
      steps: [{ content: '切好，炒熟，装盘。' }],
    }),
  })).json()
  const id = created.data.id
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.addInitScript((v) => localStorage.setItem('menu.token', v), token)
  const page = await context.newPage()
  await page.goto(`http://localhost:5173/recipes/${id}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const text = (await page.locator('body').innerText()).replace(/\s+/g, ' ')
  console.log('HAS_DELETE', text.includes('删除'))
  await page.screenshot({ path: 'docs/screenshots/desktop/desktop-recipe-delete.png', fullPage: true })
  const mobile = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
  await mobile.addInitScript((v) => localStorage.setItem('menu.token', v), token)
  const mp = await mobile.newPage()
  await mp.goto(`http://localhost:5173/recipes/${id}`, { waitUntil: 'networkidle' })
  await mp.waitForTimeout(600)
  await mp.screenshot({ path: 'docs/screenshots/mobile/mobile-recipe-delete.png', fullPage: false })
  await browser.close()
  await fetch(`http://127.0.0.1:8080/api/recipes/${id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
  console.log('deleted', id)
})().catch((e) => { console.error(e); process.exit(1) })
