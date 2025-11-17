package com.mcp.javamcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.javamcp.dto.RegisterRequestDTO;
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
@Transactional // Rollback después de cada test
class UserControllerTest {

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
        // Limpiar usuarios antes de cada test
        userRepository.deleteAll();
    }

    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(
                "newuser",
                "password123",
                "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"))
                .andExpect(jsonPath("$.username").value("newuser"));

        // Verificar que el usuario fue creado en la BD
        var user = userRepository.findByUsername("newuser");
        assert user.isPresent();
        assert user.get().getRoles().equals("USER");
    }

    @Test
    void testRegister_UserAlreadyExists() throws Exception {
        // Arrange - Crear usuario primero
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setRoles("USER");
        userRepository.save(existingUser);

        RegisterRequestDTO request = new RegisterRequestDTO(
                "existing",
                "newpassword",
                "newpassword"
        );

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Usuario ya existe"));
    }

    @Test
    void testRegister_InvalidUsername_TooShort() throws Exception {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(
                "ab",  // Muy corto
                "password123",
                "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_EmptyUsername() throws Exception {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(
                "",
                "password123",
                "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
