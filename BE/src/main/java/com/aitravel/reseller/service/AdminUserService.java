package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.UpdateUserRoleRequest;
import com.aitravel.reseller.dto.request.UpdateUserStatusRequest;
import com.aitravel.reseller.dto.request.UserUpdateRequest;
import com.aitravel.reseller.dto.respone.PageResponse;
import com.aitravel.reseller.dto.respone.UserResponse;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.RoleRepository;
import com.aitravel.reseller.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PageResponse<UserResponse> getUsers(String keyword, String role, String status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), searchKeyword),
                        cb.like(cb.lower(root.get("fullName")), searchKeyword),
                        cb.like(cb.lower(root.get("phone")), searchKeyword)
                ));
            }

            if (StringUtils.hasText(role)) {
                Join<User, Role> roles = root.join("roles");
                predicates.add(cb.equal(roles.get("name"), role.toUpperCase()));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> usersPage = userRepository.findAll(spec, pageable);
        return PageResponse.of(usersPage.map(this::mapToUserResponse));
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public void updateUserStatus(UUID id, UpdateUserStatusRequest request, UUID currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getId().equals(currentUserId) && !request.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new IllegalStateException("Admin cannot deactivate their own account");
        }

        user.setStatus(request.getStatus().toUpperCase());
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(UUID id, UpdateUserRoleRequest request, UUID currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getId().equals(currentUserId)) {
            boolean hasAdminInRequest = request.getRoles().stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));
            if (!hasAdminInRequest) {
                throw new IllegalStateException("Admin cannot remove their own ADMIN role");
            }
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(roleNames)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
