package com.fengxue.menu.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作工具类。
 *
 * <p>底层使用 {@link RedisTemplate}：key 为 String，value 为 Jackson JSON。
 * 复杂对象可直接存取，但建议只缓存本工程包内的 DTO/实体，避免跨版本反序列化问题。</p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存，永不过期。
     */
    public void set(String key, Object value) {
        Assert.hasText(key, "Redis key 不能为空");
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入缓存并设置过期时间。
     */
    public void set(String key, Object value, Duration timeout) {
        Assert.hasText(key, "Redis key 不能为空");
        Assert.notNull(timeout, "过期时间不能为空");
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 仅当 key 不存在时写入，返回是否写入成功。
     */
    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        Assert.hasText(key, "Redis key 不能为空");
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 读取缓存，不存在返回 null。
     */
    public Object get(String key) {
        Assert.hasText(key, "Redis key 不能为空");
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个 key。
     */
    public Boolean delete(String key) {
        Assert.hasText(key, "Redis key 不能为空");
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除 key，返回实际删除数量。
     */
    public Long delete(Collection<String> keys) {
        Assert.notEmpty(keys, "Redis keys 不能为空");
        return redisTemplate.delete(keys);
    }

    /**
     * 判断 key 是否存在。
     */
    public Boolean hasKey(String key) {
        Assert.hasText(key, "Redis key 不能为空");
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间。
     */
    public Boolean expire(String key, Duration timeout) {
        Assert.hasText(key, "Redis key 不能为空");
        Assert.notNull(timeout, "过期时间不能为空");
        return redisTemplate.expire(key, timeout);
    }

    /**
     * 获取剩余过期时间（秒）。-1 表示永不过期，-2 表示 key 不存在。
     */
    public Long getExpire(String key) {
        Assert.hasText(key, "Redis key 不能为空");
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 写入 Hash 字段。
     */
    public void hSet(String key, String field, Object value) {
        Assert.hasText(key, "Redis key 不能为空");
        Assert.hasText(field, "Redis hash field 不能为空");
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 读取 Hash 字段。
     */
    public Object hGet(String key, String field) {
        Assert.hasText(key, "Redis key 不能为空");
        Assert.hasText(field, "Redis hash field 不能为空");
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 读取整个 Hash。
     */
    public Map<String, Object> hGetAll(String key) {
        Assert.hasText(key, "Redis key 不能为空");
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, Object> result = new LinkedHashMap<>(entries.size());
        entries.forEach((field, value) -> result.put(String.valueOf(field), value));
        return result;
    }

    /**
     * 删除 Hash 字段。
     */
    public Long hDelete(String key, Object... fields) {
        Assert.hasText(key, "Redis key 不能为空");
        Assert.notEmpty(fields, "Redis hash fields 不能为空");
        return redisTemplate.opsForHash().delete(key, fields);
    }
}
