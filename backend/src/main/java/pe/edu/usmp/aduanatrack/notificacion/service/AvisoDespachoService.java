package pe.edu.usmp.aduanatrack.notificacion.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;

/**
 * Eventos del despacho que avisan a su agente responsable (HU-14 · T-14.2): almacenaje por vencer (CA1) y
 * canal de control asignado (CA2). El cambio de canal también avisa a los usuarios CLIENTE de la empresa del
 * despacho mediante {@link AvisoClienteService}. Crea la notificación dentro del sistema con
 * {@link NotificacionService#notificarAgente} (T-14.1, Fabio) y, según la preferencia del usuario, envía el correo
 * con {@link CorreoService} (CA3). El responsable del despacho es {@code despacho.agente}. */
@Service
public class AvisoDespachoService {

    private final NotificacionService notificaciones;
    private final CorreoService correos;
    private final AvisoClienteService avisosCliente;

    public AvisoDespachoService(NotificacionService notificaciones, CorreoService correos,
            AvisoClienteService avisosCliente) {
        this.notificaciones = notificaciones;
        this.correos = correos;
        this.avisosCliente = avisosCliente;
    }

    /** CA1: alerta «Crítica» al agente cuando al almacenaje le quedan {@code DIAS_ALERTA_ALMACENAJE} días o menos. */
    public void almacenajePorVencer(Despacho despacho, LocalDate fechaLimite, LocalDate hoy) {
        long dias = ChronoUnit.DAYS.between(hoy, fechaLimite);
        avisar(despacho, TipoNotificacion.PLAZO_ALMACENAJE_POR_VENCER, Severidad.CRITICA, "Almacenaje por vencer",
                despacho.getNumero() + " " + plazo(dias));
    }

    /** CA2: avisa al agente del canal que se guardó; naranja y rojo (reconocimiento) son más urgentes que verde. */
    public void canalAsignado(Despacho despacho, CanalControl canal) {
        Severidad severidad = canal == CanalControl.VERDE ? Severidad.INFO : Severidad.ALTA;
        avisar(despacho, TipoNotificacion.CAMBIO_ESTADO, severidad, "Canal de control asignado",
                despacho.getNumero() + ": canal " + canal.name().toLowerCase()
                        + (canal.levanteHabilitado() ? ", ya puede registrar el levante"
                                : ", programe la inspección en el depósito"));
        avisosCliente.canalAsignado(despacho, canal);
    }

    /** «vence en 2 días», «vence en 1 día», «vence hoy» o «venció hace 3 días». */
    static String plazo(long dias) {
        if (dias > 1) {
            return "vence en " + dias + " días";
        }
        if (dias == 1) {
            return "vence en 1 día";
        }
        if (dias == 0) {
            return "vence hoy";
        }
        return "venció hace " + (-dias) + (dias == -1 ? " día" : " días");
    }

    private void avisar(Despacho despacho, TipoNotificacion tipo, Severidad severidad, String titulo,
            String mensaje) {
        notificaciones.notificarAgente(despacho, tipo, severidad, titulo, mensaje);
        Usuario agente = despacho.getAgente();
        correos.enviarSegunPreferencia(agente.getCanalNotificacion(), agente.getCorreo(), titulo, mensaje);
    }
}
