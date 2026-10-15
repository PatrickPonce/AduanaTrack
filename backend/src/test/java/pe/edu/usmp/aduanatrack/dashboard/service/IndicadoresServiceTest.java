package pe.edu.usmp.aduanatrack.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse;
import pe.edu.usmp.aduanatrack.dashboard.repository.TableroRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService;

class IndicadoresServiceTest {

    private static final LocalDate LIMITE_ALMACENAJE = LocalDate.of(2026, 10, 12);
    private static final LocalDate VENCIMIENTO_CRITICO = LocalDate.of(2026, 10, 24);

    private TableroRepository repositorio;
    private IndicadoresService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(TableroRepository.class);
        PlazoAlmacenajeService plazo = mock(PlazoAlmacenajeService.class);
        CartaPoderEstadoService cartaPoder = mock(CartaPoderEstadoService.class);
        when(plazo.fechaLimiteEnAlerta()).thenReturn(LIMITE_ALMACENAJE);
        when(cartaPoder.vencimientoCritico()).thenReturn(VENCIMIENTO_CRITICO);
        servicio = new IndicadoresService(repositorio, plazo, cartaPoder);
    }

    @Test
    void cuentaLosNoBorradoresConLosFiltrosYLasFechasDeCorteDeLasAlertas() {
        IndicadoresResponse esperado = new IndicadoresResponse(8, 2, 3, 1);
        when(repositorio.indicadores(118L, 7L, EstadoDespacho.BORRADOR, LIMITE_ALMACENAJE, VENCIMIENTO_CRITICO))
                .thenReturn(esperado);

        assertThat(servicio.indicadores(118L, 7L)).isEqualTo(esperado);
    }

    @Test
    void sinFiltros_pasaNulosAlRepositorio() {
        IndicadoresResponse esperado = new IndicadoresResponse(0, 0, 0, 0);
        when(repositorio.indicadores(null, null, EstadoDespacho.BORRADOR, LIMITE_ALMACENAJE, VENCIMIENTO_CRITICO))
                .thenReturn(esperado);

        assertThat(servicio.indicadores(null, null)).isEqualTo(esperado);
    }
}
