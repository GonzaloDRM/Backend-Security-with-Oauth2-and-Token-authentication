package com.mcp.javamcp.dto;

import java.time.LocalDateTime;

public record UserProfileDTO(
        Long id,
        String email,
        String name,
        String picture,
        String provider,
        LocalDateTime lastLogin,
        Integer loginCount,
        String roles
) {
    // Método para crear desde entidad OAuth2User
    public static UserProfileDTO from(com.mcp.javamcp.model.OAuth2User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPicture(),
                user.getProvider(),
                user.getLastLogin(),
                user.getLoginCount(),
                user.getRoles()
        );
    }
}