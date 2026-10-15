package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUserEmail(String email);
    Optional<UserCredential> findByUserUsername(String username);
}