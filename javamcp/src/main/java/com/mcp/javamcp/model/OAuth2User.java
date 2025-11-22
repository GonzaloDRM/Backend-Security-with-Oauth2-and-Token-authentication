package com.mcp.javamcp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "oauth2_users")
public class OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String name;

    private String picture;

    @Column(nullable = false)
    private String provider;

    @Column(unique = true)
    private String providerId;

    private LocalDateTime firstLogin;

    private LocalDateTime lastLogin;

    private Integer loginCount = 0;

    @Column(columnDefinition = "TEXT")
    private String roles = "USER";
}