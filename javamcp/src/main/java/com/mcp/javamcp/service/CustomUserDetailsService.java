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
                    System.out.println("Email verificado: " + u.getEmailVerified());
                    System.out.println("Contraseña en BD: " + u.getPassword());
                    System.out.println("Roles: " + u.getRoles());

                    // ✅ NUEVO: Si el usuario tiene email y NO está verificado, bloquearlo
                    boolean hasEmail = u.getEmail() != null && !u.getEmail().isBlank();
                    boolean isVerified = u.getEmailVerified() != null && u.getEmailVerified();

                    if (hasEmail && !isVerified) {
                        System.out.println("⚠️ Usuario NO verificado - bloqueando login");
                        // Crear usuario deshabilitado
                        return User.builder()
                                .username(u.getUsername())
                                .password(u.getPassword())
                                .roles(u.getRoles().split(","))
                                .disabled(true) // ❌ Usuario deshabilitado
                                .accountLocked(false)
                                .build();
                    }

                    // Usuario verificado o sin email (normal)
                    return User.builder()
                            .username(u.getUsername())
                            .password(u.getPassword())
                            .roles(u.getRoles().split(","))
                            .disabled(false) // ✅ Usuario habilitado
                            .accountLocked(false)
                            .build();
                })
                .orElseThrow(() -> {
                    System.out.println("Usuario NO encontrado: " + username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
}