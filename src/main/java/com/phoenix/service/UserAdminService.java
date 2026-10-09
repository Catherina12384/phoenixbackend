package com.phoenix.service;

import com.phoenix.dto.CreateStaffRequest;
import com.phoenix.dto.PageDto;
import com.phoenix.dto.UserDto;
import com.phoenix.dto.UserUpdateRequest;
import com.phoenix.entity.User;
import com.phoenix.security.AuthenticatedUser;

public interface UserAdminService {
    PageDto<UserDto> search(String q, User.Role role, Boolean active, int page, int size);

    UserDto get(Long id);

    UserDto createStaff(CreateStaffRequest request);

    UserDto update(Long id, UserUpdateRequest request, AuthenticatedUser actor);

    void deactivate(Long id, AuthenticatedUser actor);
}
