package com.aitravel.reseller.service.admin;

import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="app.bootstrap-admin.enabled", havingValue="true")
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    @Value("${app.bootstrap-admin.email}") private String email;
    @Value("${app.bootstrap-admin.password}") private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || password.length()<12 || password.length()>72)
            throw new IllegalArgumentException("Bootstrap requires a valid email and password of 12..72 characters");
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                throw new IllegalStateException("Bootstrap will not promote an existing non-admin account");
            return;
        }
        var role = roles.findByName("ROLE_ADMIN").orElseThrow();
        users.save(User.builder().email(email).fullName("Platform Administrator")
                .passwordHash(encoder.encode(password)).status("ACTIVE").emailVerifiedAt(Instant.now())
                .roles(Set.of(role)).build());
    }
}
