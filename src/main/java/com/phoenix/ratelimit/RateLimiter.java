package com.phoenix.ratelimit;

public interface RateLimiter {
    /** Counts one hit and throws {@link com.phoenix.exception.RateLimitExceededException} if it exceeds the limit. */
    void consume(String key, RateLimit rule);

    /** Counts one hit without throwing (e.g. a failed login). */
    void record(String key, RateLimit rule);

    /** Throws if the limit has already been reached; counts nothing. */
    void assertNotReached(String key, RateLimit rule);

    void reset(String key);
}
