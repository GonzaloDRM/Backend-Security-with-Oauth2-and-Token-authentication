package com.mcp.javamcp.security;

import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para verificar la seguridad
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private String validToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Crear usuario normal
        User user = new User();
        user.setUsername("normaluser");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRoles("USER");
        user.setEmailVerified(true);
        userRepository.save(user);

        // Crear usuario admin
        User admin = new User();
        admin.setUsername("adminuser");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRoles("ADMIN,USER");
        admin.setEmailVerified(true);
        userRepository.save(admin);

        // Generar tokens
        validToken = generateToken("normaluser", List.of("ROLE_USER"));
        adminToken = generateToken("adminuser", List.of("ROLE_ADMIN", "ROLE_USER"));
    }

    private String generateToken(String username, List<String> roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .subject(username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("roles", roles)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Test
    void testPublicEndpoint_NoToken_Success() throws Exception {
        // Los endpoints públicos deben ser accesibles sin token
        mockMvc.perform(get("/api/public/data"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_NoToken_Unauthorized() throws Exception {
        // Los endpoints protegidos deben rechazar requests sin token
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoint_ValidToken_Success() throws Exception {
        // Con token válido debe permitir acceso
        mockMvc.perform(get("/api/user/info")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("normaluser"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void testProtectedEndpoint_InvalidToken_Unauthorized() throws Exception {
        // Token inválido debe ser rechazado
        mockMvc.perform(get("/api/user/info")
                .header("Authorization", "Bearer invalid_token_123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterEndpoint_NoToken_Success() throws Exception {
        // Registro debe ser público (sin token)
        String newUser = """
                {
                    "username": "newregistered",
                    "password": "password123",
                    "confirmPassword": "password123"
                }
                """;

        mockMvc.perform(post("/api/users/register")
                .contentType("application/json")
                .content(newUser))
                .andExpect(status().isCreated());
    }

    @Test
    void testLoginEndpoint_NoToken_Success() throws Exception {
        // Login debe ser público
        String credentials = """
                {
                    "username": "normaluser",
                    "password": "password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
