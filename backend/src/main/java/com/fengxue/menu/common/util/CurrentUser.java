package com.fengxue.menu.common.util;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;

/**
 * 当前请求用户（由 {@code AuthInterceptor} 写入，请求结束清理）。
 */
public final class CurrentUser {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static Long require() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
