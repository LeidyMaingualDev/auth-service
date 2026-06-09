package com.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de arranque del microservicio de autenticación.
 *
 * <p>Este microservicio expone endpoints REST para el ciclo completo de autenticación
 * de usuarios: registro, inicio de sesión, recuperación de contraseña, renovación de
 * tokens y cierre de sesión. Se integra con Consul para el descubrimiento de servicios
 * y utiliza JWT como mecanismo de autenticación sin estado (stateless).</p>
 *
 * <p>La anotación {@code @EnableAsync} habilita el procesamiento asíncrono de tareas,
 * utilizado principalmente por {@code EmailService} para el envío de correos
 * electrónicos sin bloquear el hilo principal de la solicitud HTTP.</p>
 *
 * @author Equipo Qvenly
 * @version 1.0
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.scheduling.annotation.EnableAsync
 */
@EnableAsync
@SpringBootApplication
@EnableScheduling
public class Application {

    /**
     * Punto de entrada de la aplicación Spring Boot.
     *
     * @param args argumentos de línea de comandos pasados al arrancar la JVM
     */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
