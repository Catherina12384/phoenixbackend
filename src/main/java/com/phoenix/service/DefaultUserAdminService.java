package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.common.PhoneNumbers;
import com.phoenix.dto.CreateStaffRequest;
import com.phoenix.dto.PageDto;
import com.phoenix.dto.UserDto;
import com.phoenix.dto.UserUpdateRequest;
import com.phoenix.entity.User;
import com.phoenix.repository.UserRepository;
import com.phoenix.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admin-only user management. Roles can never be changed; users are soft-deleted (active=false). */
@Service
public class DefaultUserAdminService implements UserAdminService {
    private static final Logger log = LoggerFactory.getLogger(DefaultUserAdminService.class);

    private final UserRepository users;
    private final UserSpecificationFactory specs;
    private final PasswordEncoder encoder;
    private final AuditLogger audit;

    public DefaultUserAdminService(UserRepository users, UserSpecificationFactory specs,
                                   PasswordEncoder encoder, AuditLogger audit) {
        this.users = users;
        this.specs = specs;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    public PageDto<UserDto> search(String q, User.Role role, Boolean active, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<User> result = users.findAll(specs.create(q, role, active), pageable);
        audit.log(AuditAction.USERS_LISTED, "user", null, true, Map.of("returned", result.getNumberOfElements()));
        return new PageDto<>(result.getContent().stream().map(UserDto::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public UserDto get(Long id) {
        User u = find(id);
        audit.log(AuditAction.USER_VIEWED, "user", String.valueOf(id), true, null);
        return UserDto.from(u);
    }

    @Override
    @Transactional
    public UserDto createStaff(CreateStaffRequest req) {
        String email = req.email().trim().toLowerCase();
        String phone = PhoneNumbers.normalize(req.phone());
        if (users.existsByEmail(email)) throw conflict("An account with this email already exists");
        if (users.existsByPhone(phone)) throw conflict("An account with this phone number already exists");

        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(req.temporaryPassword()));
        u.setRole(User.Role.STAFF);
        u.setMustChangePassword(true);
        User saved = users.saveAndFlush(u);

        audit.log(AuditAction.USER_CREATED, "user", String.valueOf(saved.getId()), true, Map.of("role", "STAFF"));
        log.info("Staff account created: {}", email);
        return UserDto.from(saved);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserUpdateRequest req, AuthenticatedUser actor) {
        User u = find(id);
        Map<String, Object> changes = new LinkedHashMap<>();
        boolean revokeTokens = false;

        if (req.name() != null && !req.name().isBlank() && !req.name().trim().equals(u.getName())) {
            changes.put("name", change(u.getName(), req.name().trim()));
            u.setName(req.name().trim());
        }
        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equals(u.getEmail())) {
                if (users.existsByEmailAndIdNot(email, id)) throw conflict("An account with this email already exists");
                changes.put("email", change(u.getEmail(), email));
                u.setEmail(email);
                revokeTokens = true;     // the token subject is the email
            }
        }
        if (req.phone() != null && !req.phone().isBlank()) {
            String phone = PhoneNumbers.normalize(req.phone());
            if (!phone.equals(u.getPhone())) {
                if (users.existsByPhoneAndIdNot(phone, id)) throw conflict("An account with this phone number already exists");
                changes.put("phone", change(u.getPhone(), phone));
                u.setPhone(phone);
            }
        }
        if (req.active() != null && req.active() != u.isActive()) {
            if (!req.active()) assertCanDeactivate(u, actor);
            changes.put("active", change(u.isActive(), req.active()));
            u.setActive(req.active());
            revokeTokens = true;
        }
        if (req.temporaryPassword() != null) {
            if (u.getId().equals(actor.id())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use change-password to change your own password");
            }
            u.setPasswordHash(encoder.encode(req.temporaryPassword()));
            u.setMustChangePassword(true);
            changes.put("password", "reset to a temporary password");
            revokeTokens = true;
        }

        if (changes.isEmpty()) return UserDto.from(u);
        if (revokeTokens) u.setTokenVersion(u.getTokenVersion() + 1);
        users.saveAndFlush(u);

        audit.log(AuditAction.USER_UPDATED, "user", String.valueOf(id), true, Map.of("changes", changes));
        log.info("User {} updated by {}: fields {}", id, actor.email(), changes.keySet());
        return UserDto.from(u);
    }

    @Override
    @Transactional
    public void deactivate(Long id, AuthenticatedUser actor) {
        User u = find(id);
        if (!u.isActive()) return;
        assertCanDeactivate(u, actor);
        u.setActive(false);
        u.setTokenVersion(u.getTokenVersion() + 1);
        users.saveAndFlush(u);

        audit.log(AuditAction.USER_DEACTIVATED, "user", String.valueOf(id), true, Map.of("role", u.getRole().name()));
        log.info("User {} deactivated by {}", id, actor.email());
    }

    private void assertCanDeactivate(User target, AuthenticatedUser actor) {
        if (target.getId().equals(actor.id())) {
            throw conflict("You cannot deactivate your own account");
        }
        if (target.getRole() == User.Role.ADMIN && target.isActive()
                && users.countByRoleAndActiveTrue(User.Role.ADMIN) <= 1) {
            throw conflict("The last active admin cannot be deactivated");
        }
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private static Map<String, Object> change(Object from, Object to) {
        Map<String, Object> m = new HashMap<>();
        m.put("from", from);
        m.put("to", to);
        return m;
    }
}
