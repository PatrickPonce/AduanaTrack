package pe.edu.usmp.aduanatrack.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.config.CorreoProperties;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;

/**
 * Envío de las notificaciones por correo (HU-14 · T-14.2, CA3). Solo envía si la propiedad
 * {@code aduanatrack.correo.habilitado} está activa y la preferencia del usuario es CORREO o AMBOS. Un fallo
 * del servidor de correo se registra en el log pero no interrumpe la operación del despacho que lo originó.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> remitente;
    private final CorreoProperties propiedades;

    public CorreoService(ObjectProvider<JavaMailSender> remitente, CorreoProperties propiedades) {
        this.remitente = remitente;
        this.propiedades = propiedades;
    }

    /**
     * Envía el correo si la preferencia del usuario es {@code CORREO} o {@code AMBOS}.
     *
     * @param canal preferencia guardada del usuario: SISTEMA, CORREO o AMBOS
     * @return {@code true} si el correo salió hacia el servidor SMTP
     */
    public boolean enviarSegunPreferencia(CanalNotificacion canal, String destinatario, String asunto,
            String texto) {
        if (canal == null || !canal.incluyeCorreo()) {
            return false;
        }
        return enviar(destinatario, asunto, texto);
    }

    /** Envío directo (lo usa también el envío programado de reportes, HU-15); {@code false} si no se envió. */
    public boolean enviar(String destinatario, String asunto, String texto) {
        if (!propiedades.habilitado()) {
            log.debug("Correo desactivado: no se envía «{}»", asunto);
            return false;
        }
        JavaMailSender enviador = remitente.getIfAvailable();
        if (enviador == null || destinatario == null || destinatario.isBlank()) {
            log.warn("No se envía «{}»: falta el servidor SMTP o el destinatario", asunto);
            return false;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedades.remitente());
        mensaje.setTo(destinatario.trim());
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        try {
            enviador.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo «{}»: {}", asunto, e.getMessage());
            return false;
        }
    }
}
