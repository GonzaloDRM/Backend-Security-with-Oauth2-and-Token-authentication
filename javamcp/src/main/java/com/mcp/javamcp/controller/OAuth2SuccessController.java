package com.mcp.javamcp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Controller
public class OAuth2SuccessController {

    @Autowired
    private JwtEncoder jwtEncoder;

    /**
     * Endpoint que maneja el éxito de autenticación (Form Login y OAuth2)
     * Genera JWT y redirige al frontend con el token
     */
    @GetMapping("/oauth2/success")
    public String oauth2Success(Authentication authentication) {
        System.out.println("Autenticación exitosa para: " + authentication.getName());
        System.out.println("Tipo de autenticación: " + authentication.getClass().getSimpleName());

        try {
            String username;
            String email = null;
            String provider = "local";

            // Detectar si es OAuth2 o Form Login
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                OAuth2User oauth2User = oauth2Token.getPrincipal();
                provider = oauth2Token.getAuthorizedClientRegistrationId(); // "google" o "github"

                // Extraer información según el provider
                if ("google".equals(provider)) {
                    username = oauth2User.getAttribute("name");
                    email = oauth2User.getAttribute("email");
                } else if ("github".equals(provider)) {
                    username = oauth2User.getAttribute("login");
                    email = oauth2User.getAttribute("email");
                } else {
                    username = oauth2User.getAttribute("name");
                }

                System.out.println("🔐 Login con " + provider.toUpperCase());
                System.out.println("👤 Usuario: " + username);
                System.out.println("📧 Email: " + email);

            } else {
                // Form Login tradicional
                username = authentication.getName();
                System.out.println("🔐 Login tradicional: " + username);
            }

            // Generar JWT
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                    .issuer("http://localhost:8080")
                    .subject(username)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .claim("provider", provider)
                    .claim("roles", authentication.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .collect(Collectors.toList()));

            // Agregar email si existe
            if (email != null) {
                claimsBuilder.claim("email", email);
            }

            String token = jwtEncoder.encode(
                    JwtEncoderParameters.from(claimsBuilder.build())
            ).getTokenValue();

            System.out.println("✅ Token JWT generado");

            // Redirigir al frontend con el token
            return "redirect:http://localhost:3000/dashboard.html?token=" + token;

        } catch (Exception e) {
            System.err.println("❌ Error generando token: " + e.getMessage());
            e.printStackTrace();
            return "redirect:http://localhost:3000/login.html?error=true";
        }
    }
}