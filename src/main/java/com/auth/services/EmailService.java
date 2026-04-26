package com.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Alerta por intentos fallidos
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
                """.formatted(userName, ipAddress, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de alerta enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de alerta a {}: {}", toEmail, e.getMessage());
        }
    }

    // Enlace de recuperación de contraseña
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
                """.formatted(userName, resetLink, resetLink, resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de recuperación enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación a {}: {}", toEmail, e.getMessage());
        }
    }

    // Confirmación de restablecimiento exitoso
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
}