package com.phoenix.controller;

import com.phoenix.dto.CreateStaffRequest;
import com.phoenix.dto.PageDto;
import com.phoenix.dto.UserDto;
import com.phoenix.dto.UserUpdateRequest;
import com.phoenix.entity.User;
import com.phoenix.security.AuthenticatedUser;
import com.phoenix.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** ADMIN only (enforced in SecurityConfig). Only STAFF accounts can be created here. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserAdminService service;

    public AdminUserController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<UserDto> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) User.Role role,
                                 @RequestParam(required = false) Boolean active,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(q, role, active, page, size);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return service.createStaff(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                          @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.update(id, request, actor);
    }

    /** Soft delete: the account is disabled, its history stays. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser actor) {
        service.deactivate(id, actor);
    }
}
