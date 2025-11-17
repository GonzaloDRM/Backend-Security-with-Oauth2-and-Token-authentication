package com.mcp.javamcp.controller;

import com.mcp.javamcp.dto.ErrorResponseDTO;
import com.mcp.javamcp.dto.RegisterRequestDTO;
import com.mcp.javamcp.dto.UserProfileDTO;
import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import com.mcp.javamcp.repository.OAuth2UserRepository;
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

    /**
     * Registro de nuevo usuario (público)
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            System.out.println("📝 Intento de registro para: " + request.username());

            // Verificar si el usuario ya existe
            if (userRepository.findByUsername(request.username()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ErrorResponseDTO.of("Usuario ya existe",
                                "El nombre de usuario '" + request.username() + "' ya está en uso"));
            }

            // Crear nuevo usuario
            User newUser = new User();
            newUser.setUsername(request.username());
            newUser.setPassword(passwordEncoder.encode(request.password()));
            newUser.setRoles("USER"); // Por defecto USER

            userRepository.save(newUser);

            System.out.println("✅ Usuario registrado exitosamente: " + request.username());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Usuario registrado exitosamente",
                    "username", newUser.getUsername()
            ));

        } catch (Exception e) {
            System.err.println("❌ Error en registro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponseDTO.of("Error en registro", e.getMessage()));
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
                    "roles", user.get().getRoles(),
                    "type", "local"
            ));
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
        var oauth2User = oauth2UserRepository.findByEmail(username);
        if (oauth2User.isPresent()) {
            var user = oauth2User.get();

            // Actualizar campos permitidos
            if (updates.containsKey("name")) {
                user.setName(updates.get("name"));
            }

            oauth2UserRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Perfil actualizado"
            ));
        }

        return ResponseEntity.notFound().build();
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
        if (!passwordEncoder.matches(currentPassword, user.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDTO.of("Error", "Contraseña actual incorrecta"));
        }

        // Actualizar contraseña
        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Contraseña actualizada exitosamente"
        ));
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
            userRepository.delete(user.get());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cuenta eliminada"
            ));
        }

        // Intentar borrar de usuarios OAuth2
        var oauth2User = oauth2UserRepository.findByEmail(username);
        if (oauth2User.isPresent()) {
            oauth2UserRepository.delete(oauth2User.get());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cuenta eliminada"
            ));
        }

        return ResponseEntity.notFound().build();
    }
}
