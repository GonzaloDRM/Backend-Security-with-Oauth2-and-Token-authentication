package com.mcp.javamcp.service;

import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class VerificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${verification.code.expiration-minutes:15}")
    private int codeExpirationMinutes;

    @Value("${verification.code.length:6}")
    private int codeLength;

    /**
     * Generar código de verificación aleatorio
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10)); // 0-9
        }

        return code.toString();
    }

    /**
     * Crear y enviar código de verificación
     */
    @Transactional
    public void createAndSendVerificationCode(User user) {
        try {
            // Generar código
            String code = generateVerificationCode();

            // Calcular expiración
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(codeExpirationMinutes);

            // Guardar en el usuario
            user.setVerificationCode(code);
            user.setVerificationCodeExpiresAt(expiresAt);
            user.setEmailVerified(false);
            userRepository.save(user);

            System.out.println("📧 Código generado para " + user.getEmail() + ": " + code);
            System.out.println("⏰ Expira en: " + expiresAt);

            // Enviar email
            try {
                emailService.sendVerificationCodeHtml(
                        user.getEmail(),
                        user.getUsername(),
                        code);
                System.out.println("✅ Email de verificación enviado");
            } catch (Exception e) {
                System.err.println("❌ Error enviando email: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al enviar email de verificación: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("❌ Error creando código de verificación: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear código de verificación", e);
        }
    }

    /**
     * Verificar código
     */
    @Transactional
    public boolean verifyCode(String username, String code) {
        try {
            System.out.println("🔍 Verificando código para usuario: " + username);
            System.out.println("🔢 Código recibido: " + code);

            var userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                System.out.println("❌ Usuario no encontrado: " + username);
                return false;
            }

            User user = userOpt.get();
            System.out.println("✅ Usuario encontrado: " + user.getUsername());
            System.out.println("📧 Email: " + user.getEmail());
            System.out.println("✔️ Email verificado actual: " + user.getEmailVerified());
            System.out.println("🔢 Código guardado en BD: " + user.getVerificationCode());
            System.out.println("⏰ Expira en: " + user.getVerificationCodeExpiresAt());

            // Verificar si ya está verificado
            if (user.getEmailVerified() != null && user.getEmailVerified()) {
                System.out.println("⚠️ Usuario ya verificado: " + username);
                return true; // Ya está verificado
            }

            // Verificar código
            if (user.getVerificationCode() == null || user.getVerificationCode().isBlank()) {
                System.out.println("❌ No hay código de verificación para: " + username);
                return false;
            }

            // Verificar expiración
            if (user.getVerificationCodeExpiresAt() == null) {
                System.out.println("❌ No hay fecha de expiración para: " + username);
                return false;
            }

            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                System.out.println("❌ Código expirado para: " + username);
                System.out.println("   Expiró: " + user.getVerificationCodeExpiresAt());
                System.out.println("   Ahora: " + LocalDateTime.now());
                return false;
            }

            // Verificar código correcto (comparación case-insensitive por si acaso)
            String storedCode = user.getVerificationCode().trim();
            String providedCode = code.trim();

            System.out.println("🔍 Comparando códigos:");
            System.out.println("   Guardado: '" + storedCode + "' (length: " + storedCode.length() + ")");
            System.out.println("   Recibido: '" + providedCode + "' (length: " + providedCode.length() + ")");

            if (!storedCode.equals(providedCode)) {
                System.out.println("❌ Código incorrecto para: " + username);
                return false;
            }

            // ✅ Verificación exitosa
            System.out.println("✅ Código correcto! Verificando usuario...");
            user.setEmailVerified(true);
            user.setVerificationCode(null); // Limpiar código usado
            user.setVerificationCodeExpiresAt(null);

            User savedUser = userRepository.save(user);

            System.out.println("✅ Usuario verificado y guardado en BD");
            System.out.println("   Email verificado ahora: " + savedUser.getEmailVerified());

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error inesperado al verificar código: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reenviar código de verificación
     */
    @Transactional
    public void resendVerificationCode(String username) {
        System.out.println("📧 Iniciando reenvío de código para: " + username);

        var userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Usuario no encontrado: " + username);
            throw new RuntimeException("Usuario no encontrado");
        }

        User user = userOpt.get();
        System.out.println("✅ Usuario encontrado: " + user.getUsername());
        System.out.println("📧 Email: " + user.getEmail());

        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            System.out.println("⚠️ Usuario ya está verificado");
            throw new RuntimeException("El usuario ya está verificado");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            System.out.println("❌ Usuario no tiene email registrado");
            throw new RuntimeException("El usuario no tiene email registrado");
        }

        // Generar y enviar nuevo código
        createAndSendVerificationCode(user);
        System.out.println("✅ Código reenviado exitosamente");
    }

    /**
     * Verificar si un usuario está verificado
     */
    public boolean isUserVerified(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getEmailVerified() != null && user.getEmailVerified())
                .orElse(false);
    }
}