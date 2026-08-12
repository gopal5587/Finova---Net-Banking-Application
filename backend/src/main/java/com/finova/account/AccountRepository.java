package com.finova.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPublicId(UUID publicId);

    /**
     * Loads an account with a {@code SELECT ... FOR UPDATE} row lock. Used inside transfers so that
     * concurrent operations on the same account serialise at the database rather than racing on the
     * balance. Callers must acquire locks in a consistent order (ascending id) to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    /**
     * Resolves the internal id for a public UUID without loading the managed entity. Used before
     * acquiring a pessimistic lock so we never pre-load a (soon to be stale) versioned entity into
     * the persistence context, which would otherwise trigger a spurious optimistic-lock failure.
     */
    @Query("select a.id from Account a where a.publicId = :publicId")
    Optional<Long> findIdByPublicId(@Param("publicId") UUID publicId);

    List<Account> findByOwnerId(Long ownerId);

    Optional<Account> findByPublicIdAndOwnerId(UUID publicId, Long ownerId);

    boolean existsByAccountNumber(String accountNumber);
}
