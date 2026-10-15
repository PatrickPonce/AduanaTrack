package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;

class CartaPoderAlertaServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** 7:00 del 10/10/2026 en Lima. */
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), LIMA);

    private CartaPoderRepository repositorio;
    private CartaPoderAlertaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CartaPoderRepository.class);
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER)).thenReturn(15);
        servicio = new CartaPoderAlertaService(repositorio, parametros, reloj);
    }

    @Test
    void marcaLasCartasQueVencenEn15DiasOMenos() {
        CartaPoder venceEn15 = carta(1L, LocalDate.of(2026, 10, 25));
        CartaPoder venceHoy = carta(2L, LocalDate.of(2026, 10, 10));
        // Límite = hoy + 15 días.
        when(repositorio.porVencerSinAlerta(LocalDate.of(2026, 10, 25))).thenReturn(List.of(venceHoy, venceEn15));

        int alertadas = servicio.generarAlertas();

        assertThat(alertadas).isEqualTo(2);
        assertThat(venceEn15.getAlertaGeneradaEn()).isEqualTo(LocalDateTime.of(2026, 10, 10, 7, 0));
        assertThat(venceHoy.getAlertaGeneradaEn()).isEqualTo(LocalDateTime.of(2026, 10, 10, 7, 0));
    }

    @Test
    void sinCartasPorVencer_noAlertaNada() {
        when(repositorio.porVencerSinAlerta(LocalDate.of(2026, 10, 25))).thenReturn(List.of());

        assertThat(servicio.generarAlertas()).isZero();
    }

    @Test
    void estaProgramadaTodosLosDiasALas7HoraDeLima() throws NoSuchMethodException {
        Method metodo = CartaPoderAlertaService.class.getMethod("generarAlertas");
        Scheduled programacion = metodo.getAnnotation(Scheduled.class);

        assertThat(programacion.cron()).contains(CartaPoderAlertaService.CRON_DIARIO);
        assertThat(programacion.zone()).contains("America/Lima");
        ZonedDateTime siguiente = CronExpression.parse(CartaPoderAlertaService.CRON_DIARIO)
                .next(ZonedDateTime.of(2026, 10, 10, 7, 0, 0, 0, LIMA));
        assertThat(siguiente).isEqualTo(ZonedDateTime.of(2026, 10, 11, 7, 0, 0, 0, LIMA));
    }

    private static CartaPoder carta(Long clienteId, LocalDate vencimiento) {
        return new CartaPoder(clienteId, vencimiento.minusYears(1), vencimiento, "/data/cartas-poder/x.pdf",
                "carta.pdf", 100, "a".repeat(64));
    }
}
