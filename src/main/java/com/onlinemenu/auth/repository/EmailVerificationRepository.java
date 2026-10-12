package com.onlinemenu.auth.repository;

import com.onlinemenu.auth.domain.EmailCodePurpose;
import com.onlinemenu.auth.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

  Optional<EmailVerification> findByUserIdAndPurpose(UUID userId, EmailCodePurpose purpose);

  Optional<EmailVerification> findByTokenHashAndPurpose(String tokenHash, EmailCodePurpose purpose);
}
