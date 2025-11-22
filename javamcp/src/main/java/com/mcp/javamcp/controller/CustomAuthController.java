package com.mcp.javamcp.controller;

import com.mcp.javamcp.dto.ErrorResponseDTO;
import com.mcp.javamcp.dto.LoginRequestDTO;
import com.mcp.javamcp.dto.LoginResponseDTO;
import com.mcp.javamcp.dto.UserProfileDTO;
import com.mcp.javamcp.repository.OAuth2UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomAuthController {

        @Autowired
        private com.mcp.javamcp.repository.UserRepository userRepository;

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private JwtEncoder jwtEncoder;

        @Autowired
        private OAuth2UserRepository oauth2UserRepository;

        /**
         * Endpoint para login desde frontend con JSON
         * POST /api/auth/login
         */
        @PostMapping("/login")
        public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
                try {
                        System.out.println("🔐 Intento de login para: " + request.username());

                        // Autenticar con Spring Security
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.username(),
                                                        request.password()));

                        System.out.println("Autenticación exitosa para: " + authentication.getName());

                        // Verificar si el email está verificado
                        var user = userRepository.findByUsername(request.username());
                        if (user.isPresent() && !user.get().getEmailVerified()) {
                                System.out.println("⚠️ Usuario no verificado: " + request.username());
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(ErrorResponseDTO.of("Cuenta no verificada",
                                                                "Debes verificar tu email antes de iniciar sesión"));
                        }

                        // Generar JWT
                        JwtClaimsSet claims = JwtClaimsSet.builder()
                                        .issuer("http://localhost:8080")
                                        .subject(authentication.getName())
                                        .issuedAt(Instant.now())
                                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                                        .claim("roles", authentication.getAuthorities().stream()
                                                        .map(auth -> auth.getAuthority())
                                                        .collect(Collectors.toList()))
                                        .build();

                        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

                        System.out.println("🔑 Token JWT generado");

                        // Crear respuesta usando el DTO
                        var response = LoginResponseDTO.success(
                                        token,
                                        authentication.getName(),
                                        authentication.getAuthorities().stream()
                                                        .map(auth -> auth.getAuthority())
                                                        .collect(Collectors.toList()),
                                        3600 // 1 hora en segundos
                        );

                        return ResponseEntity.ok(response);

                } catch (BadCredentialsException e) {
                        System.err.println("Credenciales inválidas para: " + request.username());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ErrorResponseDTO.of("Credenciales inválidas",
                                                        "Usuario o contraseña incorrectos"));

                } catch (AuthenticationException e) {
                        System.err.println("Error de autenticación: " + e.getMessage());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ErrorResponseDTO.of("Error de autenticación", e.getMessage()));
                }
        }

        /**
         * Endpoint para ver todos los usuarios OAuth2 registrados
         * GET /api/auth/users
         */
        @GetMapping("/users")
        public ResponseEntity<?> getOAuth2Users(Authentication authentication) {
                // Solo admin puede ver la lista de usuarios
                boolean isAdmin = authentication != null &&
                                authentication.getAuthorities().stream()
                                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (!isAdmin) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ErrorResponseDTO.of("Forbidden",
                                                        "Solo administradores pueden ver usuarios"));
                }

                var users = oauth2UserRepository.findAll().stream()
                                .map(UserProfileDTO::from)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(users);
        }
}