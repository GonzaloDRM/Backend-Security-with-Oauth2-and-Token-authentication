package com.mcp.javamcp.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Username es requerido")
        String username,

        @NotBlank(message = "Password es requerido")
        String password) {}
