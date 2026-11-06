/**
 * 视频平台识别与 B 站嵌入地址构造。
 * 只做纯字符串解析，不发起网络请求（短链解析由后端 /api/import/resolve 负责）。
 */
import type { ImportResolveResult, SourcePlatform } from '@/types/api'

const BILIBILI_DOMAINS = ['bilibili.com', 'b23.tv']
const DOUYIN_DOMAINS = ['douyin.com', 'iesdouyin.com']

/** 从整段分享文案中提取第一个 http(s) 链接 */
export function extractUrl(text: string | null | undefined): string | null {
  if (!text) return null
  const match = text.match(/https?:\/\/[^\s，。、）)】"'<>]+/i)
  return match ? match[0] : null
}

/** 从链接/文案里提取 BV 号 */
export function extractBvid(text: string | null | undefined): string | null {
  if (!text) return null
  const match = text.match(/BV[0-9A-Za-z]{10}/)
  return match ? match[0] : null
}

function hostOf(input: string): string | null {
  const candidate = extractUrl(input)
  if (!candidate) {
    // 允许直接传域名或 "b23.tv/xxxx"
    if (/^[\w.-]+\.[a-z]{2,}/i.test(input.trim())) {
      const prefixed = `https://${input.trim()}`
      return safeHost(prefixed)
    }
    return null
  }
  return safeHost(candidate)
}

function safeHost(url: string): string | null {
  try {
    return new URL(url).hostname.toLowerCase()
  } catch {
    return null
  }
}

function matchesDomain(host: string, domains: string[]): boolean {
  return domains.some((domain) => host === domain || host.endsWith(`.${domain}`))
}

/**
 * 识别平台：
 * - 链接或整段分享文案中的域名；
 * - 单独的 BV 号（BV1xx411c7mD）视为 B 站。
 */
export function detectPlatform(input: string | null | undefined): SourcePlatform {
  if (!input) return 'unknown'
  const text = input.trim()
  if (!text) return 'unknown'

  const host = hostOf(text)
  if (host) {
    if (matchesDomain(host, BILIBILI_DOMAINS)) return 'bilibili'
    if (matchesDomain(host, DOUYIN_DOMAINS)) return 'douyin'
    return 'unknown'
  }

  if (/^BV[0-9A-Za-z]{10}$/.test(text) || extractBvid(text)) return 'bilibili'
  return 'unknown'
}

/** B 站播放器 iframe 地址（契约固定格式） */
export function bilibiliEmbedUrl(bvidOrUrl: string | null | undefined): string | null {
  if (!bvidOrUrl) return null
  const bvid = extractBvid(bvidOrUrl) ?? bvidOrUrl.trim()
  if (!/^BV[0-9A-Za-z]{10}$/.test(bvid)) return null
  return `https://player.bilibili.com/player.html?bvid=${bvid}&page=1&high_quality=1&danmaku=0`
}

/**
 * 详情页是否可嵌入播放：
 * - 导入解析结果显式给了 canEmbed 时以它为准；
 * - 菜谱详情没有 canEmbed 字段，则凭 sourceUrl 里的 BV 号判断。
 */
export function canEmbedVideo(
  sourceUrl: string | null | undefined,
  canEmbed?: boolean | null,
): boolean {
  if (canEmbed === false) return false
  if (!sourceUrl) return false
  if (detectPlatform(sourceUrl) !== 'bilibili') return false
  return bilibiliEmbedUrl(sourceUrl) !== null
}

/** 解析结果 → 可嵌入播放地址（不可嵌入返回 null） */
export function embedUrlFromResolve(result: ImportResolveResult | null | undefined): string | null {
  if (!result) return null
  if (!result.canEmbed) return null
  return bilibiliEmbedUrl(result.bvid ?? result.videoId ?? result.title ?? '')
}

/** 解析结果对应的原始页面地址（用于「去 B 站看原视频」） */
export function sourcePageUrl(result: ImportResolveResult | null | undefined): string | null {
  if (!result) return null
  if (result.platform === 'bilibili') {
    const bvid = result.bvid ?? extractBvid(result.videoId)
    if (bvid) return `https://www.bilibili.com/video/${bvid}`
  }
  return null
}
