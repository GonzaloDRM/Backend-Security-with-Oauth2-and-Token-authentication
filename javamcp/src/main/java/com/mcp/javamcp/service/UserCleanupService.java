package com.mcp.javamcp.service;

import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para limpiar usuarios no verificados después de X horas
 */
@Service
public class UserCleanupService {

    @Autowired
    private UserRepository userRepository;

    @Value("${user.cleanup.unverified-hours:24}")
    private int unverifiedHours;

    /**
     * Tarea programada que se ejecuta cada hora
     * Elimina usuarios no verificados cuyo registro sea mayor a X horas
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    @Transactional
    public void cleanupUnverifiedUsers() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(unverifiedHours);

            System.out.println("🧹 Iniciando limpieza de usuarios no verificados...");
            System.out.println("   Eliminando usuarios registrados antes de: " + cutoffTime);

            List<User> unverifiedUsers = userRepository.findAll().stream()
                    .filter(user -> !user.getEmailVerified())
                    .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isBefore(cutoffTime))
                    .toList();

            if (unverifiedUsers.isEmpty()) {
                System.out.println("✅ No hay usuarios no verificados para eliminar");
                return;
            }

            System.out.println("🗑️  Eliminando " + unverifiedUsers.size() + " usuarios no verificados:");

            for (User user : unverifiedUsers) {
                System.out.println("   - " + user.getUsername() + " (creado: " + user.getCreatedAt() + ")");
                userRepository.delete(user);
            }

            System.out.println("✅ Limpieza completada: " + unverifiedUsers.size() + " usuarios eliminados");

        } catch (Exception e) {
            System.err.println("❌ Error en limpieza de usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método manual para forzar limpieza (útil para testing)
     */
    public int forceCleanup() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(unverifiedHours);

        List<User> unverifiedUsers = userRepository.findAll().stream()
                .filter(user -> !user.getEmailVerified())
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isBefore(cutoffTime))
                .toList();

        unverifiedUsers.forEach(userRepository::delete);

        return unverifiedUsers.size();
    }
}