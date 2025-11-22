package com.mcp.javamcp.controller;

import com.mcp.javamcp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:3000")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    /**
     * Endpoint para probar la configuración del email
     */
    @GetMapping("/email-config")
    public ResponseEntity<?> getEmailConfig() {
        return ResponseEntity.ok(Map.of(
                "host", mailHost,
                "port", mailPort,
                "username", mailUsername,
                "configured", mailUsername != null && !mailUsername.isBlank()));
    }

    /**
     * Endpoint para enviar un email de prueba
     */
    @PostMapping("/send-test-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String to = request.get("to");
            if (to == null || to.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Email destinatario es requerido"));
            }

            System.out.println("📧 Intentando enviar email de prueba a: " + to);

            emailService.sendVerificationCodeHtml(to, "Usuario Test", "123456");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email enviado exitosamente a " + to));

        } catch (Exception e) {
            System.err.println("❌ Error enviando email de prueba: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "Error desconocido",
                    "cause", e.getCause() != null ? e.getCause().getMessage() : "No cause"));
        }
    }
}
