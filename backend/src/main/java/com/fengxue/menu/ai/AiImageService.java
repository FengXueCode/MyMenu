package com.fengxue.menu.ai;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.file.FileStorageService;
import com.fengxue.menu.recipe.CoverService;
import com.fengxue.menu.recipe.RecipeDtos;
import com.fengxue.menu.recipe.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiImageService {

    private final AiProviderService providerService;
    private final AiClient aiClient;
    private final RecipeService recipeService;
    private final CoverService coverService;
    private final FileStorageService fileStorageService;

    public AiDtos.ImageResponse generate(Long userId, Long recipeId) {
        RecipeDtos.RecipeDetailVO recipe = recipeService.detail(userId, recipeId);
        AiProvider provider = providerService.requireDefault(userId);
        if (provider.getImageModel() == null || provider.getImageModel().isBlank()) {
            return fallback(recipe, "当前 AI 配置未设置图像模型，已生成风格化封面");
        }
        try {
            JsonNode response = aiClient.images(provider, providerService.apiKeyOf(provider),
                    PromptTemplates.imagePrompt(recipe.title(), recipe.meatType()));
            JsonNode data = response.path("data").path(0);
            String base64 = data.path("b64_json").asString("");
            byte[] bytes;
            if (!base64.isBlank()) {
                bytes = Base64.getDecoder().decode(base64);
            } else {
                String url = data.path("url").asString("");
                if (url.isBlank()) {
                    throw new BusinessException(ResultCode.AI_CALL_FAILED, "AI 未返回图片数据");
                }
                bytes = aiClient.download(url);
            }
            String coverUrl = fileStorageService.saveImage(bytes, "png");
            recipeService.updateCover(recipeId, coverUrl);
            return new AiDtos.ImageResponse(coverUrl, true, null);
        } catch (BusinessException e) {
            log.warn("AI 配图失败，使用本地兜底封面: recipeId={}, message={}", recipeId, e.getMessage());
            return fallback(recipe, "AI 配图失败，已生成风格化封面：" + e.getMessage());
        } catch (Exception e) {
            log.warn("AI 配图异常，使用本地兜底封面: recipeId={}", recipeId, e);
            return fallback(recipe, "AI 配图异常，已生成风格化封面");
        }
    }

    private AiDtos.ImageResponse fallback(RecipeDtos.RecipeDetailVO recipe, String warning) {
        String cover = coverService.generate(recipe.title(), recipe.meatType(),
                recipe.tags() == null ? null : String.join(" / ", recipe.tags()));
        recipeService.updateCover(recipe.id(), cover);
        return new AiDtos.ImageResponse(cover, false, warning);
    }
}
