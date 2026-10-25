package com.phoenix.dto;

/** What the browser receives after login, signup or a password change. The token travels in an httpOnly cookie, never here. */
public record SessionResponse(UserDto user) {}
