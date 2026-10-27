package pe.edu.usmp.aduanatrack.liquidacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ItemCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo.Tasas;

/**
 * Caso calculado a mano (Guía Sprint 2, HU-08): una partida al 6 %, FOB 10 000,00 + flete 800,00 +
 * seguro 150,00 = CIF 10 950,00 → Ad Valorem 657,00 → IGV 1 857,12 → IPM 232,14 → percepción
 * 479,37 → total 3 225,63 USD.
 */
class CalculoTributosServiceTest {

    private static final Tasas TASAS_REFERENCIA = new Tasas(bd("16"), bd("2"), bd("3.5"));

    private ParametroService parametros;
    private CalculoTributosService servicio;

    @BeforeEach
    void preparar() {
        parametros = mock(ParametroService.class);
        when(parametros.decimal(ParametroService.TASA_IGV)).thenReturn(bd("16"));
        when(parametros.decimal(ParametroService.TASA_IPM)).thenReturn(bd("2"));
        when(parametros.decimal(ParametroService.TASA_PERCEPCION)).thenReturn(bd("3.5"));
        servicio = new CalculoTributosService(parametros);
    }

    @Test
    void casoCalculadoAMano_cifEs10950_criterio1() {
        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("800.00"), bd("150.00"));

        assertThat(r.fob()).isEqualByComparingTo("10000.00");
        assertThat(r.flete()).isEqualByComparingTo("800.00");
        assertThat(r.seguro()).isEqualByComparingTo("150.00");
        assertThat(r.cif()).isEqualByComparingTo("10950.00");
    }

    @Test
    void casoCalculadoAMano_tributosYTotal_criterio2() {
        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("800.00"), bd("150.00"));

        assertThat(r.adValorem()).isEqualByComparingTo("657.00");
        assertThat(r.igv()).isEqualByComparingTo("1857.12");
        assertThat(r.ipm()).isEqualByComparingTo("232.14");
        assertThat(r.percepcion()).isEqualByComparingTo("479.37");
        assertThat(r.total()).isEqualByComparingTo("3225.63");
    }

    @Test
    void lasTasasSalenDeParametroSistemaYSeDevuelven() {
        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("800.00"), bd("150.00"));

        assertThat(r.tasas()).isEqualTo(TASAS_REFERENCIA);
    }

    @Test
    void cambiarUnaTasaCambiaElResultado_criterio2() {
        when(parametros.decimal(ParametroService.TASA_IGV)).thenReturn(bd("18"));

        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("800.00"), bd("150.00"));

        // (10 950,00 + 657,00) × 18 % = 2 089,26
        assertThat(r.igv()).isEqualByComparingTo("2089.26");
        assertThat(r.ipm()).isEqualByComparingTo("232.14");
        // (11 607,00 + 2 089,26 + 232,14) × 3,5 % = 487,494 → 487,49
        assertThat(r.percepcion()).isEqualByComparingTo("487.49");
        assertThat(r.total()).isEqualByComparingTo("3465.89");
        assertThat(r.tasas().igv()).isEqualByComparingTo("18");
    }

    @Test
    void cambiarLaTasaAdValoremDeLaPartidaCambiaElResultado() {
        ResultadoCalculo r = servicio.calcular(List.of(new ItemCalculo(bd("10000.00"), bd("0.00"))), bd("800.00"),
                bd("150.00"), TASAS_REFERENCIA);

        assertThat(r.adValorem()).isEqualByComparingTo("0.00");
        // 10 950,00 × 16 % = 1 752,00; × 2 % = 219,00; (10 950 + 1 752 + 219) × 3,5 % = 452,235 → 452,24
        assertThat(r.igv()).isEqualByComparingTo("1752.00");
        assertThat(r.ipm()).isEqualByComparingTo("219.00");
        assertThat(r.percepcion()).isEqualByComparingTo("452.24");
        assertThat(r.total()).isEqualByComparingTo("2423.24");
    }

    @Test
    void fleteYSeguroSeRepartenSegunElFobDeCadaItem() {
        // Ítem 1: 4 000 al 6 % (40 % del FOB); ítem 2: 6 000 al 11 % (60 % del FOB).
        List<ItemCalculo> items = List.of(
                new ItemCalculo(bd("4000.00"), bd("6.00")),
                new ItemCalculo(bd("6000.00"), bd("11.00")));

        ResultadoCalculo r = servicio.calcular(items, bd("800.00"), bd("150.00"), TASAS_REFERENCIA);

        // Ítem 1: CIF 4 000 + 320 + 60 = 4 380 → 262,80. Ítem 2: CIF 6 000 + 480 + 90 = 6 570 → 722,70.
        assertThat(r.cif()).isEqualByComparingTo("10950.00");
        assertThat(r.adValorem()).isEqualByComparingTo("985.50");
    }

    @Test
    void laDiferenciaDeRedondeoDelRepartoLaAbsorbeElUltimoItem() {
        // 100,00 de flete entre tres ítems iguales: 33,33 + 33,33 + 33,34.
        List<ItemCalculo> items = List.of(
                new ItemCalculo(bd("1000.00"), bd("10.00")),
                new ItemCalculo(bd("1000.00"), bd("10.00")),
                new ItemCalculo(bd("1000.00"), bd("10.00")));

        ResultadoCalculo r = servicio.calcular(items, bd("100.00"), bd("0.00"), TASAS_REFERENCIA);

        assertThat(r.cif()).isEqualByComparingTo("3100.00");
        // CIF por ítem 1 033,33 + 1 033,33 + 1 033,34 → 103,33 + 103,33 + 103,33 (HALF_UP por ítem)
        assertThat(r.adValorem()).isEqualByComparingTo("309.99");
    }

    @Test
    void redondeaHalfUpEnCadaConcepto() {
        // 0,05 × 10 % = 0,005 → 0,01 (HALF_UP); con HALF_EVEN daría 0,00.
        ResultadoCalculo r = servicio.calcular(List.of(new ItemCalculo(bd("0.05"), bd("10.00"))), bd("0.00"),
                bd("0.00"), new Tasas(bd("0"), bd("0"), bd("0")));

        assertThat(r.adValorem()).isEqualByComparingTo("0.01");
    }

    @Test
    void todosLosMontosTienenDosDecimales() {
        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("800"), bd("150"));

        for (BigDecimal monto : List.of(r.fob(), r.flete(), r.seguro(), r.cif(), r.adValorem(), r.igv(), r.ipm(),
                r.percepcion(), r.total())) {
            assertThat(monto.scale()).isEqualTo(2);
        }
    }

    @Test
    void sinFleteNiSeguroElCifEsElFob() {
        ResultadoCalculo r = servicio.calcular(unaPartidaAl6(), bd("0.00"), bd("0.00"));

        assertThat(r.cif()).isEqualByComparingTo("10000.00");
        assertThat(r.adValorem()).isEqualByComparingTo("600.00");
    }

    @Test
    void fleteNegativo_responde400ConElCampo() {
        assertThatThrownBy(() -> servicio.calcular(unaPartidaAl6(), bd("-1.00"), bd("0.00")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION);
                    assertThat(e.getCampos()).containsKey("flete");
                });
    }

    @Test
    void seguroNegativo_responde400ConElCampo() {
        assertThatThrownBy(() -> servicio.calcular(unaPartidaAl6(), bd("0.00"), bd("-0.01")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("seguro"));
    }

    @Test
    void sinItems_noSePuedeCalcular() {
        assertThatThrownBy(() -> servicio.calcular(List.of(), bd("0.00"), bd("0.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemSinFobPositivo_noSePuedeCalcular() {
        assertThatThrownBy(() -> servicio.calcular(List.of(new ItemCalculo(bd("0.00"), bd("6.00"))), bd("0.00"),
                bd("0.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<ItemCalculo> unaPartidaAl6() {
        return List.of(new ItemCalculo(bd("10000.00"), bd("6.00")));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
