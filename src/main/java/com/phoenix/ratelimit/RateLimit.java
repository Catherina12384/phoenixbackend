package com.phoenix.ratelimit;

import java.time.Duration;

/** At most {@code limit} hits per {@code window}. Bound directly from application.yml. */
public record RateLimit(int limit, Duration window) {}
