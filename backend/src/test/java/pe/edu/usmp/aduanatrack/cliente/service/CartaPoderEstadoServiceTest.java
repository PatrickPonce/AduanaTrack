package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;

class CartaPoderEstadoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 7);
    private static final int DIAS_ALERTA = 15;

    @Test
    void sinCarta() {
        assertThat(CartaPoderEstadoService.calcular(null, HOY, DIAS_ALERTA)).isEqualTo(EstadoCartaPoder.SIN_CARTA);
    }

    @Test
    void venceEn10Dias_estaPorVencer() {
        assertThat(CartaPoderEstadoService.calcular(HOY.plusDays(10), HOY, DIAS_ALERTA))
                .isEqualTo(EstadoCartaPoder.POR_VENCER);
    }

    @Test
    void venceEn15Dias_estaPorVencerYEn16EstaVigente() {
        assertThat(CartaPoderEstadoService.calcular(HOY.plusDays(15), HOY, DIAS_ALERTA))
                .isEqualTo(EstadoCartaPoder.POR_VENCER);
        assertThat(CartaPoderEstadoService.calcular(HOY.plusDays(16), HOY, DIAS_ALERTA))
                .isEqualTo(EstadoCartaPoder.VIGENTE);
    }

    @Test
    void venceHoy_aunNoEstaVencida() {
        assertThat(CartaPoderEstadoService.calcular(HOY, HOY, DIAS_ALERTA)).isEqualTo(EstadoCartaPoder.POR_VENCER);
    }

    @Test
    void vencioAyer_estaVencida() {
        assertThat(CartaPoderEstadoService.calcular(HOY.minusDays(1), HOY, DIAS_ALERTA))
                .isEqualTo(EstadoCartaPoder.VENCIDA);
    }
}
