package com.finova.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * Two-tier caching strategy.
 *
 * <ul>
 *   <li><b>Redis</b> ({@link #cacheManager}, primary): shared across app instances so a cached
 *       balance stays correct in a horizontally-scaled deployment. Used for account balances.</li>
 *   <li><b>Caffeine</b> ({@code caffeineCacheManager}): fast in-process cache for small, rarely
 *       changing reference data (e.g. currency metadata) where cross-instance consistency
 *       does not matter.</li>
 * </ul>
 *
 * Values are JSON-serialised so cached entries are human-inspectable in Redis during debugging.
 */
@Configuration
public class CacheConfig {

    public static final String CACHE_ACCOUNT_BALANCE = "accountBalance";

    @Value("${finova.cache.balance-ttl-seconds:60}")
    private long balanceTtlSeconds;

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        RedisCacheConfiguration balanceConfig = base.entryTtl(Duration.ofSeconds(balanceTtlSeconds));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withCacheConfiguration(CACHE_ACCOUNT_BALANCE, balanceConfig)
                .build();
    }

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(30)));
        return manager;
    }
}
