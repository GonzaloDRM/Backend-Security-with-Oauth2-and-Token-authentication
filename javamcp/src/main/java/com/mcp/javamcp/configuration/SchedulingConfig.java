package com.mcp.javamcp.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar tareas programadas (@Scheduled)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Esta clase habilita las tareas programadas en toda la aplicación
}
