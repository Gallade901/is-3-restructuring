package main.back.utils.cache;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.hibernate.stat.Statistics;

import java.util.logging.Logger;

@Interceptor
@CacheStatisticsLogging
@Priority(Interceptor.Priority.APPLICATION)
public class CacheStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    @Inject
    private CacheStatisticsService cacheStatisticsService;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        if (!cacheStatisticsService.isStatisticsEnabled()) {
            return context.proceed();
        }

        Statistics statistics = cacheStatisticsService.getStatistics();

        // Сохраняем начальные значения
        long secondLevelCacheHitCountBefore = statistics.getSecondLevelCacheHitCount();
        long secondLevelCacheMissCountBefore = statistics.getSecondLevelCacheMissCount();
        long queryCacheHitCountBefore = statistics.getQueryCacheHitCount();
        long queryCacheMissCountBefore = statistics.getQueryCacheMissCount();

        try {
            Object result = context.proceed();

            // Логируем статистику после выполнения
            logCacheStats(context.getMethod().getName(),
                    secondLevelCacheHitCountBefore,
                    secondLevelCacheMissCountBefore,
                    queryCacheHitCountBefore,
                    queryCacheMissCountBefore,
                    statistics);

            return result;
        } catch (Exception e) {
            logger.warning("Ошибка при выполнении метода: " + context.getMethod().getName() + " - " + e.getMessage());
            throw e;
        }
    }

    private void logCacheStats(String methodName,
                               long l2HitBefore, long l2MissBefore,
                               long queryHitBefore, long queryMissBefore,
                               Statistics statistics) {

        long l2HitAfter = statistics.getSecondLevelCacheHitCount();
        long l2MissAfter = statistics.getSecondLevelCacheMissCount();
        long queryHitAfter = statistics.getQueryCacheHitCount();
        long queryMissAfter = statistics.getQueryCacheMissCount();

        long l2Hits = l2HitAfter - l2HitBefore;
        long l2Misses = l2MissAfter - l2MissBefore;
        long queryHits = queryHitAfter - queryHitBefore;
        long queryMisses = queryMissAfter - queryMissBefore;

        if (l2Hits > 0 || l2Misses > 0 || queryHits > 0 || queryMisses > 0) {
            String logMessage = String.format(
                    "Cache Statistics for %s: L2 Cache [Hits: %d, Misses: %d], Query Cache [Hits: %d, Misses: %d]",
                    methodName, l2Hits, l2Misses, queryHits, queryMisses
            );
            logger.info(logMessage);
        }
    }
}