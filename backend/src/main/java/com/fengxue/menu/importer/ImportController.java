package com.fengxue.menu.importer;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import com.fengxue.menu.common.util.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final VideoImportService videoImportService;
    private final RateLimiter rateLimiter;

    @PostMapping("/resolve")
    public Result<VideoMeta> resolve(@Valid @RequestBody ImportDtos.ResolveRequest request) {
        Long userId = CurrentUser.require();
        rateLimiter.check("import:" + userId, 10, Duration.ofMinutes(1));
        return Result.success(videoImportService.resolve(request));
    }
}
