package com.fengxue.menu.integration;

import java.time.LocalDateTime;
import java.util.List;

public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    public record AccountVO(Long id, String platform, String nickname, String avatar,
                            String status, LocalDateTime lastLoginAt) {
    }

    public record SourceVO(Long id, String platform, String sourceType, String folderId,
                           String folderName, String sourceUrl, boolean autoScan, int scanLimit,
                           String lastScanStatus, LocalDateTime lastScanAt,
                           String lastScanMessage, int lastScanAdded) {
    }

    public record ItemVO(Long id, String platform, String videoId, String videoUrl,
                         String title, String coverUrl, String author, String status,
                         Long recipeId, String recipeTitle, String reason,
                         LocalDateTime createdAt, LocalDateTime processedAt) {
    }

    public record QueueStats(long pending, long imported, long skipped, long failed) {
    }

    public record StatusVO(AccountVO bilibili, List<SourceVO> sources,
                           QueueStats queue, List<ItemVO> recentItems) {
    }

    public record AddFolderRequest(String url) {
    }

    public record UpdateSourceRequest(Boolean autoScan, Integer scanLimit) {
    }

    public record QueueRequest(String text) {
    }

    public record QrStartVO(String key, String qrImage) {
    }

    public record QrPollVO(String status, AccountVO account, String message) {
    }

    public record ScanStartVO(String status, String message) {
    }

    public record QueueResultVO(int added, int duplicates, int invalid, int total) {
    }
}
