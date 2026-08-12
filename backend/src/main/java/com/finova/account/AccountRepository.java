package com.finova.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPublicId(UUID publicId);

    List<Account> findByOwnerId(Long ownerId);

    Optional<Account> findByPublicIdAndOwnerId(UUID publicId, Long ownerId);

    boolean existsByAccountNumber(String accountNumber);
}
