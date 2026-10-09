package com.phoenix.ratelimit;

import com.phoenix.exception.RateLimitExceededException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Fixed-window counters in Postgres (one atomic upsert per hit). Every hit commits in its OWN
 * transaction, so counts survive even when the request that caused them fails and rolls back.
 */
@Component
public class DbRateLimiter implements RateLimiter {
    private static final String UPSERT = """
            INSERT INTO rate_limits (limit_key, window_start, hits) VALUES (?, ?, 1)
            ON CONFLICT (limit_key) DO UPDATE SET
              hits = CASE WHEN rate_limits.window_start <= ? THEN 1 ELSE rate_limits.hits + 1 END,
              window_start = CASE WHEN rate_limits.window_start <= ? THEN ? ELSE rate_limits.window_start END
            RETURNING hits, window_start
            """;
    private static final RowMapper<Hit> HIT_MAPPER =
            (rs, i) -> new Hit(rs.getInt(1), rs.getTimestamp(2).toInstant());

    private record Hit(int hits, Instant windowStart) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DbRateLimiter(JdbcTemplate jdbc, PlatformTransactionManager tm, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.tx = new TransactionTemplate(tm);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void consume(String key, RateLimit rule) {
        Hit h = hit(key, rule);
        if (h.hits() > rule.limit()) {
            throw new RateLimitExceededException("Too many requests. Please try again later.",
                    secondsUntil(h.windowStart().plus(rule.window())));
        }
    }

    @Override
    public void record(String key, RateLimit rule) {
        hit(key, rule);
    }

    @Override
    public void assertNotReached(String key, RateLimit rule) {
        List<Hit> rows = jdbc.query("SELECT hits, window_start FROM rate_limits WHERE limit_key = ?", HIT_MAPPER, trim(key));
        if (rows.isEmpty()) return;
        Hit h = rows.get(0);
        Instant end = h.windowStart().plus(rule.window());
        if (end.isAfter(clock.instant()) && h.hits() >= rule.limit()) {
            throw new RateLimitExceededException("Too many attempts. Please try again later.", secondsUntil(end));
        }
    }

    @Override
    public void reset(String key) {
        jdbc.update("DELETE FROM rate_limits WHERE limit_key = ?", trim(key));
    }

    private Hit hit(String key, RateLimit rule) {
        Instant now = clock.instant();
        Timestamp nowTs = Timestamp.from(now);
        Timestamp expiredBefore = Timestamp.from(now.minus(rule.window()));
        String k = trim(key);
        return tx.execute(s -> jdbc.queryForObject(UPSERT, HIT_MAPPER, k, nowTs, expiredBefore, expiredBefore, nowTs));
    }

    private long secondsUntil(Instant end) {
        return (Duration.between(clock.instant(), end).toMillis() + 999) / 1000;
    }

    private static String trim(String key) {
        return key.length() > 300 ? key.substring(0, 300) : key;
    }
}
