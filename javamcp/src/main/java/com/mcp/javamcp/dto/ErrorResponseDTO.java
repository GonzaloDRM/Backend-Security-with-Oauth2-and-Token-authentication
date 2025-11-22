package com.mcp.javamcp.dto;

public record ErrorResponseDTO(
        boolean success,
        String error,
        String message
) {
    public static ErrorResponseDTO of(String error, String message) {
        return new ErrorResponseDTO(false, error, message);
    }
}