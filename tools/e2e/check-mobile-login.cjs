/**
 * 回归测试：局域网/手机地址访问登录 + 两个浏览器同时登录不同账号互不影响。
 *
 * 前置：后端 8080、前端 npm run dev 已在跑。
 * 运行：cd tools/e2e && node check-mobile-login.cjs
 */
const { chromium } = require('playwright')
const crypto = require('crypto')

const BACKEND = 'http://127.0.0.1:8080'
const LOCAL = 'http://localhost:5173'
const LAN = process.env.LAN_URL || 'http://10.8.131.19:5173'

async function registerUser() {
  const username = `mobile_${crypto.randomBytes(3).toString('hex')}`
  const response = await fetch(`${BACKEND}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: '123456', nickname: '手机测试' }),
  })
  const payload = await response.json()
  if (payload.code !== 200) throw new Error(`注册失败: ${JSON.stringify(payload)}`)
  return { username, token: payload.data.token }
}

async function loginUi(page, base, username, password) {
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
  await page.locator('input').first().fill(username)
  await page.locator('input[type="password"]').fill(password)
  const failed = []
  page.on('response', (response) => {
    if (response.status() === 403) failed.push(`${response.status()} ${response.url()}`)
  })
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForTimeout(1800)
  return failed
}

async function main() {
  const userB = await registerUser()
  const browser = await chromium.launch({ channel: process.env.PW_CHANNEL || 'chrome', headless: true })

  const contextA = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  const pageA = await contextA.newPage()
  const failedA = await loginUi(pageA, LOCAL, 'demo', '123456')
  const nameA = await pageA.locator('body').innerText()

  const contextB = await browser.newContext({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
  })
  const pageB = await contextB.newPage()
  const failedB = await loginUi(pageB, LAN, userB.username, '123456')
  const nameB = await pageB.locator('body').innerText()

  const tokens = await Promise.all([
    pageA.evaluate(() => localStorage.getItem('menu.token')),
    pageB.evaluate(() => localStorage.getItem('menu.token')),
  ])
  const meA = await (await fetch(`${BACKEND}/api/auth/me`, { headers: { Authorization: `Bearer ${tokens[0]}` } })).json()
  const meB = await (await fetch(`${BACKEND}/api/auth/me`, { headers: { Authorization: `Bearer ${tokens[1]}` } })).json()

  const result = {
    lanUrl: LAN,
    userB: userB.username,
    pageA: { loggedIn: nameA.includes('小灶'), http403: failedA },
    pageB: { loggedIn: nameB.includes('手机测试'), http403: failedB },
    tokensIsolated: tokens[0] !== tokens[1],
    meA: meA.data?.username,
    meB: meB.data?.username,
  }
  console.log(JSON.stringify(result, null, 2))
  await contextA.close()
  await contextB.close()
  await browser.close()

  if (!result.pageA.loggedIn || !result.pageB.loggedIn || failedA.length || failedB.length || !result.tokensIsolated) {
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
