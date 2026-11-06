package com.fengxue.menu.integration;

import com.fengxue.menu.common.PageResult;
import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import com.fengxue.menu.common.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final IntegrationScanService scanService;
    private final BilibiliQrService qrService;
    private final RateLimiter rateLimiter;

    @GetMapping("/status")
    public Result<IntegrationDtos.StatusVO> status() {
        return Result.success(integrationService.status(CurrentUser.require()));
    }

    @PostMapping("/bilibili/qr")
    public Result<IntegrationDtos.QrStartVO> startQr() {
        Long userId = CurrentUser.require();
        rateLimiter.check("bili-qr:" + userId, 10, Duration.ofMinutes(1));
        BilibiliQrService.QrStart start = qrService.start(userId);
        return Result.success(new IntegrationDtos.QrStartVO(start.key(), start.qrImage()));
    }

    @GetMapping("/bilibili/qr/{key}")
    public Result<BilibiliQrService.QrPoll> pollQr(@PathVariable String key) {
        return Result.success(qrService.poll(CurrentUser.require(), key));
    }

    @DeleteMapping("/bilibili")
    public Result<Void> disconnectBilibili() {
        integrationService.disconnectBilibili(CurrentUser.require());
        return Result.success();
    }

    @PostMapping("/bilibili/folder")
    public Result<IntegrationDtos.SourceVO> addBilibiliFolder(
            @RequestBody IntegrationDtos.AddFolderRequest request) {
        return Result.success(integrationService.addBilibiliFolder(
                CurrentUser.require(), request.url()));
    }

    @PutMapping("/sources/{id}")
    public Result<IntegrationDtos.SourceVO> updateSource(
            @PathVariable Long id, @RequestBody IntegrationDtos.UpdateSourceRequest request) {
        return Result.success(integrationService.updateSource(CurrentUser.require(), id, request));
    }

    @DeleteMapping("/sources/{id}")
    public Result<Void> deleteSource(@PathVariable Long id) {
        integrationService.deleteSource(CurrentUser.require(), id);
        return Result.success();
    }

    @PostMapping("/sources/{id}/scan")
    public Result<IntegrationDtos.ScanStartVO> scanSource(@PathVariable Long id) {
        Long userId = CurrentUser.require();
        rateLimiter.check("scan:" + userId, 6, Duration.ofMinutes(1));
        return Result.success(scanService.startScan(userId, id));
    }

    @PostMapping("/scan")
    public Result<IntegrationDtos.ScanStartVO> scanAll() {
        Long userId = CurrentUser.require();
        rateLimiter.check("scan-all:" + userId, 3, Duration.ofMinutes(1));
        return Result.success(scanService.startAll(userId));
    }

    @PostMapping("/douyin/queue")
    public Result<IntegrationDtos.QueueResultVO> enqueueDouyin(
            @RequestBody IntegrationDtos.QueueRequest request) {
        Long userId = CurrentUser.require();
        rateLimiter.check("queue:" + userId, 20, Duration.ofMinutes(1));
        return Result.success(integrationService.enqueueDouyin(userId, request.text()));
    }

    @GetMapping("/items")
    public Result<PageResult<IntegrationDtos.ItemVO>> items(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(integrationService.items(
                CurrentUser.require(), status, pageNum, pageSize));
    }
}
