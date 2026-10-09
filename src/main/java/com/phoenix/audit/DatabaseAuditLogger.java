package com.phoenix.audit;

import com.phoenix.common.ClientContext;
import com.phoenix.entity.AuditLog;
import com.phoenix.repository.AuditLogRepository;
import com.phoenix.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * Persists each event in its own transaction (so it survives a rollback of the business
 * transaction) and mirrors it to the application log. Never include passwords, OTPs or tokens in details.
 */
@Component
public class DatabaseAuditLogger implements AuditLogger {
    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final AuditLogRepository repository;
    private final ClientContext client;
    private final TransactionTemplate tx;

    public DatabaseAuditLogger(AuditLogRepository repository, ClientContext client, PlatformTransactionManager tm) {
        this.repository = repository;
        this.client = client;
        this.tx = new TransactionTemplate(tm);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void log(AuditAction action, String targetType, String targetId, boolean success, Map<String, Object> details) {
        String email = null, role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            email = u.email();
            role = u.role().name();
        }
        logAs(email, role, action, targetType, targetId, success, details);
    }

    @Override
    public void logAs(String actorEmail, String actorRole, AuditAction action, String targetType, String targetId,
                      boolean success, Map<String, Object> details) {
        log.info("action={} success={} actor={} role={} target={}:{}", action, success,
                actorEmail, actorRole, targetType, targetId);
        try {
            AuditLog row = new AuditLog();
            row.setRequestId(client.requestId());
            row.setIp(client.ip());
            row.setActorEmail(actorEmail);
            row.setActorRole(actorRole);
            row.setAction(action);
            row.setTargetType(targetType);
            row.setTargetId(targetId);
            row.setSuccess(success);
            row.setDetails(details);
            tx.executeWithoutResult(s -> repository.save(row));
        } catch (RuntimeException e) {
            log.error("FAILED TO WRITE AUDIT ROW action={} actor={}: {}", action, actorEmail, e.getClass().getSimpleName());
        }
    }
}
