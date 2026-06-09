package com.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;

/**
 * Servicio de envío de correos electrónicos transaccionales para el flujo de autenticación.
 *
 * <p>Gestiona cuatro tipos de correos HTML enviados en eventos clave del ciclo de autenticación:</p>
 * <ol>
 *   <li><b>Verificación de cuenta</b> — enviado tras el registro para activar la cuenta.</li>
 *   <li><b>Recuperación de contraseña</b> — enlace con TTL de 30 minutos para restablecer.</li>
 *   <li><b>Confirmación de cambio</b> — notifica al usuario que su contraseña fue cambiada.</li>
 *   <li><b>Alerta de seguridad</b> — notifica intentos fallidos de inicio de sesión.</li>
 * </ol>
 *
 * <p>Todos los métodos son <b>asíncronos</b> ({@code @Async}) para no bloquear el hilo
 * de la petición HTTP mientras se realiza la comunicación con el servidor SMTP.
 * Los errores de envío se registran en el log sin propagar la excepción.</p>
 *
 * <p>Los correos usan una plantilla HTML base con la identidad visual de Qvenly
 * (colores teal) y estilos inline para garantizar compatibilidad con los
 * principales clientes de correo electrónico.</p>
 *
 * <p>Configuración requerida en {@code application.yaml}:</p>
 * <ul>
 *   <li>{@code spring.mail.username} — dirección de remitente.</li>
 *   <li>{@code app.frontend-url} — URL base del frontend para construir los enlaces.</li>
 * </ul>
 *
 * @author Leidy Martinez
 * @version 3.0
 * @see AuthService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /** Cliente SMTP de Spring para crear y enviar mensajes de correo. */
    private final JavaMailSender mailSender;

    /** Dirección de correo remitente configurada en {@code spring.mail.username}. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /** URL base del frontend para construir los enlaces en los correos. */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ─── Paleta de colores de la identidad visual Qvenly ─────────────────────
    private static final String COLOR_PRIMARY    = "#14b8a6";
    private static final String COLOR_PRIMARY_DK = "#0d9488";
    private static final String COLOR_DANGER     = "#ef4444";
    private static final String COLOR_WARNING_BG = "#fffbeb";
    private static final String COLOR_WARNING_BD = "#fde68a";
    private static final String COLOR_TEXT       = "#111827";
    private static final String COLOR_TEXT_SOFT  = "#6b7280";
    private static final String COLOR_BG         = "#f9fafb";
    private static final String COLOR_WHITE      = "#ffffff";
    private static final String COLOR_BORDER     = "#e5e7eb";

    // ─────────────────────────────────────────────────────────────────────
    // CORREOS PÚBLICOS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Envía el correo de verificación de cuenta tras el registro.
     *
     * <p>Incluye un botón con el enlace de confirmación que apunta a
     * {@code {frontendUrl}/auth/confirm-email?token={verificationToken}}.
     * El enlace es de un solo uso y se invalida tras la confirmación exitosa.</p>
     *
     * @param toEmail           dirección de destino del correo
     * @param userName          nombre del usuario para personalizar el saludo
     * @param verificationToken token UUID generado durante el registro
     */
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

    /**
     * Envía el correo de recuperación de contraseña.
     *
     * <p>Incluye un botón con el enlace de restablecimiento que apunta a
     * {@code {frontendUrl}/auth/reset-password?token={resetToken}}.
     * El enlace expira en 30 minutos y es de un solo uso.</p>
     *
     * @param toEmail    dirección de destino del correo
     * @param userName   nombre del usuario para personalizar el saludo
     * @param resetToken token UUID de recuperación generado por {@code AuthService}
     */
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

    /**
     * Envía el correo de confirmación tras un restablecimiento exitoso de contraseña.
     *
     * <p>Notifica al usuario que su contraseña fue cambiada e incluye un botón
     * para ir al login. Si el usuario no realizó el cambio, se le recomienda
     * contactar con soporte.</p>
     *
     * @param toEmail  dirección de destino del correo
     * @param userName nombre del usuario para personalizar el saludo
     */
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

    /**
     * Envía una alerta de seguridad cuando se detectan múltiples intentos fallidos de login.
     *
     * <p>Se dispara cuando el número de intentos fallidos en los últimos 15 minutos
     * supera el umbral configurado en {@code app.max-login-attempts}.
     * Incluye la IP desde la que se realizaron los intentos para que el usuario
     * pueda identificar si es actividad sospechosa.</p>
     *
     * @param toEmail   dirección de destino del correo
     * @param userName  nombre del usuario para personalizar el saludo
     * @param ipAddress dirección IP desde la que se detectaron los intentos fallidos
     */
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

    // ─────────────────────────────────────────────────────────────────────
    // PLANTILLA Y COMPONENTES INTERNOS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Genera el HTML completo del correo usando la plantilla base de Qvenly.
     *
     * @param headerColor    color de fondo del encabezado (teal o rojo según el tipo de correo)
     * @param headerContent  HTML del título en el encabezado
     * @param bodyContent    HTML del contenido principal del correo
     * @param footerNote     texto informativo del pie de página
     * @return cadena HTML completa lista para enviar
     */
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
                      <tr>
                        <td style="background-color:%s; padding: 32px 40px; text-align:center;">
                          <p style="margin:0 0 12px; font-size:13px; font-weight:600;
                                    color:rgba(255,255,255,0.8); letter-spacing:2px; text-transform:uppercase;">
                            QVENLY
                          </p>
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 40px;">
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 24px 40px; border-top: 1px solid %s; text-align:center;">
                          <p style="margin:0 0 6px; font-size:12px; color:%s;">%s</p>
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

    /** Genera el HTML del título del encabezado. */
    private String headerTitle(String title) {
        return "<h1 style=\"margin:0; font-size:22px; font-weight:700; color:" + COLOR_WHITE + ";\">"
                + title + "</h1>";
    }

    /** Genera el HTML del saludo personalizado con el nombre del usuario. */
    private String greeting(String userName) {
        return "<p style=\"margin:0 0 16px; font-size:16px; color:" + COLOR_TEXT + ";\">Hola <strong>" + userName + "</strong>,</p>";
    }

    /** Genera el HTML de un párrafo de texto con el estilo estándar. */
    private String paragraph(String text) {
        return "<p style=\"margin:0 0 16px; font-size:15px; color:" + COLOR_TEXT_SOFT + "; line-height:1.6;\">" + text + "</p>";
    }

    /**
     * Genera el HTML de un botón de llamada a la acción (CTA).
     *
     * @param href  URL de destino del botón
     * @param label texto visible del botón
     * @param color color de fondo del botón
     * @return HTML del botón centrado con estilos inline
     */
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

    /** Genera el HTML del enlace alternativo para clientes que no renderizan botones. */
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

    /** Genera el HTML de una línea divisoria horizontal. */
    private String divider() {
        return "<hr style=\"border:none; border-top:1px solid " + COLOR_BORDER + "; margin: 24px 0;\">";
    }

    /** Genera el HTML de una nota pequeña en texto gris. */
    private String smallNote(String text) {
        return "<p style=\"margin:0; font-size:12px; color:" + COLOR_TEXT_SOFT + "; line-height:1.6;\">" + text + "</p>";
    }

    /**
     * Envía correo simple notificando que la contraseña fue cambiada desde el perfil.
     * Usa SimpleMailMessage (texto plano) a diferencia del HTML de sendPasswordChangedEmail.
     *
     * @param email correo del usuario
     * @param name  nombre del usuario
     */
    @Async
    public void sendPasswordChangedProfileEmail(String email, String name) {
        if (email == null || email.isBlank()) return;
        String userName = (name != null && !name.isBlank()) ? name : "usuario";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Contrasena actualizada - Qvenly");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "Te informamos que la contrasena de tu cuenta fue actualizada correctamente.\n\n" +
                            "Si tu no realizaste este cambio, comunicate con soporte de inmediato.\n\n" +
                            "Atentamente,\nEquipo Qvenly");
            mailSender.send(message);
            log.info("Correo de cambio de contrasena (perfil) enviado a: {}", email);
        } catch (Exception e) {
            log.error("Error al enviar correo de cambio de contrasena a {}: {}", email, e.getMessage());
            throw e; // propagar para permitir reintento
        }
    }

    /**
     * Envía correo notificando que el perfil será eliminado.
     * Se envía ANTES de la eliminación para que el correo llegue.
     * Propaga excepciones para permitir reintento.
     *
     * @param email correo del usuario
     * @param name  nombre del usuario
     */
    @Async
    public void sendProfileDeletedBeforeDeletionEmail(String email, String name) {
        if (email == null || email.isBlank()) return;
        String userName = (name != null && !name.isBlank()) ? name : "usuario";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Eliminacion de perfil - Qvenly");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "Te informamos que tu perfil en Qvenly sera eliminado y desvinculado del sistema.\n\n" +
                            "Si tu no solicitaste esta eliminacion, comunicate con soporte de inmediato.\n\n" +
                            "Atentamente,\nEquipo Qvenly");
            mailSender.send(message);
            log.info("Correo de eliminacion de perfil enviado a: {}", email);
        } catch (Exception e) {
            log.error("Error al enviar correo de eliminacion a {}: {}", email, e.getMessage());
            throw e;
        }
    }

    /**
     * Envía correo notificando que el perfil fue desactivado (eliminación lógica).
     *
     * @param email correo del usuario
     * @param name  nombre del usuario
     */
    @Async
    public void sendProfileDeletedEmail(String email, String name) {
        if (email == null || email.isBlank()) return;
        String userName = (name != null && !name.isBlank()) ? name : "usuario";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Perfil desactivado - Qvenly");
            message.setText(
                    "Hola " + userName + ",\n\n" +
                            "Tu perfil en Qvenly fue desactivado correctamente.\n\n" +
                            "Si tu no realizaste esta accion, comunicate con soporte.\n\n" +
                            "Atentamente,\nEquipo Qvenly");
            mailSender.send(message);
            log.info("Correo de desactivacion enviado a: {}", email);
        } catch (Exception e) {
            log.error("Error al enviar correo de desactivacion a {}: {}", email, e.getMessage());
        }
    }
}