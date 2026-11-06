package com.fengxue.menu.importer;

import java.util.List;

/**
 * 视频解析结果，字段与 docs/api-contract.md 对齐。
 */
public record VideoMeta(
        String platform,
        String videoId,
        String title,
        String description,
        String coverUrl,
        String author,
        Integer durationSec,
        String bvid,
        boolean canEmbed,
        String transcript,
        List<String> warnings) {
}
