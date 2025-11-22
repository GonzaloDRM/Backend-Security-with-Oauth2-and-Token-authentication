package com.mcp.javamcp.controller;

import com.mcp.javamcp.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/password-reset")
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Solicitar código de recuperación
     * POST /api/password-reset/request
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Email es requerido"));
            }

            System.out.println("📧 Solicitud de recuperación para: " + email);

            passwordResetService.requestPasswordReset(email);

            // Por seguridad, siempre decimos que se envió el email
            // incluso si el email no existe
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Si el email existe, recibirás un código de recuperación"));

        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());

            // Errores específicos que sí queremos mostrar
            if (e.getMessage().contains("verificar tu email")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "error", e.getMessage()));
            }

            // Otros errores genéricos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error al procesar solicitud"));
        }
    }

    /**
     * Verificar código de recuperación
     * POST /api/password-reset/verify
     * Body: { "email": "user@example.com", "code": "123456" }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyResetCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            if (email == null || code == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "verified", false,
                        "error", "Email y código son requeridos"));
            }

            boolean verified = passwordResetService.verifyResetCode(email, code);

            if (verified) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "verified", true,
                        "message", "Código válido"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "verified", false,
                        "error", "Código inválido o expirado"));
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "verified", false,
                    "error", "Error al verificar código"));
        }
    }

    /**
     * Restablecer contraseña
     * POST /api/password-reset/reset
     * Body: { "email": "user@example.com", "code": "123456", "newPassword": "..." }
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            String newPassword = request.get("newPassword");

            if (email == null || code == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Email, código y nueva contraseña son requeridos"));
            }

            if (newPassword.length() < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "La contraseña debe tener al menos 3 caracteres"));
            }

            passwordResetService.resetPassword(email, code, newPassword);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Contraseña restablecida exitosamente"));

        } catch (RuntimeException e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error al restablecer contraseña"));
        }
    }
}