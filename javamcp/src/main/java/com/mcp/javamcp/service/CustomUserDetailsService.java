package com.mcp.javamcp.service;

import com.mcp.javamcp.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== BUSCANDO USUARIO EN BD: " + username + " ===");

        return repo.findByUsername(username)
                .map(u -> {
                    System.out.println("Usuario encontrado: " + u.getUsername());
                    System.out.println("Contraseña en BD: " + u.getPassword());
                    System.out.println("Roles: " + u.getRoles());

                    return User.builder()
                            .username(u.getUsername())
                            .password(u.getPassword()) // Debe estar codificada en BCrypt
                            .roles(u.getRoles().split(","))
                            .build();
                })
                .orElseThrow(() -> {
                    System.out.println("Usuario NO encontrado: " + username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
}