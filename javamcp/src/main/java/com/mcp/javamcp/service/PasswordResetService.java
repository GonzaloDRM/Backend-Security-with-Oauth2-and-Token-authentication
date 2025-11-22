package com.mcp.javamcp.service;

import com.mcp.javamcp.model.User;
import com.mcp.javamcp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${verification.code.expiration-minutes:15}")
    private int codeExpirationMinutes;

    @Value("${verification.code.length:6}")
    private int codeLength;

    /**
     * Generar código de recuperación aleatorio
     */
    private String generateResetCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    /**
     * Solicitar recuperación de contraseña
     */
    @Transactional
    public void requestPasswordReset(String email) {
        System.out.println("🔑 Solicitando recuperación para email: " + email);

        // Buscar usuario por email
        var userOpt = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            System.out.println("⚠️ Email no encontrado: " + email);
            // Por seguridad, no revelamos si el email existe o no
            // Simplemente no hacemos nada pero no lanzamos error
            return;
        }

        User user = userOpt.get();

        // Verificar que el usuario esté verificado
        if (user.getEmailVerified() == null || !user.getEmailVerified()) {
            System.out.println("❌ Usuario no verificado: " + user.getUsername());
            throw new RuntimeException("Debes verificar tu email primero");
        }

        // Generar código de recuperación
        String resetCode = generateResetCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(codeExpirationMinutes);

        // Reutilizamos los campos de verificación
        user.setVerificationCode(resetCode);
        user.setVerificationCodeExpiresAt(expiresAt);
        userRepository.save(user);

        System.out.println("🔑 Código de recuperación generado: " + resetCode);
        System.out.println("⏰ Expira en: " + expiresAt);

        // Enviar email
        try {
            sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetCode);
            System.out.println("✅ Email de recuperación enviado");
        } catch (Exception e) {
            System.err.println("❌ Error enviando email: " + e.getMessage());
            throw new RuntimeException("Error al enviar email de recuperación", e);
        }
    }

    /**
     * Verificar código de recuperación
     */
    @Transactional
    public boolean verifyResetCode(String email, String code) {
        System.out.println("🔍 Verificando código de recuperación para: " + email);

        var userOpt = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            System.out.println("❌ Email no encontrado");
            return false;
        }

        User user = userOpt.get();

        // Verificar código
        if (user.getVerificationCode() == null || user.getVerificationCode().isBlank()) {
            System.out.println("❌ No hay código de recuperación");
            return false;
        }

        // Verificar expiración
        if (user.getVerificationCodeExpiresAt() == null ||
                user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            System.out.println("❌ Código expirado");
            return false;
        }

        // Verificar código correcto
        if (!user.getVerificationCode().trim().equals(code.trim())) {
            System.out.println("❌ Código incorrecto");
            return false;
        }

        System.out.println("✅ Código de recuperación válido");
        return true;
    }

    /**
     * Restablecer contraseña
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        System.out.println("🔑 Restableciendo contraseña para: " + email);

        // Verificar código primero
        if (!verifyResetCode(email, code)) {
            throw new RuntimeException("Código inválido o expirado");
        }

        var userOpt = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst();

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        User user = userOpt.get();

        // Actualizar contraseña
        user.setPassword(passwordEncoder.encode(newPassword));

        // Limpiar código de recuperación
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        userRepository.save(user);

        System.out.println("✅ Contraseña restablecida exitosamente para: " + user.getUsername());

        // Enviar email de confirmación
        try {
            sendPasswordChangedEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando email de confirmación: " + e.getMessage());
            // No fallar si el email de confirmación falla
        }
    }

    /**
     * Enviar email de recuperación de contraseña
     */
    private void sendPasswordResetEmail(String to, String username, String code) {
        String subject = "Recuperación de Contraseña - MCP App";

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body {
                                    font-family: Arial, sans-serif;
                                    background-color: #f4f4f4;
                                    padding: 20px;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 0 auto;
                                    background: white;
                                    border-radius: 10px;
                                    padding: 40px;
                                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                }
                                .header {
                                    text-align: center;
                                    margin-bottom: 30px;
                                }
                                .code {
                                    background: linear-gradient(135deg, #f59e0b 0%%, #dc2626 100%%);
                                    color: white;
                                    font-size: 32px;
                                    font-weight: bold;
                                    padding: 20px;
                                    border-radius: 10px;
                                    text-align: center;
                                    letter-spacing: 8px;
                                    margin: 30px 0;
                                }
                                .warning {
                                    background: #fef3c7;
                                    border-left: 4px solid #f59e0b;
                                    padding: 15px;
                                    margin: 20px 0;
                                    border-radius: 5px;
                                }
                                .footer {
                                    text-align: center;
                                    color: #666;
                                    font-size: 12px;
                                    margin-top: 30px;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>🔑 Recuperación de Contraseña</h1>
                                </div>

                                <p>Hola <strong>%s</strong>,</p>

                                <p>Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente código:</p>

                                <div class="code">%s</div>

                                <p>Este código expirará en <strong>15 minutos</strong>.</p>

                                <div class="warning">
                                    <strong>⚠️ Importante:</strong> Si no solicitaste este cambio, ignora este mensaje y tu contraseña permanecerá sin cambios.
                                </div>

                                <div class="footer">
                                    <p>© 2025 MCP App. Todos los derechos reservados.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                username, code);

        emailService.sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Enviar email de confirmación de cambio de contraseña
     */
    private void sendPasswordChangedEmail(String to, String username) {
        String subject = "Contraseña Cambiada - MCP App";

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body {
                                    font-family: Arial, sans-serif;
                                    background-color: #f4f4f4;
                                    padding: 20px;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 0 auto;
                                    background: white;
                                    border-radius: 10px;
                                    padding: 40px;
                                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                                }
                                .success {
                                    background: #d1fae5;
                                    border-left: 4px solid #10b981;
                                    padding: 15px;
                                    margin: 20px 0;
                                    border-radius: 5px;
                                }
                                .footer {
                                    text-align: center;
                                    color: #666;
                                    font-size: 12px;
                                    margin-top: 30px;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h1>✅ Contraseña Actualizada</h1>
                                
                                <p>Hola <strong>%s</strong>,</p>

                                <div class="success">
                                    Tu contraseña ha sido cambiada exitosamente.
                                </div>

                                <p>Ya puedes iniciar sesión con tu nueva contraseña.</p>

                                <p>Si no realizaste este cambio, contacta con soporte inmediatamente.</p>

                                <div class="footer">
                                    <p>© 2025 MCP App. Todos los derechos reservados.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                username);

        emailService.sendHtmlEmail(to, subject, htmlContent);
    }
}
