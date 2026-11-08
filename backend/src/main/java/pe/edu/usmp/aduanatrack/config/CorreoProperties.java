package pe.edu.usmp.aduanatrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Envío de correo de las notificaciones (HU-14 · T-14.2). {@code habilitado} activa el envío; en Render
 * queda en {@code false} salvo que se configure un servidor SMTP, y en local el servidor es Mailpit.
 */
@ConfigurationProperties(prefix = "aduanatrack.correo")
public record CorreoProperties(
        @DefaultValue("false") boolean habilitado,
        @DefaultValue("no-responder@aduanatrack.local") String remitente) {
}
