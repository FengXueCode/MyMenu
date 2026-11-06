package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.ai.AiDtos;
import com.fengxue.menu.ai.AiService;
import com.fengxue.menu.category.Category;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.importer.ImportDtos;
import com.fengxue.menu.importer.VideoImportService;
import com.fengxue.menu.importer.VideoMeta;
import com.fengxue.menu.mapper.CategoryMapper;
import com.fengxue.menu.mapper.FavoriteSourceMapper;
import com.fengxue.menu.mapper.VideoAccountMapper;
import com.fengxue.menu.mapper.VideoImportItemMapper;
import com.fengxue.menu.recipe.RecipeDtos;
import com.fengxue.menu.recipe.RecipeService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * 收藏夹扫描：B 站收藏夹 → 视频入队 → AI 判断/提取 → 生成菜谱。
 *
 * <p>扫描在独立线程池执行，source.lastScanStatus 记录 RUNNING/SUCCESS/FAILED，
 * 页面轮询状态即可。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationScanService {

    private final FavoriteSourceMapper sourceMapper;
    private final VideoImportItemMapper itemMapper;
    private final VideoAccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final BilibiliApi bilibiliApi;
    private final BilibiliQrService qrService;
    private final IntegrationService integrationService;
    private final IntegrationCoverService coverService;
    private final AiService aiService;
    private final RecipeService recipeService;
    private final VideoImportService videoImportService;

    @Resource(name = "integrationScanExecutor")
    private TaskExecutor scanExecutor;

    public IntegrationDtos.ScanStartVO startScan(Long userId, Long sourceId) {
        FavoriteSource source = integrationService.requireSource(userId, sourceId);
        if ("RUNNING".equals(source.getLastScanStatus())
                && source.getLastScanAt() != null
                && source.getLastScanAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
            return new IntegrationDtos.ScanStartVO("RUNNING", "这个收藏源正在扫描中");
        }
        source.setLastScanStatus("RUNNING");
        source.setLastScanAt(LocalDateTime.now());
        source.setLastScanMessage("正在扫描…");
        sourceMapper.updateById(source);

        // 上次异常中断留下的 PROCESSING 重置
        itemMapper.update(null, Wrappers.<VideoImportItem>lambdaUpdate()
                .eq(VideoImportItem::getUserId, userId)
                .eq(VideoImportItem::getSourceId, sourceId)
                .eq(VideoImportItem::getStatus, "PROCESSING")
                .set(VideoImportItem::getStatus, "PENDING"));

        try {
            scanExecutor.execute(() -> doScan(source.getId()));
        } catch (RejectedExecutionException e) {
            markFailed(source, "扫描任务过多，请稍后再试");
            return new IntegrationDtos.ScanStartVO("FAILED", "扫描任务过多，请稍后再试");
        }
        return new IntegrationDtos.ScanStartVO("RUNNING", "已开始扫描");
    }

    public IntegrationDtos.ScanStartVO startAll(Long userId) {
        List<FavoriteSource> sources = sourceMapper.selectList(
                Wrappers.<FavoriteSource>lambdaQuery()
                        .eq(FavoriteSource::getUserId, userId)
                        .eq(FavoriteSource::getAutoScan, 1));
        int started = 0;
        for (FavoriteSource source : sources) {
            IntegrationDtos.ScanStartVO result = startScan(userId, source.getId());
            if ("RUNNING".equals(result.status())) {
                started++;
            }
        }
        return new IntegrationDtos.ScanStartVO(started > 0 ? "RUNNING" : "IDLE",
                started > 0 ? "已开始扫描 " + started + " 个来源" : "没有可扫描的来源");
    }

    private void doScan(Long sourceId) {
        FavoriteSource source = sourceMapper.selectById(sourceId);
        if (source == null) {
            return;
        }
        int discovered = 0;
        int imported = 0;
        int skipped = 0;
        int failed = 0;
        try {
            if ("BILIBILI_FOLDER".equals(source.getSourceType())) {
                VideoAccount account = integrationService.requireActiveAccount(source.getUserId());
                discovered = discoverBilibili(source, account);
            }
            ScanOutcome outcome = processPending(source);
            imported = outcome.imported();
            skipped = outcome.skipped();
            failed = outcome.failed();
            source.setLastScanStatus("SUCCESS");
            source.setLastScanAdded(imported);
            source.setLastScanMessage(outcome.message() + "（新增视频 " + discovered + "）");
            source.setLastScanAt(LocalDateTime.now());
            sourceMapper.updateById(source);
            log.info("收藏源扫描完成: sourceId={}, discovered={}, imported={}, skipped={}, failed={}",
                    sourceId, discovered, imported, skipped, failed);
        } catch (BilibiliAuthException e) {
            markFailed(source, e.getMessage());
        } catch (BusinessException e) {
            markFailed(source, e.getMessage());
        } catch (Exception e) {
            log.warn("收藏源扫描异常: sourceId={}", sourceId, e);
            markFailed(source, "扫描异常：" + e.getClass().getSimpleName());
        }
    }

    private int discoverBilibili(FavoriteSource source, VideoAccount account) {
        if (source.getFolderId() == null || source.getFolderId().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收藏夹未配置");
        }
        String cookie = qrService.cookieHeader(account);
        int limit = source.getScanLimit() == null ? 20 : source.getScanLimit();
        int discovered = 0;
        int page = 1;
        while (discovered < limit && page <= 10) {
            JsonNode root;
            try {
                root = bilibiliApi.favList(source.getFolderId(), page, 20, cookie);
            } catch (BilibiliAuthException e) {
                integrationService.markExpired(account);
                throw e;
            }
            BilibiliApi.assertCode(root);
            JsonNode data = root.path("data");
            JsonNode medias = data.path("medias");
            if (!medias.isArray() || medias.isEmpty()) {
                break;
            }
            for (JsonNode media : medias) {
                if (discovered >= limit) {
                    break;
                }
                String bvid = media.path("bvid").asString("");
                if (bvid.isBlank()) {
                    continue;
                }
                Long exists = itemMapper.selectCount(Wrappers.<VideoImportItem>lambdaQuery()
                        .eq(VideoImportItem::getUserId, source.getUserId())
                        .eq(VideoImportItem::getPlatform, "bilibili")
                        .eq(VideoImportItem::getVideoId, bvid));
                if (exists != null && exists > 0) {
                    continue;
                }
                VideoImportItem item = new VideoImportItem();
                item.setUserId(source.getUserId());
                item.setSourceId(source.getId());
                item.setPlatform("bilibili");
                item.setVideoId(bvid);
                item.setVideoUrl("https://www.bilibili.com/video/" + bvid);
                item.setTitle(truncate(media.path("title").asString(""), 300));
                item.setCoverUrl(media.path("cover").asString(""));
                item.setAuthor(truncate(media.path("upper").path("name").asString(""), 100));
                item.setDescription(truncate(media.path("intro").asString(""), 5000));
                item.setStatus("PENDING");
                item.setAttempts(0);
                item.setCreatedAt(LocalDateTime.now());
                try {
                    itemMapper.insert(item);
                    discovered++;
                } catch (DuplicateKeyException ignored) {
                    // 并发扫描时可能重复插入，跳过
                }
            }
            if (!data.path("has_more").asBoolean(false)) {
                break;
            }
            page++;
        }
        return discovered;
    }

    private ScanOutcome processPending(FavoriteSource source) {
        int limit = source.getScanLimit() == null ? 20 : source.getScanLimit();
        List<VideoImportItem> items = itemMapper.selectList(Wrappers.<VideoImportItem>lambdaQuery()
                .eq(VideoImportItem::getUserId, source.getUserId())
                .eq(VideoImportItem::getSourceId, source.getId())
                .in(VideoImportItem::getStatus, List.of("PENDING", "FAILED"))
                .lt(VideoImportItem::getAttempts, 3)
                .orderByAsc(VideoImportItem::getId)
                .last("limit " + limit));
        int imported = 0;
        int skipped = 0;
        int failed = 0;
        boolean aiMissing = false;
        for (VideoImportItem item : items) {
            ItemOutcome outcome = processItem(item);
            switch (outcome.status()) {
                case "IMPORTED" -> imported++;
                case "SKIPPED" -> skipped++;
                case "PENDING" -> {
                    aiMissing = outcome.reason() != null && outcome.reason().contains("AI");
                    break;
                }
                default -> failed++;
            }
            if (aiMissing) {
                break;
            }
        }
        String message;
        if (imported + skipped + failed == 0 && aiMissing) {
            message = "未配置 AI 或 AI 不可用，视频已加入待处理队列";
        } else {
            message = "生成菜谱 " + imported + " 道，跳过 " + skipped + " 条，失败 " + failed + " 条";
        }
        return new ScanOutcome(imported, skipped, failed, message);
    }

    private ItemOutcome processItem(VideoImportItem item) {
        item.setStatus("PROCESSING");
        item.setAttempts((item.getAttempts() == null ? 0 : item.getAttempts()) + 1);
        itemMapper.updateById(item);
        try {
            if (item.getPlatform().equals("douyin")
                    && (item.getTitle() == null || item.getTitle().isBlank())) {
                resolveDouyin(item);
            }
            if (item.getTitle() == null || item.getTitle().isBlank()) {
                return finish(item, "FAILED", null, "没有拿到视频标题，无法识别");
            }
            AiDtos.VideoRecipeDecision decision = aiService.decideVideoRecipe(
                    item.getUserId(), item.getTitle(), item.getDescription());
            if (!decision.isRecipe()) {
                return finish(item, "SKIPPED", null,
                        decision.reason() == null ? "不是菜谱视频" : decision.reason());
            }
            AiDtos.RecipeDraft draft = decision.recipe();
            if (draft == null || draft.ingredients() == null || draft.ingredients().isEmpty()
                    || draft.steps() == null || draft.steps().isEmpty()) {
                return finish(item, "FAILED", null, "AI 认为是菜谱，但未提取到完整配料/步骤");
            }
            Long recipeId = createRecipe(item, draft);
            return finish(item, "IMPORTED", recipeId, decision.reason());
        } catch (BusinessException e) {
            if (e.getCode() == ResultCode.AI_NOT_CONFIGURED.getCode()) {
                // AI 未配置不算视频失败，不要消耗重试次数
                item.setAttempts(Math.max(0, (item.getAttempts() == null ? 1 : item.getAttempts()) - 1));
                item.setStatus("PENDING");
                item.setReason("请先配置 AI 模型");
                item.setProcessedAt(LocalDateTime.now());
                itemMapper.updateById(item);
                return new ItemOutcome("PENDING", "请先配置 AI 模型");
            }
            return finish(item, "FAILED", null, truncate(e.getMessage(), 480));
        } catch (BilibiliAuthException e) {
            return finish(item, "FAILED", null, e.getMessage());
        } catch (Exception e) {
            log.warn("视频处理失败: itemId={}", item.getId(), e);
            return finish(item, "FAILED", null, "处理失败：" + e.getClass().getSimpleName());
        }
    }

    private void resolveDouyin(VideoImportItem item) {
        ImportDtos.ResolveRequest request = new ImportDtos.ResolveRequest(
                item.getVideoUrl(), item.getDescription(), null);
        VideoMeta meta = videoImportService.resolve(request);
        if (meta.title() != null && !meta.title().isBlank()) {
            item.setTitle(truncate(meta.title(), 300));
        }
        if (item.getDescription() == null || item.getDescription().isBlank()) {
            item.setDescription(truncate(meta.description(), 5000));
        }
        if (item.getCoverUrl() == null || item.getCoverUrl().isBlank()) {
            item.setCoverUrl(meta.coverUrl());
        }
        if (item.getAuthor() == null || item.getAuthor().isBlank()) {
            item.setAuthor(truncate(meta.author(), 100));
        }
        itemMapper.updateById(item);
    }

    private Long createRecipe(VideoImportItem item, AiDtos.RecipeDraft draft) {
        Long categoryId = findCategoryId(item.getUserId(), draft.categoryName());
        String cover = coverService.store(item.getCoverUrl());
        List<RecipeDtos.IngredientDto> ingredients = new ArrayList<>();
        for (AiDtos.IngredientDraft ingredient : draft.ingredients()) {
            ingredients.add(new RecipeDtos.IngredientDto(null, ingredient.name(),
                    ingredient.amount(), ingredient.unit(), null, null));
        }
        List<RecipeDtos.StepDto> steps = new ArrayList<>();
        int stepNo = 1;
        for (String content : draft.steps()) {
            steps.add(new RecipeDtos.StepDto(null, stepNo++, content, null));
        }
        RecipeDtos.RecipeUpsertRequest request = new RecipeDtos.RecipeUpsertRequest(
                draft.title() == null || draft.title().isBlank() ? item.getTitle() : draft.title(),
                draft.description(),
                categoryId,
                cover,
                item.getPlatform(),
                item.getVideoUrl(),
                item.getAuthor(),
                draft.cookMinutes(),
                draft.difficulty(),
                draft.servings(),
                draft.meatType(),
                draft.spiceLevel(),
                draft.tags(),
                ingredients,
                steps);
        return recipeService.create(item.getUserId(), request).id();
    }

    private Long findCategoryId(Long userId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        List<Category> categories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId))
                .eq(Category::getName, categoryName.trim())
                .orderByAsc(Category::getIsSystem)
                .last("limit 1"));
        return categories.isEmpty() ? null : categories.get(0).getId();
    }

    private ItemOutcome finish(VideoImportItem item, String status, Long recipeId, String reason) {
        item.setStatus(status);
        item.setRecipeId(recipeId);
        item.setReason(truncate(reason, 480));
        item.setProcessedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return new ItemOutcome(status, reason);
    }

    private void markFailed(FavoriteSource source, String message) {
        source.setLastScanStatus("FAILED");
        source.setLastScanMessage(truncate(message, 480));
        source.setLastScanAt(LocalDateTime.now());
        sourceMapper.updateById(source);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private record ScanOutcome(int imported, int skipped, int failed, String message) {
    }

    private record ItemOutcome(String status, String reason) {
    }
}
