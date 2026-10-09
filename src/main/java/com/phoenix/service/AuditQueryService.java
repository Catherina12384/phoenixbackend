package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.audit.AuditSpecificationFactory;
import com.phoenix.dto.AuditLogDto;
import com.phoenix.dto.PageDto;
import com.phoenix.entity.AuditLog;
import com.phoenix.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditQueryService {
    private final AuditLogRepository repository;
    private final AuditSpecificationFactory specs;
    private final AuditLogger audit;

    public AuditQueryService(AuditLogRepository repository, AuditSpecificationFactory specs, AuditLogger audit) {
        this.repository = repository;
        this.specs = specs;
        this.audit = audit;
    }

    public PageDto<AuditLogDto> search(String actor, AuditAction action, Boolean success,
                                       Instant from, Instant to, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        Page<AuditLog> result = repository.findAll(specs.create(actor, action, success, from, to), pageable);

        Map<String, Object> filters = new LinkedHashMap<>();
        if (actor != null) filters.put("actor", actor);
        if (action != null) filters.put("action", action.name());
        if (success != null) filters.put("success", success);
        audit.log(AuditAction.AUDIT_LOG_VIEWED, "audit_log", null, true, filters);

        return new PageDto<>(result.getContent().stream().map(AuditLogDto::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
