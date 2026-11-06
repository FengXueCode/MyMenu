package com.fengxue.menu.importer;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.common.util.JsonSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B 站公开接口客户端：标题/简介/封面/时长，字幕尽力而为。
 */
@Component
@RequiredArgsConstructor
public class BilibiliClient {

    private static final Pattern BV_PATTERN = Pattern.compile("(BV[0-9A-Za-z]{10})");

    private final HttpFetcher fetcher;

    public static String extractBvid(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = BV_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    public VideoMeta fetch(String urlOrBvid, List<String> warnings) {
        String bvid = extractBvid(urlOrBvid);
        String finalUrl = urlOrBvid;
        if (bvid == null) {
            HttpFetcher.FetchedPage redirect = fetcher.get(urlOrBvid,
                    Map.of("Referer", "https://www.bilibili.com/"));
            finalUrl = redirect.finalUrl();
            bvid = extractBvid(finalUrl);
        }
        if (bvid == null) {
            throw new BusinessException(ResultCode.IMPORT_FAILED, "未能识别 B 站视频号");
        }

        String api = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        HttpFetcher.FetchedPage page = fetcher.get(api,
                Map.of("Referer", "https://www.bilibili.com/"));
        JsonNode root = JsonSupport.readTree(page.body());
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String message = root.path("message").asString("视频不存在或需要登录");
            throw new BusinessException(ResultCode.IMPORT_FAILED, "B站解析失败：" + message);
        }
        JsonNode data = root.path("data");
        String title = data.path("title").asString("");
        String description = data.path("desc").asString("");
        String cover = data.path("pic").asString("");
        if (cover.startsWith("http://")) {
            cover = cover.replaceFirst("http://", "https://");
        }
        String author = data.path("owner").path("name").asString("");
        int duration = data.path("duration").asInt(0);
        String cid = data.path("pages").path(0).path("cid").asString("");

        String transcript = "";
        if (!cid.isBlank()) {
            try {
                transcript = fetchSubtitle(bvid, cid);
            } catch (Exception e) {
                warnings.add("字幕获取失败，将仅根据标题和简介提取菜谱");
            }
        }
        return new VideoMeta("bilibili", bvid, title, description, cover, author, duration,
                bvid, true, transcript, new ArrayList<>(warnings));
    }

    private String fetchSubtitle(String bvid, String cid) {
        String url = "https://api.bilibili.com/x/player/v2?bvid=" + bvid + "&cid=" + cid;
        HttpFetcher.FetchedPage page = fetcher.get(url,
                Map.of("Referer", "https://www.bilibili.com/video/" + bvid));
        JsonNode root = JsonSupport.readTree(page.body());
        JsonNode subtitles = root.path("data").path("subtitle").path("subtitles");
        if (!subtitles.isArray() || subtitles.isEmpty()) {
            return "";
        }
        String subtitleUrl = subtitles.path(0).path("subtitle_url").asString("");
        if (subtitleUrl.isBlank()) {
            return "";
        }
        if (subtitleUrl.startsWith("//")) {
            subtitleUrl = "https:" + subtitleUrl;
        }
        HttpFetcher.FetchedPage subtitlePage = fetcher.get(subtitleUrl, null);
        JsonNode body = JsonSupport.readTree(subtitlePage.body()).path("body");
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : body) {
            builder.append(item.path("content").asString(""));
        }
        String text = builder.toString().replaceAll("\\s+", " ").trim();
        return text.length() > 6000 ? text.substring(0, 6000) : text;
    }
}
