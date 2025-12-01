package main.back.utils.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.logging.Logger;

@ApplicationScoped
public class CacheStatisticsService {

    private static final Logger logger = Logger.getLogger(CacheStatisticsService.class.getName());

    private boolean statisticsEnabled = true;

    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("myDb");

    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void enableStatistics() {
        this.statisticsEnabled = true;
        getStatistics().setStatisticsEnabled(true);
        logger.info("L2 Cache statistics enabled");
    }

    public void disableStatistics() {
        this.statisticsEnabled = false;
        getStatistics().setStatisticsEnabled(false);
        logger.info("L2 Cache statistics disabled");
    }

    public Statistics getStatistics() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    public String getCacheStatistics() {
        Statistics stats = getStatistics();
        return String.format(
                "L2 Cache Statistics: Hits=%d, Misses=%d, PutCount=%d | Query Cache: Hits=%d, Misses=%d",
                stats.getSecondLevelCacheHitCount(),
                stats.getSecondLevelCacheMissCount(),
                stats.getSecondLevelCachePutCount(),
                stats.getQueryCacheHitCount(),
                stats.getQueryCacheMissCount()
        );
    }

    public void clearCache() {
        emf.getCache().evictAll();
        logger.info("L2 Cache cleared");
    }
}
