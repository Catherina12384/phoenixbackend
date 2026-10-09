package com.phoenix.otp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Component
public class JdbcOtpStore implements OtpStore {
    private static final String COLUMNS = "id, code_hash, attempts, created_at, expires_at";
    private static final RowMapper<StoredOtp> MAPPER = (rs, i) -> new StoredOtp(
            rs.getLong("id"), rs.getString("code_hash"), rs.getInt("attempts"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("expires_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcOtpStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean saveUnlessCoolingDown(OtpPurpose purpose, String phone, String codeHash,
                                         Instant createdAt, Instant expiresAt, Instant cooldownThreshold) {
        int rows = jdbc.update("""
                INSERT INTO otp_challenges (purpose, phone, code_hash, attempts, created_at, expires_at)
                VALUES (?, ?, ?, 0, ?, ?)
                ON CONFLICT (purpose, phone) DO UPDATE SET
                  code_hash = EXCLUDED.code_hash, attempts = 0,
                  created_at = EXCLUDED.created_at, expires_at = EXCLUDED.expires_at
                WHERE otp_challenges.created_at <= ?
                """,
                purpose.name(), phone, codeHash, Timestamp.from(createdAt), Timestamp.from(expiresAt),
                Timestamp.from(cooldownThreshold));
        return rows == 1;
    }

    @Override
    public Optional<StoredOtp> find(OtpPurpose purpose, String phone) {
        return jdbc.query("SELECT " + COLUMNS + " FROM otp_challenges WHERE purpose = ? AND phone = ?",
                MAPPER, purpose.name(), phone).stream().findFirst();
    }

    @Override
    public Optional<StoredOtp> findForUpdate(OtpPurpose purpose, String phone) {
        return jdbc.query("SELECT " + COLUMNS + " FROM otp_challenges WHERE purpose = ? AND phone = ? FOR UPDATE",
                MAPPER, purpose.name(), phone).stream().findFirst();
    }

    @Override
    public void updateAttempts(long id, int attempts) {
        jdbc.update("UPDATE otp_challenges SET attempts = ? WHERE id = ?", attempts, id);
    }

    @Override
    public void delete(OtpPurpose purpose, String phone) {
        jdbc.update("DELETE FROM otp_challenges WHERE purpose = ? AND phone = ?", purpose.name(), phone);
    }
}
