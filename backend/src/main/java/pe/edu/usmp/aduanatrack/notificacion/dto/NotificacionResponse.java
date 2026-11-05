package pe.edu.usmp.aduanatrack.notificacion.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.notificacion.entity.Notificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;

/**
 * Notificación de la bandeja (contrato C9). {@code despachoId} y {@code numeroDespacho} son nulos en los
 * avisos que no son de un despacho; con ellos la pantalla arma el enlace al detalle.
 */
public record NotificacionResponse(Long id, TipoNotificacion tipo, Severidad severidad, String titulo,
        String mensaje, Long despachoId, String numeroDespacho, boolean leida, LocalDateTime creadaEn,
        LocalDateTime leidaEn) {

    public static NotificacionResponse de(Notificacion n) {
        Despacho despacho = n.getDespacho();
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getSeveridad(), n.getTitulo(), n.getMensaje(),
                despacho == null ? null : despacho.getId(), despacho == null ? null : despacho.getNumero(),
                n.isLeida(), n.getCreadaEn(), n.getLeidaEn());
    }
}
