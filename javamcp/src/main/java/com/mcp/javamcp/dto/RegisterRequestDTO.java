package com.mcp.javamcp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para registro de nuevos usuarios
 */
public record RegisterRequestDTO(
        @NotBlank(message = "Username es requerido")
        @Size(min = 3, max = 50, message = "Username debe tener entre 3 y 50 caracteres")
        String username,

        @Email(message = "Email debe ser válido")
        String email,

        @NotBlank(message = "Password es requerido")
        @Size(min = 3, message = "Password debe tener al menos 3 caracteres")
        String password,

        String confirmPassword
) {
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}