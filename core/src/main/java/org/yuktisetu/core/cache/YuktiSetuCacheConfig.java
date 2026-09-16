package org.yuktisetu.core.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Per-cache TTLs and size caps for the read paths that run on every request.
 *
 * Caffeine rather than the default ConcurrentMapCacheManager because those caches
 * are unbounded and never expire: on a service that runs for a whole placement
 * season that is both a slow memory leak and a source of stale eligibility
 * criteria, which is the one thing that must not go stale silently.
 *
 * Each cache is declared explicitly with its own spec rather than one blanket
 * default, because the right staleness window differs. Criteria and policy are
 * evicted explicitly when the TnP edits them, so their TTL is only a backstop for
 * a missed eviction. Student-facing posting detail is read constantly and changes
 * rarely, so it gets the longest window.
 *
 * Deliberately in-process rather than Redis-backed. At this scale each service
 * runs one or two instances, so a per-instance cache costs at most a few seconds
 * of divergence between them, and it removes a network hop plus a hard dependency
 * on Redis being up from every read path.
 */
@Configuration
@EnableCaching
public class YuktiSetuCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                // Evicted explicitly on criteria update; the TTL is the backstop.
                caffeine(CacheNames.POSTING_CRITERIA, Duration.ofMinutes(10), 2_000),
                caffeine(CacheNames.POSTING_REAPPLICATION_POLICY, Duration.ofMinutes(10), 2_000),
                // Read on every applicant-pool authorization check, changes only
                // while a posting is still a draft.
                caffeine(CacheNames.POSTING_TARGET_COLLEGES, Duration.ofMinutes(15), 5_000),
                caffeine(CacheNames.POSTING_DETAIL, Duration.ofMinutes(5), 2_000)
        ));
        return manager;
    }

    private Cache caffeine(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                // Lets an operator see hit rates through the cache statistics
                // rather than guessing whether a cache is earning its keep.
                .recordStats()
                .build());
    }
}
