#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Component
@SuppressWarnings("unused")
public class CacheUtils implements ApplicationContextAware {
    public static final String IN_PROCESS_SHORT_CACHE_MANAGER = "inProcessShortCacheManager";
    public static final String IN_PROCESS_MIDDLE_CACHE_MANAGER = "inProcessMiddleCacheManager";
    public static final String IN_PROCESS_LONG_CACHE_MANAGER = "inProcessLongCacheManager";
    public static final String LONG_PERIOD_CACHE_NAME = "longPeriod";
    public static final String MIDDLE_PERIOD_CACHE_NAME = "middlePeriod";
    public static final String SHORT_PERIOD_CACHE_NAME = "shortPeriod";
    public static final String AI_NEWS_QUERY_CACHE_NAME = "aiNewsQueryCache";
    public static final String KEY_GENERATOR = "fullNameKeyGenerator";

    private static CacheManager defaultCacheManager;
    private static CacheManager inProcessShortCacheManager;
    private static CacheManager inProcessMiddleCacheManager;
    private static CacheManager inProcessLongCacheManager;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        defaultCacheManager = applicationContext.getBean(CacheManager.class);
        inProcessShortCacheManager = applicationContext.getBean(IN_PROCESS_SHORT_CACHE_MANAGER, CacheManager.class);
        inProcessMiddleCacheManager = applicationContext.getBean(IN_PROCESS_MIDDLE_CACHE_MANAGER, CacheManager.class);
        inProcessLongCacheManager = applicationContext.getBean(IN_PROCESS_LONG_CACHE_MANAGER, CacheManager.class);
    }

    public static void cacheEvict(String cacheName, List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }

        Optional.ofNullable(defaultCacheManager)
                .map(manager -> manager.getCache(cacheName))
                .ifPresent(cache -> {
                    for (String key : keys) {
                        if (StringUtils.isNotBlank(key)) {
                            if ("*".equals(key)) {
                                cache.clear();
                            } else {
                                cache.evict(key);
                            }
                        }
                    }
                });
    }

    public static void longPeriodCacheEvict(List<String> keys) {
        cacheEvict(LONG_PERIOD_CACHE_NAME, keys);
    }

    public static void middlePeriodCacheEvict(List<String> keys) {
        cacheEvict(MIDDLE_PERIOD_CACHE_NAME, keys);
    }

    public static void shortPeriodCacheEvict(List<String> keys) {
        cacheEvict(SHORT_PERIOD_CACHE_NAME, keys);
    }

    public static void aiNewsQueryCacheEvict(List<String> keys) {
        cacheEvict(AI_NEWS_QUERY_CACHE_NAME, keys);
    }

    public static Cache inProcessShortPeriodCache() {
        return Optional.ofNullable(inProcessShortCacheManager)
                .map(manager -> manager.getCache(SHORT_PERIOD_CACHE_NAME))
                .orElse(null);
    }

    public static Cache inProcessMiddlePeriodCache() {
        return Optional.ofNullable(inProcessMiddleCacheManager)
                .map(manager -> manager.getCache(MIDDLE_PERIOD_CACHE_NAME))
                .orElse(null);
    }

    public static Cache inProcessLongPeriodCache() {
        return Optional.ofNullable(inProcessLongCacheManager)
                .map(manager -> manager.getCache(LONG_PERIOD_CACHE_NAME))
                .orElse(null);
    }

    public static Cache defaultShortPeriodCache() {
        return Optional.ofNullable(defaultCacheManager)
                .map(manager -> manager.getCache(SHORT_PERIOD_CACHE_NAME))
                .orElse(null);
    }

    public static Cache defaultMiddlePeriodCache() {
        return Optional.ofNullable(defaultCacheManager)
                .map(manager -> manager.getCache(MIDDLE_PERIOD_CACHE_NAME))
                .orElse(null);
    }

    public static Cache defaultLongPeriodCache() {
        return Optional.ofNullable(defaultCacheManager)
                .map(manager -> manager.getCache(LONG_PERIOD_CACHE_NAME))
                .orElse(null);
    }
}
