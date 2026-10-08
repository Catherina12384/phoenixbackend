package com.phoenix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DealerRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "lowercase slug, e.g. cp-plus") String id,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String logo,
        Integer sortOrder) {}