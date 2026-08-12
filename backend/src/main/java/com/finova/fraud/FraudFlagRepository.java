package com.finova.fraud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {

    Page<FraudFlag> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FraudFlag> findByResolvedOrderByCreatedAtDesc(boolean resolved, Pageable pageable);

    java.util.Optional<FraudFlag> findByPublicId(java.util.UUID publicId);
}
