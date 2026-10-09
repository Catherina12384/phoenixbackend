package com.phoenix.audit;

import com.phoenix.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuditSpecificationFactory {
    public Specification<AuditLog> create(String actor, AuditAction action, Boolean success, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                p.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actor.trim().toLowerCase() + "%"));
            }
            if (action != null) p.add(cb.equal(root.get("action"), action));
            if (success != null) p.add(cb.equal(root.get("success"), success));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) p.add(cb.lessThan(root.get("occurredAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
