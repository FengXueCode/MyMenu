package com.fengxue.menu.recipe;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.mapper.RecipeCookLogMapper;
import com.fengxue.menu.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 真实烹饪计时：记录本次用时，并把自己菜谱的预估时长更新为实际分钟。
 */
@Service
@RequiredArgsConstructor
public class CookTimerService {

    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;
    private final RecipeCookLogMapper cookLogMapper;

    @Transactional
    public CookResult record(Long userId, Long recipeId, int durationSeconds) {
        if (durationSeconds < 1 || durationSeconds > 12 * 3600) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "计时时长不合法");
        }
        Recipe recipe = recipeService.requireVisible(userId, recipeId);

        RecipeCookLog log = new RecipeCookLog();
        log.setRecipeId(recipeId);
        log.setUserId(userId);
        log.setDurationSeconds(durationSeconds);
        log.setCreatedAt(LocalDateTime.now());
        cookLogMapper.insert(log);

        int actualMinutes = Math.max(1, (int) Math.round(durationSeconds / 60.0));
        boolean updated = false;
        // 系统示例菜谱是共享数据，只记录本次用时，不修改公共预估
        if (recipe.getUserId() != null && recipe.getUserId() != 0L
                && recipe.getUserId().equals(userId)) {
            recipe.setDurationMin(actualMinutes);
            recipe.setUpdatedAt(LocalDateTime.now());
            recipeMapper.updateById(recipe);
            updated = true;
        }
        return new CookResult(actualMinutes, actualMinutes, updated);
    }

    public record CookResult(int actualMinutes, int estimateMinutes, boolean updatedEstimate) {
    }
}
