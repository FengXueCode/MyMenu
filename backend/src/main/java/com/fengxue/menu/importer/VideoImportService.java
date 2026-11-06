package com.fengxue.menu.importer;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VideoImportService {

    private final UrlGuard urlGuard;
    private final BilibiliClient bilibiliClient;
    private final DouyinClient douyinClient;

    public VideoMeta resolve(ImportDtos.ResolveRequest request) {
        ShareTextParser.ParsedShare parsed = request.shareText() == null ? null
                : ShareTextParser.parse(request.shareText());
        String rawUrl = request.url() != null && !request.url().isBlank()
                ? request.url()
                : (parsed == null ? null : parsed.url());
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请粘贴视频链接或完整分享文案");
        }
        String normalized = urlGuard.normalize(rawUrl);
        String host = URI.create(normalized).getHost();
        String lowerHost = host == null ? "" : host.toLowerCase(Locale.ROOT);

        VideoMeta meta;
        if (lowerHost.contains("bilibili") || lowerHost.contains("b23.tv")) {
            meta = bilibiliClient.fetch(normalized, new ArrayList<>());
        } else if (lowerHost.contains("douyin")) {
            meta = douyinClient.fetch(normalized, parsed);
        } else {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "仅支持 B 站 / 抖音分享链接");
        }
        return merge(meta, parsed, request.extraText());
    }

    private VideoMeta merge(VideoMeta meta, ShareTextParser.ParsedShare parsed, String extraText) {
        String title = meta.title();
        if ((title == null || title.isBlank()) && parsed != null && parsed.title() != null) {
            title = parsed.title();
        }
        String author = meta.author();
        if ((author == null || author.isBlank()) && parsed != null) {
            author = parsed.author();
        }
        String description = meta.description();
        if (extraText != null && !extraText.isBlank()) {
            String extra = extraText.trim();
            description = (description == null || description.isBlank() ? "" : description + "\n")
                    + "补充文案：" + extra;
        }
        return new VideoMeta(meta.platform(), meta.videoId(), title, description, meta.coverUrl(),
                author, meta.durationSec(), meta.bvid(), meta.canEmbed(), meta.transcript(),
                meta.warnings());
    }
}
