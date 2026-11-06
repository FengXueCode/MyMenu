package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.common.PageResult;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.importer.BilibiliClient;
import com.fengxue.menu.importer.UrlGuard;
import com.fengxue.menu.mapper.FavoriteSourceMapper;
import com.fengxue.menu.mapper.RecipeMapper;
import com.fengxue.menu.mapper.VideoAccountMapper;
import com.fengxue.menu.mapper.VideoImportItemMapper;
import com.fengxue.menu.recipe.Recipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private static final Pattern DOUYIN_VIDEO_ID = Pattern.compile("(?:video|share/video)/(\\d{6,})");

    private final VideoAccountMapper accountMapper;
    private final FavoriteSourceMapper sourceMapper;
    private final VideoImportItemMapper itemMapper;
    private final RecipeMapper recipeMapper;
    private final BilibiliQrService qrService;
    private final BilibiliApi bilibiliApi;
    private final UrlGuard urlGuard;

    public IntegrationDtos.StatusVO status(Long userId) {
        VideoAccount account = qrService.getAccount(userId);
        List<IntegrationDtos.SourceVO> sources = sourceMapper.selectList(
                        Wrappers.<FavoriteSource>lambdaQuery()
                                .eq(FavoriteSource::getUserId, userId)
                                .orderByAsc(FavoriteSource::getId))
                .stream().map(this::toSourceVo).toList();

        IntegrationDtos.QueueStats stats = new IntegrationDtos.QueueStats(
                count(userId, "PENDING"), count(userId, "IMPORTED"),
                count(userId, "SKIPPED"), count(userId, "FAILED"));

        List<VideoImportItem> recent = itemMapper.selectList(
                Wrappers.<VideoImportItem>lambdaQuery()
                        .eq(VideoImportItem::getUserId, userId)
                        .orderByDesc(VideoImportItem::getId)
                        .last("limit 8"));
        return new IntegrationDtos.StatusVO(toAccountVo(account), sources, stats, toItemVos(recent));
    }

    public IntegrationDtos.SourceVO addBilibiliFolder(Long userId, String folderUrl) {
        VideoAccount account = requireActiveAccount(userId);
        String mediaId = BilibiliFolderParser.parseMediaId(folderUrl);
        if (mediaId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "收藏夹链接格式不正确，应包含 fid= 数字，例如 …/favlist?fid=123456");
        }
        String title;
        try {
            JsonNode root = bilibiliApi.folderInfo(mediaId, qrService.cookieHeader(account));
            BilibiliApi.assertCode(root);
            JsonNode data = root.path("data");
            title = data.path("title").asString(data.path("name").asString("我的收藏夹"));
        } catch (BilibiliAuthException e) {
            qrService.markExpired(account);
            throw new BusinessException(ResultCode.PARAM_ERROR, e.getMessage());
        }
        FavoriteSource existing = sourceMapper.selectOne(Wrappers.<FavoriteSource>lambdaQuery()
                .eq(FavoriteSource::getUserId, userId)
                .eq(FavoriteSource::getPlatform, "bilibili")
                .eq(FavoriteSource::getFolderId, mediaId)
                .last("limit 1"));
        FavoriteSource source = existing == null ? new FavoriteSource() : existing;
        source.setUserId(userId);
        source.setPlatform("bilibili");
        source.setSourceType("BILIBILI_FOLDER");
        source.setFolderId(mediaId);
        source.setFolderName(title == null || title.isBlank() ? "B站收藏夹" : title);
        source.setSourceUrl(folderUrl == null ? null : folderUrl.trim());
        if (source.getAutoScan() == null) {
            source.setAutoScan(1);
        }
        if (source.getScanLimit() == null) {
            source.setScanLimit(20);
        }
        source.setUpdatedAt(LocalDateTime.now());
        if (source.getId() == null) {
            source.setCreatedAt(LocalDateTime.now());
            sourceMapper.insert(source);
        } else {
            sourceMapper.updateById(source);
        }
        return toSourceVo(source);
    }

    public IntegrationDtos.SourceVO updateSource(Long userId, Long sourceId,
                                                 IntegrationDtos.UpdateSourceRequest request) {
        FavoriteSource source = requireSource(userId, sourceId);
        if (request.autoScan() != null) {
            source.setAutoScan(request.autoScan() ? 1 : 0);
        }
        if (request.scanLimit() != null) {
            source.setScanLimit(Math.max(1, Math.min(100, request.scanLimit())));
        }
        source.setUpdatedAt(LocalDateTime.now());
        sourceMapper.updateById(source);
        return toSourceVo(source);
    }

    public void deleteSource(Long userId, Long sourceId) {
        requireSource(userId, sourceId);
        sourceMapper.deleteById(sourceId);
    }

    public void disconnectBilibili(Long userId) {
        qrService.disconnect(userId);
        sourceMapper.delete(Wrappers.<FavoriteSource>lambdaQuery()
                .eq(FavoriteSource::getUserId, userId)
                .eq(FavoriteSource::getPlatform, "bilibili"));
    }

    @Transactional
    public IntegrationDtos.QueueResultVO enqueueDouyin(Long userId, String text) {
        List<String> urls = UrlGuard.extractUrls(text);
        if (urls.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有识别到链接，请粘贴完整的分享文案");
        }
        FavoriteSource queueSource = queueSource(userId);
        int added = 0;
        int duplicates = 0;
        int invalid = 0;
        for (String raw : urls) {
            try {
                String normalized = urlGuard.normalize(raw);
                String host = URI.create(normalized).getHost();
                if (host == null || !host.contains("douyin")) {
                    invalid++;
                    continue;
                }
                String platform = "douyin";
                String videoId = extractDouyinId(normalized);
                if (videoId == null) {
                    videoId = hash(normalized);
                }
                VideoImportItem item = new VideoImportItem();
                item.setUserId(userId);
                item.setSourceId(queueSource.getId());
                item.setPlatform(platform);
                item.setVideoId(videoId);
                item.setVideoUrl(normalized);
                item.setDescription(text.length() > 2000 ? text.substring(0, 2000) : text);
                item.setStatus("PENDING");
                item.setAttempts(0);
                item.setCreatedAt(LocalDateTime.now());
                try {
                    itemMapper.insert(item);
                    added++;
                } catch (DuplicateKeyException e) {
                    duplicates++;
                }
            } catch (BusinessException e) {
                invalid++;
            }
        }
        return new IntegrationDtos.QueueResultVO(added, duplicates, invalid, urls.size());
    }

    public PageResult<IntegrationDtos.ItemVO> items(Long userId, String status,
                                                    long pageNum, long pageSize) {
        long safePage = Math.max(1, pageNum);
        long safeSize = Math.min(50, Math.max(1, pageSize));
        var query = Wrappers.<VideoImportItem>lambdaQuery()
                .eq(VideoImportItem::getUserId, userId);
        if (status != null && !status.isBlank()) {
            query.eq(VideoImportItem::getStatus, status);
        }
        query.orderByDesc(VideoImportItem::getId);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<VideoImportItem> page =
                itemMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        safePage, safeSize), query);
        return PageResult.of(toItemVos(page.getRecords()), page.getTotal(), safePage, safeSize);
    }

    public FavoriteSource requireSource(Long userId, Long sourceId) {
        FavoriteSource source = sourceMapper.selectById(sourceId);
        if (source == null || !source.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收藏源不存在");
        }
        return source;
    }

    public VideoAccount requireActiveAccount(Long userId) {
        VideoAccount account = qrService.getAccount(userId);
        if (account == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先在「视频账号」里扫码登录 B 站");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "B站登录已过期，请重新扫码登录");
        }
        return account;
    }

    public String cookieHeader(VideoAccount account) {
        return qrService.cookieHeader(account);
    }

    public void markExpired(VideoAccount account) {
        qrService.markExpired(account);
    }

    public IntegrationDtos.AccountVO accountVo(Long userId) {
        return toAccountVo(qrService.getAccount(userId));
    }

    public static String extractDouyinId(String url) {
        Matcher matcher = DOUYIN_VIDEO_ID.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private FavoriteSource queueSource(Long userId) {
        FavoriteSource source = sourceMapper.selectOne(Wrappers.<FavoriteSource>lambdaQuery()
                .eq(FavoriteSource::getUserId, userId)
                .eq(FavoriteSource::getSourceType, "DOUYIN_QUEUE")
                .last("limit 1"));
        if (source != null) {
            return source;
        }
        source = new FavoriteSource();
        source.setUserId(userId);
        source.setPlatform("douyin");
        source.setSourceType("DOUYIN_QUEUE");
        source.setFolderName("抖音分享链接队列");
        source.setAutoScan(1);
        source.setScanLimit(20);
        source.setLastScanStatus("IDLE");
        source.setLastScanAdded(0);
        source.setCreatedAt(LocalDateTime.now());
        source.setUpdatedAt(LocalDateTime.now());
        sourceMapper.insert(source);
        return source;
    }

    private long count(Long userId, String status) {
        Long value = itemMapper.selectCount(Wrappers.<VideoImportItem>lambdaQuery()
                .eq(VideoImportItem::getUserId, userId)
                .eq(VideoImportItem::getStatus, status));
        return value == null ? 0 : value;
    }

    private IntegrationDtos.AccountVO toAccountVo(VideoAccount account) {
        if (account == null) {
            return null;
        }
        return new IntegrationDtos.AccountVO(account.getId(), account.getPlatform(),
                account.getNickname(), account.getAvatar(), account.getStatus(),
                account.getLastLoginAt());
    }

    private IntegrationDtos.SourceVO toSourceVo(FavoriteSource source) {
        return new IntegrationDtos.SourceVO(
                source.getId(), source.getPlatform(), source.getSourceType(), source.getFolderId(),
                source.getFolderName(), source.getSourceUrl(),
                source.getAutoScan() != null && source.getAutoScan() == 1,
                source.getScanLimit() == null ? 20 : source.getScanLimit(),
                source.getLastScanStatus(), source.getLastScanAt(),
                source.getLastScanMessage(),
                source.getLastScanAdded() == null ? 0 : source.getLastScanAdded());
    }

    private List<IntegrationDtos.ItemVO> toItemVos(List<VideoImportItem> items) {
        Map<Long, String> titles = new LinkedHashMap<>();
        List<Long> recipeIds = items.stream().map(VideoImportItem::getRecipeId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (!recipeIds.isEmpty()) {
            recipeMapper.selectBatchIds(recipeIds).forEach(recipe -> titles.put(recipe.getId(), recipe.getTitle()));
        }
        List<IntegrationDtos.ItemVO> result = new ArrayList<>();
        for (VideoImportItem item : items) {
            result.add(new IntegrationDtos.ItemVO(
                    item.getId(), item.getPlatform(), item.getVideoId(), item.getVideoUrl(),
                    item.getTitle(), item.getCoverUrl(), item.getAuthor(), item.getStatus(),
                    item.getRecipeId(), titles.get(item.getRecipeId()), item.getReason(),
                    item.getCreatedAt(), item.getProcessedAt()));
        }
        return result;
    }

    private String hash(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(url.hashCode());
        }
    }
}
