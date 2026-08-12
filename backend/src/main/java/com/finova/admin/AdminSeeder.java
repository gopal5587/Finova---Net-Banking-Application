package com.finova.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.finova.user.Role;
import com.finova.user.User;
import com.finova.user.UserRepository;

/**
 * Ensures an administrator account exists so the system is usable out of the box.
 *
 * <p>Runs once at startup: if no admin with the configured username exists, one is created with a
 * BCrypt-hashed password taken from {@code finova.admin.password} (env {@code ADMIN_PASSWORD}). The
 * operation is idempotent, so repeated restarts never create duplicates or reset the password.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${finova.admin.username:admin}") String adminUsername,
                       @Value("${finova.admin.email:admin@finova.local}") String adminEmail,
                       @Value("${finova.admin.password:${ADMIN_PASSWORD:Admin@12345}}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase(adminUsername)) {
            log.debug("Admin user '{}' already present; skipping seed", adminUsername);
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setFullName("Finova Administrator");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.warn("Seeded default admin user '{}'. Change its password immediately in a real deployment.",
                adminUsername);
    }
}
