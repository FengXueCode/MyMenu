package com.fengxue.menu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis 序列化配置。
 *
 * <p>Spring Boot 4 默认使用 Jackson 3（tools.jackson），因此这里使用
 * {@link GenericJacksonJsonRedisSerializer}，而不是 Jackson 2 时代的
 * GenericJackson2JsonRedisSerializer。</p>
 *
 * <p>key 使用 UTF-8 字符串，value 使用带类型信息的 JSON；类型校验器只放行
 * 本工程、JDK 集合和 java.time 类型，避免反序列化任意类。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = StringRedisSerializer.UTF_8;

        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.fengxue.menu.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build();

        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
