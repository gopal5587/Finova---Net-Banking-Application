package com.finova.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Case-insensitive lookup used during login. */
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByPublicId(UUID publicId);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
