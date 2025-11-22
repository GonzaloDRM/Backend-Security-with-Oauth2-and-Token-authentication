package com.mcp.javamcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JavamcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavamcpApplication.class, args);
	}

}


/* Para usar Gmail necesitás:
Opción A: App Password (Recomendado)

Ve a tu cuenta de Google
Seguridad → Verificación en 2 pasos (activala si no la tenés)
Seguridad → Contraseñas de aplicaciones
Genera una contraseña de aplicación para "Mail"
Usá esa contraseña en el application.properties*/