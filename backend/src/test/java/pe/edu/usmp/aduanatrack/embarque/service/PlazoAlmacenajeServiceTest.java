package pe.edu.usmp.aduanatrack.embarque.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService.Plazo;

class PlazoAlmacenajeServiceTest {

    private PlazoAlmacenajeService servicio;

    @BeforeEach
    void preparar() {
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.entero(ParametroService.DIAS_ALMACENAJE)).thenReturn(30);
        when(parametros.entero(ParametroService.DIAS_ALERTA_ALMACENAJE)).thenReturn(3);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new PlazoAlmacenajeService(parametros, reloj);
    }

    /** Caso de QA de HU-05, CA3. */
    @Test
    void cambiarLaEta_recalculaLaFechaLimite() {
        assertThat(servicio.fechaLimite(LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(servicio.fechaLimite(LocalDate.of(2026, 10, 25))).isEqualTo(LocalDate.of(2026, 11, 24));
    }

    @Test
    void quedan3Dias_activaLaAlerta() {
        Plazo plazo = servicio.plazo(LocalDate.of(2026, 11, 19));

        assertThat(plazo.diasRestantes()).isEqualTo(3);
        assertThat(plazo.alerta()).isTrue();
    }

    @Test
    void quedan8Dias_sinAlerta() {
        Plazo plazo = servicio.plazo(LocalDate.of(2026, 11, 24));

        assertThat(plazo.diasRestantes()).isEqualTo(8);
        assertThat(plazo.alerta()).isFalse();
    }

    @Test
    void plazoVencido_diasNegativosConAlerta() {
        Plazo plazo = PlazoAlmacenajeService.plazo(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16), 3);

        assertThat(plazo.diasRestantes()).isEqualTo(-6);
        assertThat(plazo.alerta()).isTrue();
    }
}
