package com.phoenix.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotBlank @Size(max = 60) String category,
        @DecimalMin("0.0") BigDecimal price,
        @Size(max = 500) String imageUrl,
        Boolean inStock,
        Map<String, String> specs,
        @NotBlank String dealerId) {}