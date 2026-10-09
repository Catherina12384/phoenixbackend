package com.phoenix.dto;

import com.phoenix.entity.AuditLog;

import java.time.Instant;
import java.util.Map;

public record AuditLogDto(Long id, Instant occurredAt, String requestId, String actorEmail, String actorRole,
                          String action, String targetType, String targetId, boolean success, String ip,
                          Map<String, Object> details) {
    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getOccurredAt(), a.getRequestId(), a.getActorEmail(), a.getActorRole(),
                a.getAction().name(), a.getTargetType(), a.getTargetId(), a.isSuccess(), a.getIp(), a.getDetails());
    }
}
