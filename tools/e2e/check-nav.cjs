const { chromium } = require('playwright')
;(async () => {
  const login = await (await fetch('http://127.0.0.1:8080/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: '123456' }),
  })).json()
  const token = login.data.token
  const browser = await chromium.launch({ channel: 'chrome', headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.addInitScript((value) => localStorage.setItem('menu.token', value), token)
  const page = await context.newPage()
  await page.goto('http://localhost:5173/', { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const header = page.locator('header.sticky')
  const text = (await header.innerText()).replace(/\s+/g, ' ')
  console.log('HEADER:', text)
  const count = (name) => (text.match(new RegExp(name, 'g')) || []).length
  console.log('导入=', count('导入'), '记一笔=', count('记一笔'), '我的=', count('我的'))
  await page.screenshot({ path: 'docs/screenshots/desktop/desktop-nav.png', clip: { x: 0, y: 0, width: 1280, height: 80 } })
  await browser.close()
})().catch(e => { console.error(e); process.exit(1) })
