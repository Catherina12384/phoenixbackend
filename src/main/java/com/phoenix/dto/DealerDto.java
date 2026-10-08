package com.phoenix.dto;

// Shape matches the front end: { id, name, logo? }
public record DealerDto(String id, String name, String logo) {}