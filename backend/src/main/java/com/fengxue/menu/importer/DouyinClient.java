package com.fengxue.menu.importer;

import com.fengxue.menu.common.util.JsonSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音解析：优先读取分享页 meta / _ROUTER_DATA；失败时用分享文案兜底。
 *
 * <p>抖音网页有反爬，解析失败是正常分支，必须返回可继续操作的部分结果和 warnings。</p>
 */
@Component
@RequiredArgsConstructor
public class DouyinClient {

    private static final Pattern VIDEO_ID = Pattern.compile("(?:video|share/video)/(\\d{6,})");
    private static final Pattern ROUTER_DATA = Pattern.compile(
            "_ROUTER_DATA\\s*=\\s*(\\{.*?\\})\\s*</script>", Pattern.DOTALL);
    private static final Pattern META_TITLE = Pattern.compile(
            "<meta[^>]+property=\"og:title\"[^>]+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_IMAGE = Pattern.compile(
            "<meta[^>]+property=\"og:image\"[^>]+content=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private final HttpFetcher fetcher;

    public VideoMeta fetch(String url, ShareTextParser.ParsedShare parsed) {
        List<String> warnings = new ArrayList<>();
        String videoId = null;
        String finalUrl = url;
        String title = parsed == null ? null : parsed.title();
        String author = parsed == null ? null : parsed.author();
        String description = null;
        String cover = null;

        try {
            HttpFetcher.FetchedPage page = fetcher.get(url,
                    Map.of("Referer", "https://www.douyin.com/"));
            finalUrl = page.finalUrl();
            videoId = extractVideoId(finalUrl);
            cover = firstMatch(META_IMAGE, page.body());
        } catch (Exception e) {
            warnings.add("抖音分享页访问受限，已使用分享文案中的信息");
        }

        if (videoId == null) {
            videoId = extractVideoId(url);
        }

        if (videoId != null) {
            try {
                String sharePage = "https://www.iesdouyin.com/share/video/" + videoId + "/";
                HttpFetcher.FetchedPage page = fetcher.get(sharePage,
                        Map.of("Referer", "https://www.douyin.com/"));
                JsonNode info = findRouterData(page.body());
                if (info != null) {
                    title = firstNonBlank(title, info.path("desc").asString(""));
                    author = firstNonBlank(author, info.path("author").path("nickname").asString(""));
                    description = info.path("desc").asString("");
                    JsonNode coverNode = info.path("video").path("cover");
                    if (coverNode.isArray() && !coverNode.isEmpty()) {
                        cover = coverNode.path(0).asString(cover);
                    } else if (coverNode.isObject()) {
                        JsonNode urls = coverNode.path("url_list");
                        if (urls.isArray() && !urls.isEmpty()) {
                            cover = urls.path(0).asString(cover);
                        }
                    }
                } else {
                    title = firstNonBlank(title, firstMatch(META_TITLE, page.body()));
                }
            } catch (Exception e) {
                warnings.add("抖音视频详情需要登录，已跳过详情抓取");
            }
        }

        if (title == null || title.isBlank()) {
            warnings.add("未能获取视频标题，请粘贴视频文案或手动填写菜名");
        }
        if (cover != null && cover.startsWith("//")) {
            cover = "https:" + cover;
        }
        return new VideoMeta("douyin", videoId, title, description, cover, author, null,
                null, false, "", warnings);
    }

    private String extractVideoId(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = VIDEO_ID.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private JsonNode findRouterData(String html) {
        Matcher matcher = ROUTER_DATA.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        try {
            JsonNode root = JsonSupport.readTree(matcher.group(1));
            return searchItem(root);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode searchItem(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject() && node.has("desc") && node.has("video")) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = searchItem(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String firstMatch(Pattern pattern, String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null || second.isBlank() ? null : second);
    }
}
