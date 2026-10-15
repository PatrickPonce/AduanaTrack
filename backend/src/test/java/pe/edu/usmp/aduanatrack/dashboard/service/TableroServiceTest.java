package pe.edu.usmp.aduanatrack.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroFila;
import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroResponse;
import pe.edu.usmp.aduanatrack.dashboard.repository.TableroRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService;

class TableroServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 9);

    private TableroRepository repositorio;
    private CartaPoderEstadoService cartaPoder;
    private TableroService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(TableroRepository.class);
        cartaPoder = mock(CartaPoderEstadoService.class);
        PlazoAlmacenajeService plazo = mock(PlazoAlmacenajeService.class);
        when(plazo.plazo(any())).thenAnswer(i -> PlazoAlmacenajeService.plazo(i.getArgument(0), HOY, 3));
        servicio = new TableroService(repositorio, plazo, cartaPoder);
    }

    @Test
    void calculaDiasDeAlmacenajeYAlertas() {
        when(repositorio.despachos(null, null, EstadoDespacho.BORRADOR)).thenReturn(List.of(
                fila(1L, 1L, LocalDate.of(2026, 10, 15)),
                fila(2L, 2L, LocalDate.of(2026, 9, 10)),
                fila(3L, 3L, null)));
        when(cartaPoder.estados(anySet())).thenReturn(Map.of(
                1L, EstadoCartaPoder.VIGENTE, 2L, EstadoCartaPoder.VIGENTE, 3L, EstadoCartaPoder.VENCIDA));

        List<DespachoTableroResponse> r = servicio.despachos(null, null);

        // ETA 15/10 → límite 14/11: quedan 36 días, sin alerta.
        assertThat(r.get(0).diasAlmacenaje()).isEqualTo(36);
        assertThat(r.get(0).alertaCritica()).isFalse();
        // ETA 10/09 → límite 10/10: queda 1 día, alerta de almacenaje.
        assertThat(r.get(1).diasAlmacenaje()).isEqualTo(1);
        assertThat(r.get(1).alertaAlmacenaje()).isTrue();
        assertThat(r.get(1).alertaCritica()).isTrue();
        // Sin embarque pero con carta poder vencida: alerta crítica.
        assertThat(r.get(2).diasAlmacenaje()).isNull();
        assertThat(r.get(2).alertaCritica()).isTrue();
    }

    @Test
    void pasaLosFiltrosAlRepositorioYConservaSuOrden() {
        when(repositorio.despachos(118L, 1L, EstadoDespacho.BORRADOR)).thenReturn(List.of(
                fila(7L, 1L, LocalDate.of(2026, 10, 15)), fila(5L, 1L, LocalDate.of(2026, 10, 20))));
        when(cartaPoder.estados(anySet())).thenReturn(Map.of(1L, EstadoCartaPoder.VIGENTE));

        assertThat(servicio.despachos(118L, 1L)).extracting(DespachoTableroResponse::id).containsExactly(7L, 5L);
    }

    private static DespachoTableroFila fila(Long id, Long clienteId, LocalDate eta) {
        return new DespachoTableroFila(id, "DESP-2026-000" + id, clienteId, "Cliente " + clienteId, "118",
                "Marítima del Callao", eta == null ? null : "HBL-" + id, null, 0L, eta,
                eta == null ? null : eta.plusDays(30),
                EstadoDespacho.APERTURA, null);
    }
}
