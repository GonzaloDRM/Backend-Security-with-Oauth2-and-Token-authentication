package com.mcp.javamcp.dto;

import java.util.List;

public record LoginResponseDTO(
        boolean success,
        String token,
        String username,
        List<String> roles,
        int expiresIn
) {
    // Constructor estático para crear respuesta exitosa
    public static LoginResponseDTO success(String token, String username, List<String> roles, int expiresIn) {
        return new LoginResponseDTO(true, token, username, roles, expiresIn);
    }

    // Constructor estático para crear respuesta de error
    public static LoginResponseDTO error() {
        return new LoginResponseDTO(false, null, null, null, 0);
    }
}
