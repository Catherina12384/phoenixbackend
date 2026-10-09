package com.phoenix.audit;

import java.util.Map;

/** Writes the audit trail. Implementations must never throw: auditing must not break a request. */
public interface AuditLogger {
    /** Actor is taken from the current security context (null actor = anonymous). */
    void log(AuditAction action, String targetType, String targetId, boolean success, Map<String, Object> details);

    /** For flows where the caller is not authenticated yet (login, signup, password reset). */
    void logAs(String actorEmail, String actorRole, AuditAction action, String targetType, String targetId,
               boolean success, Map<String, Object> details);
}
