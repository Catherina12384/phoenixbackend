package com.phoenix.controller;

import com.phoenix.audit.AuditAction;
import com.phoenix.dto.AuditLogDto;
import com.phoenix.dto.PageDto;
import com.phoenix.service.AuditQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Read-only. from/to are ISO-8601 instants, e.g. 2026-10-01T00:00:00Z. */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
    private final AuditQueryService service;

    public AdminAuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<AuditLogDto> list(@RequestParam(required = false) String actor,
                                     @RequestParam(required = false) AuditAction action,
                                     @RequestParam(required = false) Boolean success,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return service.search(actor, action, success, from, to, page, size);
    }
}
