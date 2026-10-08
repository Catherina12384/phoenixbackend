package com.phoenix.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductDto(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal price,
        String imageUrl,
        boolean inStock,
        Map<String, String> specs,
        String dealerId,
        String dealerName) {}