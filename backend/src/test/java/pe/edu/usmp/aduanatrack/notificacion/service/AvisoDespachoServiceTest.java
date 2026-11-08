package pe.edu.usmp.aduanatrack.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;

/** Avisos al agente de HU-14 · T-14.2 (SCRUM-91): lo que se notifica y a quién. */
class AvisoDespachoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 7, 0);

    private NotificacionService notificaciones;
    private CorreoService correos;
    private Despacho despacho;
    private AvisoDespachoService servicio;

    @BeforeEach
    void preparar() {
        notificaciones = mock(NotificacionService.class);
        correos = mock(CorreoService.class);
        Usuario agente = mock(Usuario.class);
        when(agente.getCanalNotificacion()).thenReturn(CanalNotificacion.AMBOS);
        when(agente.getCorreo()).thenReturn("agente@ejemplo.pe");
        despacho = new Despacho(new Cliente("20787789324", AHORA), agente, AHORA);
        despacho.abrir("DESP-2026-0007");
        servicio = new AvisoDespachoService(notificaciones, correos);
    }

    // CA1: un despacho con 2 días de almacenaje restantes genera una alerta «Crítica»

    @Test
    void almacenajeConDosDiasRestantes_creaUnaAlertaCriticaParaElAgenteDelDespacho() {
        servicio.almacenajePorVencer(despacho, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 10));

        verify(notificaciones).notificarAgente(despacho, TipoNotificacion.PLAZO_ALMACENAJE_POR_VENCER, Severidad.CRITICA,
                "Almacenaje por vencer", "DESP-2026-0007 vence en 2 días");
    }

    // CA2: guardar un canal crea una notificación para el agente del despacho

    @Test
    void canalRojo_avisaAlAgenteConSeveridadAlta() {
        servicio.canalAsignado(despacho, CanalControl.ROJO);

        verify(notificaciones).notificarAgente(despacho, TipoNotificacion.CAMBIO_ESTADO, Severidad.ALTA,
                "Canal de control asignado", "DESP-2026-0007: canal rojo, programe la inspección en el depósito");
    }

    @Test
    void canalVerde_avisaConSeveridadInfoYQueYaPuedeRegistrarElLevante() {
        servicio.canalAsignado(despacho, CanalControl.VERDE);

        verify(notificaciones).notificarAgente(despacho, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO,
                "Canal de control asignado", "DESP-2026-0007: canal verde, ya puede registrar el levante");
    }

    // CA3: además de la notificación, el correo según la preferencia del usuario

    @Test
    void cadaAvisoPasaPorElCorreoConElCorreoDelAgente() {
        servicio.almacenajePorVencer(despacho, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 10));

        verify(correos).enviarSegunPreferencia(eq(CanalNotificacion.AMBOS), eq("agente@ejemplo.pe"), eq("Almacenaje por vencer"),
                eq("DESP-2026-0007 vence en 2 días"));
    }

    @Test
    void plazo_describeLosDiasQueFaltan() {
        assertThat(AvisoDespachoService.plazo(3)).isEqualTo("vence en 3 días");
        assertThat(AvisoDespachoService.plazo(1)).isEqualTo("vence en 1 día");
        assertThat(AvisoDespachoService.plazo(0)).isEqualTo("vence hoy");
        assertThat(AvisoDespachoService.plazo(-1)).isEqualTo("venció hace 1 día");
        assertThat(AvisoDespachoService.plazo(-4)).isEqualTo("venció hace 4 días");
    }
}
