package pe.edu.usmp.aduanatrack.notificacion.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;

/** HU-14 · T-14.2: avisos a los usuarios CLIENTE de la empresa del despacho. */
class AvisoClienteServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 9, 0);
    private static final long EMPRESA = 1L;
    private static final long DESPACHO = 318L;

    private UsuarioRepository usuarios;
    private NotificacionService notificaciones;
    private CorreoService correos;
    private AvisoClienteService servicio;
    private Despacho despacho;
    private Usuario soloSistema;
    private Usuario conCorreo;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        notificaciones = mock(NotificacionService.class);
        correos = mock(CorreoService.class);
        servicio = new AvisoClienteService(usuarios, notificaciones, correos, mock(PlatformTransactionManager.class));

        Cliente empresa = new Cliente("20787789324", AHORA);
        ReflectionTestUtils.setField(empresa, "id", EMPRESA);
        despacho = new Despacho(empresa, usuario(2L, "agente", "AGENTE", CanalNotificacion.SISTEMA), AHORA);
        despacho.abrir("DESP-2026-0318");
        ReflectionTestUtils.setField(despacho, "id", DESPACHO);

        soloSistema = usuario(50L, "jperez", "CLIENTE", CanalNotificacion.SISTEMA);
        conCorreo = usuario(51L, "mrojas", "CLIENTE", CanalNotificacion.AMBOS);
        when(usuarios.clientesActivosDe(EMPRESA)).thenReturn(List.of(soloSistema, conCorreo));
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void canalNaranja_avisaATodosLosUsuariosClienteDeLaEmpresa() {
        servicio.canalAsignado(despacho, CanalControl.NARANJA);

        String mensaje = "DESP-2026-0318: canal naranja. Se programará la inspección en el depósito.";
        verify(notificaciones).notificar(50L, TipoNotificacion.CAMBIO_ESTADO, Severidad.ALTA,
                "Canal de control asignado", mensaje, DESPACHO);
        verify(notificaciones).notificar(51L, TipoNotificacion.CAMBIO_ESTADO, Severidad.ALTA,
                "Canal de control asignado", mensaje, DESPACHO);
    }

    @Test
    void canalVerde_severidadInfoYElAgenteGestionaElLevante() {
        servicio.canalAsignado(despacho, CanalControl.VERDE);

        verify(notificaciones).notificar(50L, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO,
                "Canal de control asignado",
                "DESP-2026-0318: canal verde. Su agente de aduanas gestionará el levante.", DESPACHO);
    }

    @Test
    void inspeccionConforme_yConObservaciones() {
        servicio.inspeccionConforme(despacho);
        servicio.inspeccionConObservaciones(despacho, "Rotulado y marcas (Rotulado incompleto)");

        verify(notificaciones).notificar(50L, TipoNotificacion.INSPECCION_CONFORME, Severidad.INFO,
                "Inspección conforme",
                "DESP-2026-0318: la inspección terminó conforme y el levante quedó habilitado.", DESPACHO);
        verify(notificaciones).notificar(50L, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones",
                "DESP-2026-0318: la inspección terminó con observaciones: Rotulado y marcas (Rotulado incompleto)."
                        + " Su agente de aduanas se pondrá en contacto.",
                DESPACHO);
    }

    @Test
    void levanteOtorgado_yRetiroProgramado() {
        servicio.levanteOtorgado(despacho, "LEV-2026-000318", LocalDate.of(2026, 11, 13));
        servicio.retiroProgramado(despacho, "TK-2026-004999 el 20/11/2026 10:00 en Terminal Callao");

        verify(notificaciones).notificar(50L, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO, "Levante otorgado",
                "DESP-2026-0318: se otorgó el levante LEV-2026-000318 el 13/11/2026.", DESPACHO);
        verify(notificaciones).notificar(50L, TipoNotificacion.CAMBIO_ESTADO, Severidad.INFO, "Retiro programado",
                "DESP-2026-0318: retiro de mercancía programado (cita TK-2026-004999 el 20/11/2026 10:00 en Terminal"
                        + " Callao).",
                DESPACHO);
    }

    @Test
    void elCorreoSaleSegunLaPreferenciaDeCadaUsuario() {
        servicio.inspeccionConforme(despacho);

        String mensaje = "DESP-2026-0318: la inspección terminó conforme y el levante quedó habilitado.";
        verify(correos).enviarSegunPreferencia(CanalNotificacion.SISTEMA, "jperez@ejemplo.pe", "Inspección conforme",
                mensaje);
        verify(correos).enviarSegunPreferencia(CanalNotificacion.AMBOS, "mrojas@ejemplo.pe", "Inspección conforme",
                mensaje);
    }

    // CA3: solo los usuarios de la empresa del despacho; los de otras empresas no reciben nada.
    @Test
    void soloAvisaALosUsuariosDeLaEmpresaDelDespacho() {
        Usuario deOtraEmpresa = usuario(60L, "ajeno", "CLIENTE", CanalNotificacion.AMBOS);
        when(usuarios.clientesActivosDe(2L)).thenReturn(List.of(deOtraEmpresa));

        servicio.levanteOtorgado(despacho, "LEV-2026-000318", LocalDate.of(2026, 11, 13));

        verify(usuarios).clientesActivosDe(EMPRESA);
        verify(usuarios, never()).clientesActivosDe(2L);
        verify(notificaciones, never()).notificar(eq(60L), any(), any(), anyString(), anyString(), anyLong());
        verify(correos, never()).enviarSegunPreferencia(any(), eq("ajeno@ejemplo.pe"), anyString(), anyString());
    }

    @Test
    void empresaSinUsuariosCliente_noAvisaANadie() {
        when(usuarios.clientesActivosDe(EMPRESA)).thenReturn(List.of());

        servicio.inspeccionConforme(despacho);

        verifyNoInteractions(notificaciones, correos);
    }

    @Test
    void despachoSinCliente_noHaceNada() {
        Despacho sinCliente = new Despacho(null, usuario(2L, "agente", "AGENTE", CanalNotificacion.SISTEMA), AHORA);

        servicio.inspeccionConforme(sinCliente);

        verifyNoInteractions(usuarios, notificaciones, correos);
    }

    @Test
    void unFalloAlGuardarElAvisoNoLanzaYNoImpideAvisarALosDemas() {
        doThrow(new IllegalStateException("base caída")).when(notificaciones).notificar(eq(50L), any(), any(),
                anyString(), anyString(), anyLong());

        assertThatCode(() -> servicio.inspeccionConforme(despacho)).doesNotThrowAnyException();

        verify(notificaciones).notificar(eq(51L), any(), any(), anyString(), anyString(), eq(DESPACHO));
    }

    @Test
    void unFalloDelCorreoNoLanzaYSeSigueConElSiguiente() {
        doThrow(new IllegalStateException("SMTP caído")).when(correos).enviarSegunPreferencia(
                eq(CanalNotificacion.SISTEMA), anyString(), anyString(), anyString());

        assertThatCode(() -> servicio.inspeccionConforme(despacho)).doesNotThrowAnyException();

        verify(correos).enviarSegunPreferencia(eq(CanalNotificacion.AMBOS), eq("mrojas@ejemplo.pe"), anyString(),
                anyString());
    }

    @Test
    void unFalloAlBuscarLosDestinatariosNoLanza() {
        when(usuarios.clientesActivosDe(EMPRESA)).thenThrow(new IllegalStateException("base caída"));

        assertThatCode(() -> servicio.inspeccionConforme(despacho)).doesNotThrowAnyException();

        verifyNoInteractions(notificaciones);
    }

    @Test
    void dentroDeUnaTransaccion_losAvisosSalenSoloDespuesDeConfirmarla() {
        TransactionSynchronizationManager.initSynchronization();

        servicio.inspeccionConforme(despacho);

        verifyNoInteractions(notificaciones, correos);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(notificaciones).notificar(eq(50L), any(), any(), anyString(), anyString(), eq(DESPACHO));
    }

    @Test
    void siLaTransaccionSeRevierte_noSaleNingunAviso() {
        TransactionSynchronizationManager.initSynchronization();

        servicio.inspeccionConforme(despacho);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(notificaciones, never()).notificar(anyLong(), any(), any(), anyString(), anyString(), anyLong());
        verifyNoInteractions(correos);
    }

    private static Usuario usuario(Long id, String nombre, String rol, CanalNotificacion canal) {
        Usuario u = new Usuario(nombre, nombre + "@ejemplo.pe", nombre, "hash", new Rol(rol, rol));
        ReflectionTestUtils.setField(u, "id", id);
        u.setCanalNotificacion(canal);
        return u;
    }
}
