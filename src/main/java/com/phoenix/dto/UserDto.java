package com.phoenix.dto;

import com.phoenix.entity.User;

public record UserDto(Long id, String name, String email, String phone, String role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole().name());
    }
}