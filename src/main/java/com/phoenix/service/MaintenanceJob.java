package com.phoenix.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly cleanup of expired OTPs, abandoned signups and stale rate-limit counters. */
@Component
public class MaintenanceJob {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceJob.class);
    private final JdbcTemplate jdbc;

    public MaintenanceJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "0 15 * * * *")
    public void purgeExpired() {
        try {
            int otps = jdbc.update("DELETE FROM otp_challenges WHERE expires_at < now() - interval '1 hour'");
            int pending = jdbc.update("DELETE FROM pending_registrations WHERE expires_at < now() - interval '1 hour'");
            int limits = jdbc.update("DELETE FROM rate_limits WHERE window_start < now() - interval '1 day'");
            log.info("Maintenance: removed {} OTPs, {} pending signups, {} rate-limit rows", otps, pending, limits);
        } catch (RuntimeException e) {
            log.error("Maintenance job failed: {}", e.getClass().getSimpleName());
        }
    }
}
