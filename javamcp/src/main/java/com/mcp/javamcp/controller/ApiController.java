package com.mcp.javamcp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") // Doble protección
public class ApiController {

    // Endpoint público (sin autenticación)
    @GetMapping("/public/data")
    public ResponseEntity<?> getPublicData() {
        return ResponseEntity.ok(Map.of(
                "message", "Este es data pública",
                "timestamp", new Date(),
                "status", "success"
        ));
    }

    // Endpoint protegido (requiere JWT)
    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(auth -> auth.getAuthority()).toList(),
                "authenticated", true,
                "message", "¡Hola desde el API protegido!",
                "timestamp", new Date()
        ));
    }

    // Endpoint para verificar token
    @GetMapping("/auth/verify")
    public ResponseEntity<?> verifyToken(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "valid", authentication != null,
                "user", authentication != null ? authentication.getName() : "anonymous",
                "timestamp", new Date()
        ));
    }
}