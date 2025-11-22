package com.mcp.javamcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.javamcp.dto.LoginRequestDTO;
import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class CustomAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Limpiar y crear usuario de prueba
        userRepository.deleteAll();

        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("testpass"));
        testUser.setRoles("USER");
        testUser.setEmailVerified(true);
        userRepository.save(testUser);
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("testuser", "testpass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void testLogin_InvalidPassword() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("nonexistent", "password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogin_EmptyCredentials() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_TokenFormat() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO("testuser", "testpass");

        // Act & Assert
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verificar que el token es un JWT válido (tiene 3 partes separadas por puntos)
        var jsonResponse = objectMapper.readTree(response);
        String token = jsonResponse.get("token").asText();

        assert token.split("\\.").length == 3 : "JWT debe tener 3 partes";
        assert token.startsWith("eyJ") : "JWT debe comenzar con 'eyJ'";
    }

    @Test
    void testLogin_UnverifiedEmail_ShouldFail() throws Exception {
        // Arrange
        User unverifiedUser = new User();
        unverifiedUser.setUsername("unverified");
        unverifiedUser.setPassword(passwordEncoder.encode("password"));
        unverifiedUser.setRoles("USER");
        unverifiedUser.setEmailVerified(false);
        userRepository.save(unverifiedUser);

        LoginRequestDTO request = new LoginRequestDTO("unverified", "password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cuenta no verificada"));
    }
}