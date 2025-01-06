#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

import ${package}.service.util.CacheUtils.*;

@Configuration
@SuppressWarnings("unused")
public class CacheConfig {
    @Value("${symbol_dollar}{cache.redisson.config}")
    private String config;

    @Bean
    @Primary
    CacheManager distributedCacheManager(RedissonClient redissonClient) {
        return new RedissonSpringCacheManager(redissonClient, config);
    }

    @Bean(name = IN_PROCESS_SHORT_CACHE_MANAGER)
    public CacheManager inProcessShortCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(SHORT_PERIOD_CACHE_NAME);
        cacheManager.setAllowNullValues(false);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .expireAfterAccess(5, TimeUnit.SECONDS));
        return cacheManager;
    }

    @Bean(name = IN_PROCESS_MIDDLE_CACHE_MANAGER)
    public CacheManager inProcessMiddleCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(MIDDLE_PERIOD_CACHE_NAME);
        cacheManager.setAllowNullValues(false);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES));
        return cacheManager;
    }

    @Bean(name = IN_PROCESS_LONG_CACHE_MANAGER)
    public CacheManager inProcessLongCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(LONG_PERIOD_CACHE_NAME);
        cacheManager.setAllowNullValues(false);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .expireAfterAccess(30, TimeUnit.MINUTES));
        return cacheManager;
    }
}
