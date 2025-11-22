package com.mcp.javamcp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("📧 EmailService inicializado. Enviando desde: " + fromEmail);
    }

    /**
     * Enviar email simple (texto plano)
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            System.out.println("✅ Email enviado a: " + to);

        } catch (Exception e) {
            System.err.println("❌ Error enviando email: " + e.getMessage());
            throw new RuntimeException("Error al enviar email", e);
        }
    }

    /**
     * Enviar código de verificación
     */
    public void sendVerificationCode(String to, String username, String code) {
        String subject = "Verifica tu cuenta - MCP App";

        String text = String.format("""
                Hola %s,

                Gracias por registrarte en MCP App.

                Tu código de verificación es: %s

                Este código expirará en 15 minutos.

                Si no solicitaste este código, puedes ignorar este mensaje.

                Saludos,
                El equipo de MCP App
                """, username, code);

        sendSimpleEmail(to, subject, text);
    }

    /**
     * Enviar email HTML (más bonito)
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            System.out.println("📧 Preparando email HTML...");
            System.out.println("   Para: " + to);
            System.out.println("   Asunto: " + subject);
            System.out.println("   Desde: " + fromEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Extract to local variables for null-safety
            String from = fromEmail;
            String recipient = to;
            String emailSubject = subject;
            String content = htmlContent;

            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(emailSubject);
            helper.setText(content, true); // true = es HTML

            System.out.println("📤 Enviando email...");
            mailSender.send(message);
            System.out.println("✅ Email HTML enviado exitosamente a: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Error enviando email HTML: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al enviar email HTML: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Error inesperado enviando email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error inesperado al enviar email: " + e.getMessage(), e);
        }
    }

    /**
     * Enviar código de verificación HTML
     */
    public void sendVerificationCodeHtml(String to, String username, String code) {
        String subject = "Verifica tu cuenta - MCP App";

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
                                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                    color: white;
                                    font-size: 32px;
                                    font-weight: bold;
                                    padding: 20px;
                                    border-radius: 10px;
                                    text-align: center;
                                    letter-spacing: 8px;
                                    margin: 30px 0;
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
                                    <h1>🔐 Verifica tu cuenta</h1>
                                </div>

                                <p>Hola <strong>%s</strong>,</p>

                                <p>Gracias por registrarte en MCP App. Para completar tu registro, ingresa el siguiente código:</p>

                                <div class="code">%s</div>

                                <p>Este código expirará en <strong>15 minutos</strong>.</p>

                                <p>Si no solicitaste este código, puedes ignorar este mensaje de forma segura.</p>

                                <div class="footer">
                                    <p>© 2025 MCP App. Todos los derechos reservados.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                username, code);

        sendHtmlEmail(to, subject, htmlContent);
    }
}
