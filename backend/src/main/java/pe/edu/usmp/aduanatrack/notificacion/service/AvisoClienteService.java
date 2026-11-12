package pe.edu.usmp.aduanatrack.notificacion.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;

/**
 * Avisos al cliente / importador del despacho (HU-14 · T-14.2): los usuarios CLIENTE activos de la empresa del
 * despacho (HU-16) reciben la notificación en el sistema y, según su preferencia de canal, el correo.
 *
 * <p>Un fallo al avisar nunca revierte la operación del despacho: los avisos se envían <b>después de confirmar</b>
 * la transacción de quien llama (si esa se revierte, no sale ningún aviso) y cada destinatario se guarda en su propia
 * transacción, con los errores capturados y registrados en el log.
 */
@Service
public class AvisoClienteService {

    private static final Logger log = LoggerFactory.getLogger(AvisoClienteService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarios;
    private final NotificacionService notificaciones;
    private final CorreoService correos;
    private final PlatformTransactionManager transacciones;

    public AvisoClienteService(UsuarioRepository usuarios, NotificacionService notificaciones,
            CorreoService correos, PlatformTransactionManager transacciones) {
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.correos = correos;
        this.transacciones = transacciones;
    }

    /** Cambio de canal de control (CA2). */
    public void canalAsignado(Despacho despacho, CanalControl canal) {
        Severidad severidad = canal == CanalControl.VERDE ? Severidad.INFO : Severidad.ALTA;
        avisar(despacho, TipoNotificacion.CAMBIO_ESTADO, severidad, "Canal de control asignado",
                despacho.getNumero() + ": canal " + canal.name().toLowerCase()
                        + (canal.levanteHabilitado() ? ". Su agente de aduanas gestionará el levante."
                                : ". Se programará la inspección en el depósito."));
    }

    /** Inspección conforme: el levante queda habilitado. */
    public void inspeccionConforme(Despacho despacho) {
        avisar(despacho, TipoNotificacion.INSPECCION_CONFORME, Severidad.INFO, "Inspección conforme",
                despacho.getNumero() + ": la inspección terminó conforme y el levante quedó habilitado.");
    }

    /** Inspección con observaciones; {@code detalle} resume los ítems no conformes. */
    public void inspeccionConObservaciones(Despacho despacho, String detalle) {
        avisar(despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones", despacho.getNumero() + ": la inspección terminó con observaciones: "
                        + detalle + ". Su agente de aduanas se pondrá en contacto.");
    }

    /** Levante otorgado. */
    public void levanteOtorgado(Despacho despacho, String numeroLevante, LocalDate fecha) {
        avisar(despacho, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO, "Levante otorgado",
                despacho.getNumero() + ": se otorgó el levante " + numeroLevante + " el " + FECHA.format(fecha) + ".");
    }

    /** Retiro de la mercancía programado; {@code detalle} trae la cita, la fecha y el depósito. */
    public void retiroProgramado(Despacho despacho, String detalle) {
        avisar(despacho, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO, "Retiro programado",
                despacho.getNumero() + ": retiro de mercancía programado (cita " + detalle + ").");
    }

    private void avisar(Despacho despacho, TipoNotificacion tipo, Severidad severidad, String titulo,
            String mensaje) {
        if (despacho.getCliente() == null) {
            return;
        }
        Long clienteId = despacho.getCliente().getId();
        Long despachoId = despacho.getId();
        Runnable envio = () -> enviar(clienteId, despachoId, tipo, severidad, titulo, mensaje);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    /** Nunca lanza: cualquier error queda en el log. */
    private void enviar(Long clienteId, Long despachoId, TipoNotificacion tipo, Severidad severidad,
            String titulo, String mensaje) {
        List<Destinatario> destinatarios;
        try {
            destinatarios = usuarios.clientesActivosDe(clienteId).stream().map(Destinatario::de).toList();
        } catch (RuntimeException e) {
            log.warn("No se pudo buscar a los usuarios cliente de la empresa {} para avisar «{}»: {}", clienteId,
                    titulo, e.getMessage());
            return;
        }
        TransactionTemplate propia = new TransactionTemplate(transacciones);
        propia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Destinatario destinatario : destinatarios) {
            try {
                propia.executeWithoutResult(estado -> notificaciones.notificar(destinatario.id(), tipo, severidad,
                        titulo, mensaje, despachoId));
            } catch (RuntimeException e) {
                log.warn("No se pudo guardar el aviso «{}» para el usuario {}: {}", titulo, destinatario.id(),
                        e.getMessage());
            }
            try {
                correos.enviarSegunPreferencia(destinatario.canal(), destinatario.correo(), titulo, mensaje);
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el correo «{}» al usuario {}: {}", titulo, destinatario.id(),
                        e.getMessage());
            }
        }
    }

    private record Destinatario(Long id, String correo, CanalNotificacion canal) {

        static Destinatario de(Usuario u) {
            return new Destinatario(u.getId(), u.getCorreo(), u.getCanalNotificacion());
        }
    }
}
