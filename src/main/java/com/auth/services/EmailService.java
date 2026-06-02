package com.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de envío de correos electrónicos transaccionales para el flujo de autenticación.
 *
 * <p>Todos los métodos son asíncronos ({@code @Async}) para no bloquear el hilo
 * de la petición HTTP mientras se realiza la comunicación con el servidor SMTP.</p>
 *
 * <p>Los correos se envían en formato HTML con estilos inline para garantizar
 * compatibilidad con los principales clientes de correo.</p>
 *
 * @author Equipo Qvenly
 * @version 2.0
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

    // ─── Colores de la identidad Qvenly ──────────────────────────────────────
    private static final String COLOR_PRIMARY    = "#14b8a6";   // teal-500
    private static final String COLOR_PRIMARY_DK = "#0d9488";   // teal-600
    private static final String COLOR_DANGER     = "#ef4444";   // red-500
    private static final String COLOR_WARNING_BG = "#fffbeb";
    private static final String COLOR_WARNING_BD = "#fde68a";
    private static final String COLOR_SUCCESS    = "#14b8a6";
    private static final String COLOR_TEXT       = "#111827";   // gray-900
    private static final String COLOR_TEXT_SOFT  = "#6b7280";   // gray-500
    private static final String COLOR_BG         = "#f9fafb";   // gray-50
    private static final String COLOR_WHITE      = "#ffffff";
    private static final String COLOR_BORDER     = "#e5e7eb";   // gray-200

    // ─── Plantilla base compartida ────────────────────────────────────────────
    private String baseTemplate(String headerColor, String headerContent,
                                String bodyContent, String footerNote) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Qvenly</title>
            </head>
            <body style="margin:0; padding:0; background-color:%s; font-family:'Segoe UI', Arial, sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:%s; padding: 40px 16px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="max-width:600px; width:100%%; background-color:%s;
                                  border-radius:12px; overflow:hidden;
                                  box-shadow: 0 4px 24px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background-color:%s; padding: 32px 40px; text-align:center;">
                          <p style="margin:0 0 12px; font-size:13px; font-weight:600;
                                    color:rgba(255,255,255,0.8); letter-spacing:2px; text-transform:uppercase;">
                            QVENLY
                          </p>
                          %s
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding: 40px;">
                          %s
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="padding: 24px 40px; border-top: 1px solid %s; text-align:center;">
                          <p style="margin:0 0 6px; font-size:12px; color:%s;">
                            %s
                          </p>
                          <p style="margin:0; font-size:12px; color:%s;">
                            &copy; 2025 Qvenly &mdash; Plataforma de gesti&oacute;n de eventos
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                COLOR_BG, COLOR_BG, COLOR_WHITE,
                headerColor, headerContent,
                bodyContent,
                COLOR_BORDER, COLOR_TEXT_SOFT, footerNote, COLOR_TEXT_SOFT
        );
    }

    // ─── Componentes reutilizables ────────────────────────────────────────────
    private String headerTitle(String title) {
        return "<h1 style=\"margin:0; font-size:22px; font-weight:700; color:" + COLOR_WHITE + ";\">"
                + title + "</h1>";
    }

    private String greeting(String userName) {
        return "<p style=\"margin:0 0 16px; font-size:16px; color:" + COLOR_TEXT + ";\">Hola <strong>" + userName + "</strong>,</p>";
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 16px; font-size:15px; color:" + COLOR_TEXT_SOFT + "; line-height:1.6;\">" + text + "</p>";
    }

    private String ctaButton(String href, String label, String color) {
        return """
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 28px 0;">
              <tr>
                <td align="center">
                  <a href="%s"
                     style="display:inline-block; background-color:%s; color:%s;
                            padding: 14px 36px; border-radius:8px; font-size:15px;
                            font-weight:600; text-decoration:none; letter-spacing:0.3px;">
                    %s
                  </a>
                </td>
              </tr>
            </table>
            """.formatted(href, color, COLOR_WHITE, label);
    }

    private String fallbackLink(String href, String color) {
        return """
            <p style="margin: 0 0 8px; font-size:13px; color:%s;">
              Si el bot&oacute;n no funciona, copia y pega este enlace en tu navegador:
            </p>
            <p style="margin:0; word-break:break-all;">
              <a href="%s" style="font-size:13px; color:%s;">%s</a>
            </p>
            """.formatted(COLOR_TEXT_SOFT, href, color, href);
    }

    private String divider() {
        return "<hr style=\"border:none; border-top:1px solid " + COLOR_BORDER + "; margin: 24px 0;\">";
    }

    private String smallNote(String text) {
        return "<p style=\"margin:0; font-size:12px; color:" + COLOR_TEXT_SOFT + "; line-height:1.6;\">" + text + "</p>";
    }

    // ─── Correo 1: Verificación de cuenta ────────────────────────────────────
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String confirmLink = frontendUrl + "/auth/confirm-email?token=" + verificationToken;

            String header = headerTitle("Confirma tu cuenta");

            String body = greeting(userName)
                    + paragraph("Gracias por registrarte en <strong style=\"color:" + COLOR_TEXT + ";\">Qvenly</strong>. "
                    + "Para activar tu cuenta y comenzar a gestionar tus eventos, confirma tu direcci&oacute;n de correo.")
                    + ctaButton(confirmLink, "Confirmar mi cuenta", COLOR_PRIMARY)
                    + divider()
                    + fallbackLink(confirmLink, COLOR_PRIMARY)
                    + divider()
                    + smallNote("Si no creaste esta cuenta, puedes ignorar este correo de forma segura.");

            String html = baseTemplate(COLOR_PRIMARY, header, body,
                    "Este es un correo autom&aacute;tico, por favor no respondas.");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Confirma tu cuenta en Qvenly");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de verificacion enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de verificacion a {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Correo 2: Recuperación de contraseña ────────────────────────────────
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;

            String header = headerTitle("Recupera tu contrase&ntilde;a");

            String body = greeting(userName)
                    + paragraph("Recibimos una solicitud para restablecer la contrase&ntilde;a de tu cuenta.")
                    + paragraph("Haz clic en el bot&oacute;n para continuar. "
                    + "Este enlace es v&aacute;lido por <strong style=\"color:" + COLOR_TEXT + ";\">30 minutos</strong>.")
                    + ctaButton(resetLink, "Restablecer contrase&ntilde;a", COLOR_PRIMARY)
                    + divider()
                    + fallbackLink(resetLink, COLOR_PRIMARY)
                    + divider()
                    + smallNote("Si no solicitaste este cambio, puedes ignorar este correo. "
                    + "Tu contrase&ntilde;a no ser&aacute; modificada.");

            String html = baseTemplate(COLOR_PRIMARY, header, body,
                    "Este es un correo autom&aacute;tico, por favor no respondas.");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperacion de contrasena - Qvenly");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de recuperacion enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de recuperacion a {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Correo 3: Contraseña cambiada exitosamente ───────────────────────────
    @Async
    public void sendPasswordChangedEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String header = headerTitle("Contrase&ntilde;a actualizada");

            String successBox = """
                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background-color:#f0fdfa; border:1px solid #99f6e4;
                              border-radius:8px; margin: 20px 0;">
                  <tr>
                    <td style="padding: 16px 20px;">
                      <p style="margin:0; font-size:14px; color:#0d9488; font-weight:500;">
                        Tu contrase&ntilde;a ha sido restablecida exitosamente.
                      </p>
                    </td>
                  </tr>
                </table>
                """;

            String body = greeting(userName)
                    + paragraph("Tu contrase&ntilde;a ha sido <strong style=\"color:" + COLOR_TEXT + ";\">actualizada con &eacute;xito</strong>. "
                    + "Ya puedes iniciar sesi&oacute;n con tus nuevas credenciales.")
                    + successBox
                    + ctaButton(frontendUrl + "/auth/login", "Iniciar sesi&oacute;n", COLOR_PRIMARY)
                    + divider()
                    + smallNote("Si no realizaste este cambio, contacta con nuestro equipo de soporte de inmediato.");

            String html = baseTemplate(COLOR_PRIMARY, header, body,
                    "Este es un correo autom&aacute;tico, por favor no respondas.");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Tu contrasena ha sido actualizada - Qvenly");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de confirmacion de cambio enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar confirmacion de cambio a {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Correo 4: Alerta de seguridad ────────────────────────────────────────
    @Async
    public void sendLoginAlertEmail(String toEmail, String userName, String ipAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String header = headerTitle("Alerta de seguridad");

            String warningBox = """
                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background-color:%s; border:1px solid %s;
                              border-radius:8px; margin: 20px 0;">
                  <tr>
                    <td style="padding: 16px 20px;">
                      <p style="margin:0 0 6px; font-size:13px; font-weight:600; color:#92400e;">
                        Informacion del intento
                      </p>
                      <p style="margin:0; font-size:14px; color:#78350f;">
                        IP detectada: <strong>%s</strong>
                      </p>
                    </td>
                  </tr>
                </table>
                """.formatted(COLOR_WARNING_BG, COLOR_WARNING_BD, ipAddress);

            String body = greeting(userName)
                    + paragraph("Hemos detectado <strong style=\"color:" + COLOR_TEXT + ";\">m&uacute;ltiples intentos fallidos</strong> "
                    + "de inicio de sesi&oacute;n en tu cuenta.")
                    + warningBox
                    + paragraph("Si fuiste t&uacute;, puedes ignorar este mensaje. "
                    + "Si <strong style=\"color:" + COLOR_TEXT + ";\">no reconoces esta actividad</strong>, "
                    + "te recomendamos cambiar tu contrase&ntilde;a de inmediato.")
                    + ctaButton(frontendUrl + "/auth/forgot-password", "Cambiar contrase&ntilde;a", COLOR_DANGER)
                    + divider()
                    + smallNote("Si no reconoces esta actividad, contacta con nuestro equipo de soporte.");

            String html = baseTemplate(COLOR_DANGER, header, body,
                    "Este es un correo autom&aacute;tico de seguridad, por favor no respondas.");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Alerta de seguridad: Intentos fallidos de inicio de sesion - Qvenly");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de alerta enviado a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error al enviar correo de alerta a {}: {}", toEmail, e.getMessage());
        }
    }
}
