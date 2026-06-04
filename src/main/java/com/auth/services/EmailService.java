package com.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;

/**
 * Servicio de envío de correos electrónicos transaccionales para el flujo de
 * autenticación.
 *
 * <p>
 * Todos los métodos son asíncronos ({@code @Async}) para no bloquear el hilo
 * de la petición HTTP mientras se realiza la comunicación con el servidor SMTP.
 * Esto requiere que {@code @EnableAsync} esté activo en la clase principal
 * {@code Application}.
 * </p>
 *
 * <p>
 * Los correos se envían en formato HTML con estilos inline para garantizar
 * compatibilidad con los principales clientes de correo.
 * </p>
 *
 * <p>
 * Configuración requerida en {@code application.yaml}:
 * </p>
 * <ul>
 * <li>{@code spring.mail.username} — dirección de correo remitente</li>
 * <li>{@code spring.mail.host}, {@code port}, {@code properties} —
 * configuración SMTP</li>
 * <li>{@code app.frontend-url} — URL base del frontend para construir los
 * enlaces del correo</li>
 * </ul>
 *
 * @author Equipo Qvenly
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Envía una alerta de seguridad al usuario cuando se detectan múltiples
     * intentos
     * fallidos de inicio de sesión.
     *
     * <p>
     * Se invoca desde {@code AuthService} cuando el número de intentos fallidos
     * supera el umbral configurado ({@code app.max-login-attempts}). El correo
     * incluye
     * la IP detectada y un enlace para cambiar la contraseña.
     * </p>
     *
     * <p>
     * El envío es asíncrono; si falla, se registra el error en el log pero
     * no interrumpe el flujo de autenticación del usuario.
     * </p>
     *
     * @param toEmail   correo electrónico destino del usuario afectado
     * @param userName  nombre del usuario para personalizar el mensaje
     * @param ipAddress dirección IP desde la que se realizaron los intentos
     *                  fallidos
     */
    @Async
    public void sendLoginAlertEmail(String toEmail, String userName, String ipAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Alerta de seguridad: Intentos fallidos de inicio de sesión");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <div style="background-color: #FF4444; padding: 20px; text-align: center;">
                            <h1 style="color: white; margin: 0;">⚠️ Alerta de Seguridad</h1>
                        </div>
                        <div style="padding: 30px; background-color: #f9f9f9;">
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Hemos detectado <strong>múltiples intentos fallidos</strong> de inicio de sesión en tu cuenta.</p>
                            <div style="background-color: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 5px; margin: 20px 0;">
                                <p style="margin: 0;"><strong>IP detectada:</strong> %s</p>
                            </div>
                            <p>Si fuiste tú, puedes ignorar este mensaje. Si <strong>no reconoces esta actividad</strong>, te recomendamos:</p>
                            <ul>
                                <li>Cambiar tu contraseña inmediatamente</li>
                                <li>Revisar los accesos recientes a tu cuenta</li>
                            </ul>
                            <a href="%s/auth/forgot-password"
                               style="background-color: #007bff; color: white; padding: 12px 25px;
                                      text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 10px;">
                                Cambiar contraseña
                            </a>
                        </div>
                        <div style="padding: 15px; text-align: center; color: #888; font-size: 12px;">
                            <p>Este es un correo automático, por favor no respondas.</p>
                        </div>
                    </div>
                    """
                    .formatted(userName, ipAddress, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de alerta enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de alerta a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía el correo de verificación de cuenta tras el registro.
     *
     * <p>
     * El enlace construido tiene la forma:
     * {@code {frontendUrl}/auth/confirm-email?token={verificationToken}}
     * El usuario debe hacer clic para activar su cuenta.
     * </p>
     *
     * @param toEmail           correo electrónico destino
     * @param userName          nombre del usuario para personalizar el mensaje
     * @param verificationToken token UUID generado durante el registro
     */
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String confirmLink = frontendUrl + "/auth/confirm-email?token=" + verificationToken;

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("✉️ Confirma tu cuenta en Qvenly");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <div style="background-color: #14b8a6; padding: 20px; text-align: center;">
                            <h1 style="color: white; margin: 0;">✉️ Confirma tu cuenta</h1>
                        </div>
                        <div style="padding: 30px; background-color: #f9f9f9;">
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Gracias por registrarte en <strong>Qvenly</strong>. Para activar tu cuenta haz clic en el botón:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s"
                                   style="background-color: #14b8a6; color: white; padding: 14px 30px;
                                          text-decoration: none; border-radius: 5px; font-size: 16px; display: inline-block;">
                                    Confirmar mi cuenta
                                </a>
                            </div>
                            <p style="color: #666; font-size: 13px;">
                                Si no puedes hacer clic en el botón, copia y pega este enlace:<br>
                                <a href="%s" style="color: #14b8a6;">%s</a>
                            </p>
                            <p style="color: #999; font-size: 12px; margin-top: 20px;">
                                Si no creaste esta cuenta, puedes ignorar este correo.
                            </p>
                        </div>
                    </div>
                    """
                    .formatted(userName, confirmLink, confirmLink, confirmLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de verificación enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de verificación a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía el correo con el enlace de recuperación de contraseña.
     *
     * <p>
     * El enlace construido tiene la forma:
     * {@code {frontendUrl}/auth/reset-password?token={resetToken}}
     * y es válido por 30 minutos desde su generación.
     * </p>
     *
     * <p>
     * El envío es asíncrono; si falla, se registra el error pero el usuario
     * recibirá igualmente la respuesta genérica del endpoint
     * {@code forgot-password}.
     * </p>
     *
     * @param toEmail    correo electrónico destino
     * @param userName   nombre del usuario para personalizar el mensaje
     * @param resetToken token UUID de recuperación generado por {@code AuthService}
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Recuperación de contraseña");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <div style="background-color: #007bff; padding: 20px; text-align: center;">
                            <h1 style="color: white; margin: 0;">🔐 Recuperar Contraseña</h1>
                        </div>
                        <div style="padding: 30px; background-color: #f9f9f9;">
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta.</p>
                            <p>Haz clic en el siguiente botón para continuar. Este enlace es válido por <strong>30 minutos</strong>.</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s"
                                   style="background-color: #007bff; color: white; padding: 14px 30px;
                                          text-decoration: none; border-radius: 5px; font-size: 16px; display: inline-block;">
                                    Restablecer contraseña
                                </a>
                            </div>
                            <p style="color: #666; font-size: 13px;">
                                Si no puedes hacer clic en el botón, copia y pega este enlace en tu navegador:<br>
                                <a href="%s" style="color: #007bff;">%s</a>
                            </p>
                            <p style="color: #999; font-size: 12px; margin-top: 20px;">
                                Si no solicitaste este cambio, puedes ignorar este correo. Tu contraseña no será modificada.
                            </p>
                        </div>
                    </div>
                    """
                    .formatted(userName, resetLink, resetLink, resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de recuperación enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía una confirmación al usuario notificando que su contraseña fue cambiada
     * exitosamente.
     *
     * <p>
     * Se invoca al final del flujo {@code reset-password} después de que la nueva
     * contraseña ha sido persistida. Incluye un botón de acceso directo al login.
     * </p>
     *
     * <p>
     * El envío es asíncrono y los errores se registran sin interrumpir el flujo.
     * </p>
     *
     * @param toEmail  correo electrónico destino
     * @param userName nombre del usuario para personalizar el mensaje
     */
    @Async
    public void sendPasswordChangedEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("✅ Tu contraseña ha sido cambiada");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <div style="background-color: #28a745; padding: 20px; text-align: center;">
                            <h1 style="color: white; margin: 0;">✅ Contraseña Actualizada</h1>
                        </div>
                        <div style="padding: 30px; background-color: #f9f9f9;">
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Tu contraseña ha sido <strong>restablecida exitosamente</strong>.</p>
                            <p>Ya puedes iniciar sesión con tu nueva contraseña.</p>
                            <div style="text-align: center; margin: 25px 0;">
                                <a href="%s/auth/login"
                                   style="background-color: #28a745; color: white; padding: 12px 25px;
                                          text-decoration: none; border-radius: 5px; display: inline-block;">
                                    Iniciar sesión
                                </a>
                            </div>
                            <p style="color: #999; font-size: 12px;">
                                Si no realizaste este cambio, contacta con soporte de inmediato.
                            </p>
                        </div>
                    </div>
                    """.formatted(userName, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de confirmación de cambio enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar confirmación de cambio a {}: {}", toEmail, e.getMessage());
        }
    }

    private final JavaMailSender javaMailSender;

    /**
     * Envía un correo de notificación al usuario cuando su perfil es desactivado.
     *
     * @param email correo del usuario
     * @param name  nombre del usuario
     * 
     * @author Natali Ramirez
     * @version 1.0
     */
    public void sendProfileDeletedEmail(String email, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo del usuario es obligatorio");
        }

        String userName = (name != null && !name.isBlank()) ? name : "usuario";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Perfil desactivado - Qvenly");
        message.setText(
                "Hola " + userName + ",\n\n" +
                        "Te informamos que tu perfil en Qvenly fue desactivado correctamente.\n\n" +
                        "Si tú no realizaste esta acción, por favor comunícate con soporte.\n\n" +
                        "Atentamente,\n" +
                        "Equipo Qvenly");

        javaMailSender.send(message);
    }
}