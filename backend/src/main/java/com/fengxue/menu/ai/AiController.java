package com.fengxue.menu.ai;

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
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProviderService providerService;
    private final AiService aiService;
    private final AiImageService aiImageService;
    private final RateLimiter rateLimiter;

    @GetMapping("/providers")
    public Result<List<AiDtos.ProviderVO>> providers() {
        return Result.success(providerService.list(CurrentUser.require()));
    }

    @PostMapping("/providers")
    public Result<AiDtos.ProviderVO> createProvider(@RequestBody AiDtos.ProviderInput input) {
        return Result.success(providerService.create(CurrentUser.require(), input));
    }

    @PutMapping("/providers/{id}")
    public Result<AiDtos.ProviderVO> updateProvider(@PathVariable Long id,
                                                    @RequestBody AiDtos.ProviderInput input) {
        return Result.success(providerService.update(CurrentUser.require(), id, input));
    }

    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id) {
        providerService.delete(CurrentUser.require(), id);
        return Result.success();
    }

    @PostMapping("/providers/{id}/test")
    public Result<AiDtos.TestResult> testProvider(@PathVariable Long id) {
        Long userId = CurrentUser.require();
        rateLimiter.check("ai-test:" + userId, 10, Duration.ofMinutes(1));
        return Result.success(providerService.test(userId, id));
    }

    @GetMapping("/presets")
    public Result<List<AiDtos.Preset>> presets() {
        CurrentUser.require();
        return Result.success(ProviderPresets.all());
    }

    @PostMapping("/extract")
    public Result<AiDtos.RecipeDraft> extract(@RequestBody AiDtos.ExtractRequest request) {
        Long userId = CurrentUser.require();
        rateLimiter.check("ai-extract:" + userId, 20, Duration.ofMinutes(1));
        return Result.success(aiService.extract(userId, request));
    }

    @PostMapping("/recipes/{id}/summary")
    public Result<AiDtos.SummaryResponse> summary(@PathVariable Long id) {
        Long userId = CurrentUser.require();
        rateLimiter.check("ai-summary:" + userId, 20, Duration.ofMinutes(1));
        return Result.success(aiService.summary(userId, id));
    }

    @PostMapping("/recipes/{id}/image")
    public Result<AiDtos.ImageResponse> image(@PathVariable Long id) {
        Long userId = CurrentUser.require();
        rateLimiter.check("ai-image:" + userId, 10, Duration.ofMinutes(1));
        return Result.success(aiImageService.generate(userId, id));
    }
}
