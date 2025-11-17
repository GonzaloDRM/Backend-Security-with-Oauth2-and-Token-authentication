package com.mcp.javamcp.utils;

import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificar si ya existen usuarios
            if (userRepository.count() == 0) {
                System.out.println("=== CREANDO USUARIOS DE PRUEBA ===");

                // Usuario 1
                User user1 = new User();
                user1.setUsername("gonza");
                user1.setPassword(passwordEncoder.encode("123"));
                user1.setRoles("USER");
                userRepository.save(user1);
                System.out.println("✅ Usuario creado: gonza/1234");

                // Usuario 2 (opcional)
                User user2 = new User();
                user2.setUsername("admin");
                user2.setPassword(passwordEncoder.encode("admin"));
                user2.setRoles("ADMIN,USER");
                userRepository.save(user2);
                System.out.println("✅ Usuario creado: admin/admin123");

                System.out.println("=== USUARIOS CREADOS CORRECTAMENTE ===");
            } else {
                System.out.println("=== USUARIOS YA EXISTEN EN LA BD ===");
            }
        };
    }
}
