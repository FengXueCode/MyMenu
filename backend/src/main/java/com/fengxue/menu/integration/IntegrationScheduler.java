package com.fengxue.menu.integration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.mapper.FavoriteSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日扫描收藏夹/待处理链接。
 *
 * <p>默认每天 06:00（Asia/Shanghai），可用 app.integration.scan-cron 覆盖。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationScheduler {

    private final FavoriteSourceMapper sourceMapper;
    private final IntegrationScanService scanService;

    @Scheduled(cron = "${app.integration.scan-cron:0 0 6 * * *}", zone = "Asia/Shanghai")
    public void dailyScan() {
        List<FavoriteSource> sources = sourceMapper.selectList(
                Wrappers.<FavoriteSource>lambdaQuery().eq(FavoriteSource::getAutoScan, 1));
        log.info("每日视频收藏扫描开始: sources={}", sources.size());
        for (FavoriteSource source : sources) {
            try {
                scanService.startScan(source.getUserId(), source.getId());
            } catch (Exception e) {
                log.warn("启动收藏源扫描失败: sourceId={}", source.getId(), e);
            }
        }
    }
}
