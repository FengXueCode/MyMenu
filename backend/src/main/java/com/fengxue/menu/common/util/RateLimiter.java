package com.fengxue.menu.common.util;

import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量内存限流器：滑动窗口计数，单机足够；Redis 不可用也不会影响可用性。
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    /**
     * 在 {@code window} 内最多允许 {@code limit} 次调用，超过抛 429。
     */
    public void check(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        Deque<Long> queue = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                throw new BusinessException(ResultCode.RATE_LIMITED);
            }
            queue.addLast(now);
        }
    }
}
