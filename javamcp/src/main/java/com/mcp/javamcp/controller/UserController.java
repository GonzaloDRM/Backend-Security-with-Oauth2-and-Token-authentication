package com.mcp.javamcp.controller;

import com.mcp.javamcp.dto.ErrorResponseDTO;
import com.mcp.javamcp.dto.RegisterRequestDTO;
import com.mcp.javamcp.dto.UserProfileDTO;
import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import com.mcp.javamcp.repository.OAuth2UserRepository;
import com.mcp.javamcp.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2UserRepository oauth2UserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationService verificationService;

    /**
     * Registro de nuevo usuario (público)
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            System.out.println("🔐 Intento de registro para: " + request.username());
            System.out.println("📧 Email: " + request.email());

            // Verificar si el usuario ya existe
            if (userRepository.findByUsername(request.username()).isPresent()) {
                System.out.println("❌ Usuario ya existe: " + request.username());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "success", false,
                                "error", "Usuario ya existe",
                                "message", "El nombre de usuario '" + request.username() + "' ya está en uso"));
            }

            // ✅ NUEVO: Verificar si el email ya existe
            if (request.email() != null && !request.email().isBlank()) {
                boolean emailExists = userRepository.findAll().stream()
                        .anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(request.email()));

                if (emailExists) {
                    System.out.println("❌ Email ya existe: " + request.email());
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(
                                    "success", false,
                                    "error", "Email ya registrado",
                                    "message", "El email '" + request.email() + "' ya está en uso"));
                }
            }

            // Crear nuevo usuario
            User newUser = new User();
            newUser.setUsername(request.username());
            newUser.setEmail(request.email());
            newUser.setPassword(passwordEncoder.encode(request.password()));
            newUser.setRoles("USER");
            newUser.setEmailVerified(false);

            userRepository.save(newUser);

            System.out.println("✅ Usuario registrado exitosamente: " + request.username());

            // Si tiene email, enviar código de verificación
            boolean emailSent = false;
            if (request.email() != null && !request.email().isBlank()) {
                try {
                    verificationService.createAndSendVerificationCode(newUser);
                    System.out.println("📧 Código de verificación enviado a: " + request.email());
                    emailSent = true;
                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando email de verificación: " + e.getMessage());
                    e.printStackTrace();
                    // No fallar el registro si falla el email
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Usuario registrado exitosamente",
                    "username", newUser.getUsername(),
                    "emailSent", emailSent,
                    "requiresVerification", request.email() != null && !request.email().isBlank()));

        } catch (Exception e) {
            System.err.println("❌ Error en registro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error en registro",
                            "message", e.getMessage()));
        }
    }

    /**
     * Verificar código de email
     * POST /api/users/verify
     * Body: { "username": "user", "code": "123456" }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            System.out.println("📥 Request recibido en /api/users/verify");
            System.out.println("📦 Body: " + request);

            String username = request.get("username");
            String code = request.get("code");

            System.out.println("🔍 Verificando código para usuario: " + username);
            System.out.println("🔢 Código recibido: " + code);

            if (username == null || username.isBlank()) {
                System.out.println("❌ Usuario faltante o vacío");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "verified", false,
                        "error", "Usuario es requerido"));
            }

            if (code == null || code.isBlank()) {
                System.out.println("❌ Código faltante o vacío");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "verified", false,
                        "error", "Código es requerido"));
            }

            System.out.println("🔄 Llamando a verificationService.verifyCode...");
            boolean verified = verificationService.verifyCode(username, code);
            System.out.println("📤 Resultado de verificación: " + verified);

            if (verified) {
                System.out.println("✅ Código verificado correctamente para: " + username);
                Map<String, Object> response = Map.of(
                        "success", true,
                        "verified", true,
                        "message", "Email verificado correctamente");
                System.out.println("📤 Enviando response: " + response);
                return ResponseEntity.ok(response);
            } else {
                System.out.println("❌ Código inválido o expirado para: " + username);
                Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "verified", false,
                        "error", "Código inválido o expirado");
                System.out.println("📤 Enviando error response: " + errorResponse);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

        } catch (Exception e) {
            System.err.println("❌ Excepción capturada en verifyEmail:");
            System.err.println("   Tipo: " + e.getClass().getName());
            System.err.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "verified", false,
                    "error", "Error al verificar código",
                    "message", e.getMessage() != null ? e.getMessage() : "Error desconocido");

            System.out.println("📤 Enviando exception response: " + errorResponse);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Reenviar código de verificación
     * POST /api/users/resend-code
     * Body: { "username": "user" }
     */
    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");

            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Usuario es requerido",
                        "message", "Debe proporcionar un nombre de usuario"));
            }

            System.out.println("📧 Solicitando reenvío de código para: " + username);
            verificationService.resendVerificationCode(username);
            System.out.println("✅ Código reenviado exitosamente");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Código de verificación reenviado exitosamente"));

        } catch (RuntimeException e) {
            System.err.println("❌ Error en resend-code: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido al reenviar código";

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", "Error al reenviar código",
                    "message", errorMsg));
        } catch (Exception e) {
            System.err.println("❌ Error inesperado en resend-code: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno del servidor",
                    "message", "Por favor, intente nuevamente más tarde"));
        }
    }

    /**
     * Obtener perfil del usuario actual
     * GET /api/users/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String username = authentication.getName();

        // Buscar en usuarios tradicionales
        var user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "username", user.get().getUsername(),
                    "email", user.get().getEmail() != null ? user.get().getEmail() : "",
                    "emailVerified", user.get().getEmailVerified(),
                    "roles", user.get().getRoles(),
                    "type", "local"));
        }

        // Buscar en usuarios OAuth2
        var oauth2User = oauth2UserRepository.findByEmail(username);
        if (oauth2User.isPresent()) {
            return ResponseEntity.ok(UserProfileDTO.from(oauth2User.get()));
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Actualizar perfil del usuario
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> updates,
            Authentication authentication) {

        String username = authentication.getName();

        // Buscar usuario OAuth2
        return oauth2UserRepository.findByEmail(username)
                .map(user -> {
                    // Actualizar campos permitidos
                    if (updates.containsKey("name")) {
                        user.setName(updates.get("name"));
                    }
                    oauth2UserRepository.save(user);

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Perfil actualizado"));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cambiar contraseña (solo para usuarios locales)
     * PUT /api/users/change-password
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> passwords,
            Authentication authentication) {

        String username = authentication.getName();
        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of("Datos inválidos",
                            "Se requieren currentPassword y newPassword"));
        }

        var user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponseDTO.of("Error",
                            "Solo usuarios locales pueden cambiar contraseña"));
        }

        // Verificar contraseña actual
        var foundUser = user.get();
        if (!passwordEncoder.matches(currentPassword, foundUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDTO.of("Error", "Contraseña actual incorrecta"));
        }

        // Actualizar contraseña
        foundUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(foundUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Contraseña actualizada exitosamente"));
    }

    /**
     * Eliminar cuenta del usuario
     * DELETE /api/users/profile
     */
    @DeleteMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        String username = authentication.getName();

        // Intentar borrar de usuarios locales
        var user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            var userToDelete = user.get();
            userRepository.delete(userToDelete);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cuenta eliminada"));
        }

        // Intentar borrar de usuarios OAuth2
        var oauth2User = oauth2UserRepository.findByEmail(username);
        if (oauth2User.isPresent()) {
            var oauth2UserToDelete = oauth2User.get();
            oauth2UserRepository.delete(oauth2UserToDelete);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cuenta eliminada"));
        }

        return ResponseEntity.notFound().build();
    }
}