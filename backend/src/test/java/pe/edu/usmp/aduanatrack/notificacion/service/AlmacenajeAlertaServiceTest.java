package pe.edu.usmp.aduanatrack.notificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.notificacion.dto.AlmacenajePorVencer;
import pe.edu.usmp.aduanatrack.notificacion.repository.AlmacenajeRepository;

/** Tarea diaria de HU-14 · T-14.2 (SCRUM-91), CA1: almacenaje por vencer. */
class AlmacenajeAlertaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private RelojMutable reloj;
    private AlmacenajeRepository repositorio;
    private AvisoDespachoService avisos;
    private AlmacenajeAlertaService servicio;

    @BeforeEach
    void preparar() {
        // 7:00 del 10/11/2026 en Lima.
        reloj = new RelojMutable(Instant.parse("2026-11-10T12:00:00Z"));
        repositorio = mock(AlmacenajeRepository.class);
        avisos = mock(AvisoDespachoService.class);
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.entero(ParametroService.DIAS_ALERTA_ALMACENAJE)).thenReturn(3);
        servicio = new AlmacenajeAlertaService(repositorio, parametros, avisos, reloj);
    }

    private static Despacho despacho(String numero) {
        Despacho despacho = new Despacho(new Cliente("20787789324", LocalDateTime.of(2026, 10, 1, 9, 0)), null,
                LocalDateTime.of(2026, 10, 1, 9, 0));
        despacho.abrir(numero);
        return despacho;
    }

    @Test
    void alertaLosDespachosSinLevanteConPlazoDeTresDiasOMenos() {
        Despacho enDosDias = despacho("DESP-2026-0007");
        Despacho vencido = despacho("DESP-2026-0003");
        // Límite = hoy + 3 días (DIAS_ALERTA_ALMACENAJE).
        when(repositorio.sinLevanteConPlazoHasta(LocalDate.of(2026, 11, 13), EstadoDespacho.BORRADOR))
                .thenReturn(List.of(new AlmacenajePorVencer(vencido, LocalDate.of(2026, 11, 8)),
                        new AlmacenajePorVencer(enDosDias, LocalDate.of(2026, 11, 12))));

        int alertados = servicio.generarAlertas();

        assertThat(alertados).isEqualTo(2);
        verify(avisos).almacenajePorVencer(vencido, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 10));
        verify(avisos).almacenajePorVencer(enDosDias, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 10));
    }

    @Test
    void sinDespachosPorVencer_noAvisaNada() {
        when(repositorio.sinLevanteConPlazoHasta(LocalDate.of(2026, 11, 13), EstadoDespacho.BORRADOR))
                .thenReturn(List.of());

        assertThat(servicio.generarAlertas()).isZero();

        verify(avisos, never()).almacenajePorVencer(any(), any(), any());
    }

    @Test
    void noRepiteLaAlertaElMismoDia() {
        Despacho despacho = despacho("DESP-2026-0007");
        when(repositorio.sinLevanteConPlazoHasta(any(), any()))
                .thenReturn(List.of(new AlmacenajePorVencer(despacho, LocalDate.of(2026, 11, 12))));

        assertThat(servicio.generarAlertas()).isEqualTo(1);
        assertThat(servicio.generarAlertas()).isZero();

        verify(avisos, times(1)).almacenajePorVencer(any(), any(), any());
    }

    @Test
    void alDiaSiguienteVuelveAAlertarMientrasNoHayaLevante() {
        Despacho despacho = despacho("DESP-2026-0007");
        when(repositorio.sinLevanteConPlazoHasta(any(), any()))
                .thenReturn(List.of(new AlmacenajePorVencer(despacho, LocalDate.of(2026, 11, 12))));

        servicio.generarAlertas();
        reloj.avanzarUnDia();
        assertThat(servicio.generarAlertas()).isEqualTo(1);

        verify(avisos).almacenajePorVencer(despacho, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 11));
        verify(avisos, times(2)).almacenajePorVencer(any(), any(), any());
    }

    @Test
    void siFallaUnAviso_noQuedaMarcadoComoGeneradoYSePuedeReintentar() {
        Despacho despacho = despacho("DESP-2026-0007");
        when(repositorio.sinLevanteConPlazoHasta(any(), any()))
                .thenReturn(List.of(new AlmacenajePorVencer(despacho, LocalDate.of(2026, 11, 12))));
        org.mockito.Mockito.doThrow(new IllegalStateException("base de datos caída")).doNothing().when(avisos)
                .almacenajePorVencer(any(), any(), any());

        try {
            servicio.generarAlertas();
        } catch (IllegalStateException esperado) {
            // La transacción se revierte; la tarea no debe quedar como ejecutada hoy.
        }

        assertThat(servicio.generarAlertas()).isEqualTo(1);
    }

    @Test
    void sePlanificaTodosLosDiasALas7DeLaMananaEnLima() throws Exception {
        Method metodo = AlmacenajeAlertaService.class.getMethod("generarAlertas");
        Scheduled programada = metodo.getAnnotation(Scheduled.class);

        assertThat(programada).isNotNull();
        assertThat(programada.cron()).contains(AlmacenajeAlertaService.CRON_DIARIO);
        CronExpression cron = CronExpression.parse(AlmacenajeAlertaService.CRON_DIARIO);
        ZonedDateTime proxima = cron.next(ZonedDateTime.of(2026, 11, 10, 7, 0, 0, 0, LIMA));
        assertThat(proxima).isEqualTo(ZonedDateTime.of(2026, 11, 11, 7, 0, 0, 0, LIMA));
    }

    /** Reloj que avanza de día en día para probar que la alerta se repite al día siguiente. */
    private static final class RelojMutable extends Clock {
        private Instant ahora;

        RelojMutable(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzarUnDia() {
            ahora = ahora.plusSeconds(24 * 3600L);
        }

        @Override
        public ZoneId getZone() {
            return LIMA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

}
