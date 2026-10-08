package com.phoenix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DealerUpdateRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String logo,
        Integer sortOrder) {}