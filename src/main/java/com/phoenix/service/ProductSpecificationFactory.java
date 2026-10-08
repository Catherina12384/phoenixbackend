package com.phoenix.service;

import com.phoenix.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductSpecificationFactory {
    public Specification<Product> create(List<String> dealerIds, String category, String queryText) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dealerIds != null && !dealerIds.isEmpty()) {
                predicates.add(root.get("dealer").get("id").in(dealerIds));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        category.trim().toLowerCase()));
            }

            if (queryText != null && !queryText.isBlank()) {
                String like = "%" + queryText.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), like)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
