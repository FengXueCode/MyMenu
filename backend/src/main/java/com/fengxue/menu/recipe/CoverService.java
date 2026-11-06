package com.fengxue.menu.recipe;

import com.fengxue.menu.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 无 AI 图像模型时的本地兜底封面：按菜名/荤素生成一张有设计感的 SVG。
 *
 * <p>注意：封面里不写菜名和标签文字——列表卡片、详情页 hero 都已在图片外展示标题，
 * 图片内再写一遍会与页面文字重叠。这里只保留品牌小字与食物插画。</p>
 */
@Service
@RequiredArgsConstructor
public class CoverService {

    private static final String[][] PALETTES = {
            {"#D8503C", "#F0A868"},
            {"#4C7A4F", "#A8C686"},
            {"#B5651D", "#E7B24A"},
            {"#7B4B3A", "#D9A679"},
            {"#5B6C8F", "#A9B8D8"},
            {"#8C4A5A", "#E0A3AE"}
    };

    private final FileStorageService fileStorageService;

    public String generate(String title, String meatType, String tags) {
        String seed = (title == null ? "菜" : title) + "|" + (meatType == null ? "OTHER" : meatType);
        int hash = Math.floorMod(seed.hashCode(), PALETTES.length);
        String[] palette = PALETTES[hash];
        // 装饰元素随菜品轻微变化，避免所有封面完全一样
        int offset = Math.floorMod(seed.hashCode() / 7, 60);

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                    <radialGradient id="glow" cx="0.78" cy="0.16" r="0.75">
                      <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.32"/>
                      <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
                    </radialGradient>
                    <linearGradient id="soup" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.30"/>
                      <stop offset="1" stop-color="#FFFFFF" stop-opacity="0.08"/>
                    </linearGradient>
                  </defs>
                  <rect width="800" height="600" fill="url(#g)"/>
                  <rect width="800" height="600" fill="url(#glow)"/>
                  <circle cx="665" cy="120" r="118" fill="#FFFFFF" fill-opacity="0.06"/>
                  <circle cx="112" cy="505" r="86" fill="#FFFFFF" fill-opacity="0.05"/>
                  <circle cx="690" cy="470" r="40" fill="#FFFFFF" fill-opacity="0.08"/>

                  <!-- 热气 -->
                  <path d="M342 246 C322 214 362 196 342 164" stroke="#FFFFFF" stroke-opacity="0.42"
                        stroke-width="6" stroke-linecap="round" fill="none"/>
                  <path d="M400 236 C380 200 420 184 400 148" stroke="#FFFFFF" stroke-opacity="0.30"
                        stroke-width="6" stroke-linecap="round" fill="none"/>
                  <path d="M458 246 C438 214 478 196 458 164" stroke="#FFFFFF" stroke-opacity="0.42"
                        stroke-width="6" stroke-linecap="round" fill="none"/>

                  <!-- 碗 -->
                  <ellipse cx="400" cy="424" rx="196" ry="26" fill="#000000" fill-opacity="0.10"/>
                  <path d="M232 304 C252 446 548 446 568 304 Z" fill="#FFFFFF" fill-opacity="0.90"/>
                  <ellipse cx="400" cy="304" rx="170" ry="40" fill="#FFFFFF"/>
                  <ellipse cx="400" cy="304" rx="146" ry="29" fill="url(#soup)"/>
                  <ellipse cx="400" cy="304" rx="146" ry="29" fill="none"
                           stroke="#FFFFFF" stroke-opacity="0.55" stroke-width="2"/>

                  <!-- 食材点缀 -->
                  <circle cx="352" cy="300" r="9" fill="#4C7A4F" fill-opacity="0.55"/>
                  <circle cx="386" cy="310" r="7" fill="#E7B24A" fill-opacity="0.65"/>
                  <circle cx="424" cy="298" r="8" fill="#D8503C" fill-opacity="0.50"/>
                  <circle cx="454" cy="310" r="6" fill="#FFFFFF" fill-opacity="0.60"/>
                  <circle cx="410" cy="288" r="5" fill="#4C7A4F" fill-opacity="0.45"/>
                  <path d="M362 292 q12 -14 26 -4" stroke="#FFFFFF" stroke-opacity="0.65"
                        stroke-width="3" fill="none" stroke-linecap="round"/>

                  <text x="62" y="548" font-size="21" fill="#FFFFFF" fill-opacity="0.62"
                        font-family="'PingFang SC','Microsoft YaHei',sans-serif" letter-spacing="3">一餐一饭，皆是手记</text>
                  <text x="738" y="548" font-size="20" fill="#FFFFFF" fill-opacity="0.42" text-anchor="end"
                        font-family="'PingFang SC','Microsoft YaHei',sans-serif" letter-spacing="2">NO.%02d</text>
                </svg>
                """.formatted(palette[0], palette[1], Math.floorMod(hash + offset, 99) + 1);
        return fileStorageService.saveSvg(svg);
    }
}
