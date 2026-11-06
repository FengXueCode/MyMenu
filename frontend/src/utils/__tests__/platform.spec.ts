import { describe, expect, it } from 'vitest'

import {
  bilibiliEmbedUrl,
  canEmbedVideo,
  detectPlatform,
  embedUrlFromResolve,
  extractBvid,
  extractUrl,
  sourcePageUrl,
} from '@/utils/platform'
import type { ImportResolveResult } from '@/types/api'

describe('extractUrl', () => {
  it('从整段分享文案中提取第一个链接', () => {
    const text =
      '【标题】红烧肉教程 分享自 B 站 https://www.bilibili.com/video/BV1xx411c7mD?share_source=copy 复制链接'
    expect(extractUrl(text)).toBe('https://www.bilibili.com/video/BV1xx411c7mD?share_source=copy')
  })

  it('没有链接时返回 null', () => {
    expect(extractUrl('纯文案没有链接')).toBeNull()
    expect(extractUrl('')).toBeNull()
  })
})

describe('detectPlatform', () => {
  it('识别 B 站链接与短链', () => {
    expect(detectPlatform('https://www.bilibili.com/video/BV1xx411c7mD')).toBe('bilibili')
    expect(detectPlatform('https://b23.tv/abcd123')).toBe('bilibili')
    expect(detectPlatform('https://m.bilibili.com/video/BV1xx411c7mD')).toBe('bilibili')
  })

  it('识别抖音链接（含短链与分享页）', () => {
    expect(detectPlatform('https://v.douyin.com/iRabc123/')).toBe('douyin')
    expect(detectPlatform('https://www.douyin.com/video/7300000000000000000')).toBe('douyin')
    expect(detectPlatform('https://www.iesdouyin.com/share/video/7300')).toBe('douyin')
  })

  it('识别分享文案与裸 BV 号', () => {
    expect(detectPlatform('看这个 https://b23.tv/zzz 超好吃')).toBe('bilibili')
    expect(detectPlatform('BV1xx411c7mD')).toBe('bilibili')
  })

  it('其他域名与空输入为 unknown', () => {
    expect(detectPlatform('https://example.com/video/1')).toBe('unknown')
    expect(detectPlatform('')).toBe('unknown')
    expect(detectPlatform(null)).toBe('unknown')
    expect(detectPlatform(undefined)).toBe('unknown')
  })
})

describe('extractBvid / bilibiliEmbedUrl', () => {
  it('从链接中提取 BV 号', () => {
    expect(extractBvid('https://www.bilibili.com/video/BV1xx411c7mD/?p=1')).toBe('BV1xx411c7mD')
    expect(extractBvid('https://b23.tv/abcd')).toBeNull()
  })

  it('生成契约规定的播放器地址', () => {
    expect(bilibiliEmbedUrl('BV1xx411c7mD')).toBe(
      'https://player.bilibili.com/player.html?bvid=BV1xx411c7mD&page=1&high_quality=1&danmaku=0',
    )
    expect(bilibiliEmbedUrl('https://www.bilibili.com/video/BV1xx411c7mD')).toBe(
      'https://player.bilibili.com/player.html?bvid=BV1xx411c7mD&page=1&high_quality=1&danmaku=0',
    )
    expect(bilibiliEmbedUrl('https://b23.tv/abcd')).toBeNull()
    expect(bilibiliEmbedUrl(null)).toBeNull()
  })
})

describe('canEmbedVideo', () => {
  it('B 站链接可嵌入', () => {
    expect(canEmbedVideo('https://www.bilibili.com/video/BV1xx411c7mD')).toBe(true)
  })

  it('显式 canEmbed=false 或非 B 站不可嵌入', () => {
    expect(canEmbedVideo('https://www.bilibili.com/video/BV1xx411c7mD', false)).toBe(false)
    expect(canEmbedVideo('https://v.douyin.com/iRabc123/')).toBe(false)
    expect(canEmbedVideo(null)).toBe(false)
  })
})

describe('解析结果辅助方法', () => {
  const result: ImportResolveResult = {
    platform: 'bilibili',
    bvid: 'BV1xx411c7mD',
    canEmbed: true,
    warnings: [],
  }

  it('embedUrlFromResolve 依赖 canEmbed 与 bvid', () => {
    expect(embedUrlFromResolve(result)).toContain('bvid=BV1xx411c7mD')
    expect(embedUrlFromResolve({ ...result, canEmbed: false })).toBeNull()
    expect(embedUrlFromResolve(null)).toBeNull()
  })

  it('sourcePageUrl 生成 B 站原视频地址', () => {
    expect(sourcePageUrl(result)).toBe('https://www.bilibili.com/video/BV1xx411c7mD')
    expect(sourcePageUrl({ ...result, platform: 'douyin', bvid: undefined })).toBeNull()
  })
})
