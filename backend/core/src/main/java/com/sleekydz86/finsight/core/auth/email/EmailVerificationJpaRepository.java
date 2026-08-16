package com.sleekydz86.finsight.core.auth.email;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationJpaEntity, Long> {

    Optional<EmailVerificationJpaEntity> findByChallengeId(String challengeId);

    Optional<EmailVerificationJpaEntity> findFirstByEmailAndPurposeAndStatusOrderByVerifiedAtDesc(
            String email,
            EmailVerificationPurpose purpose,
            EmailVerificationStatus status);

    List<EmailVerificationJpaEntity> findByEmailAndPurposeAndStatus(
            String email,
            EmailVerificationPurpose purpose,
            EmailVerificationStatus status);

    long countByEmailAndPurposeAndRequestedAtAfter(
            String email,
            EmailVerificationPurpose purpose,
            LocalDateTime after);
}
