package com.mcp.javamcp.repository;

import com.mcp.javamcp.model.OAuth2User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuth2UserRepository extends JpaRepository<OAuth2User, Long> {
    Optional<OAuth2User> findByEmail(String email);
    Optional<OAuth2User> findByProviderAndProviderId(String provider, String providerId);
}