package pe.edu.usmp.aduanatrack.clasificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.clasificacion.dto.PartidaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;
import pe.edu.usmp.aduanatrack.clasificacion.repository.PartidaReferencialRepository;

class PartidaServiceTest {

    private PartidaReferencialRepository repositorio;
    private PartidaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(PartidaReferencialRepository.class);
        servicio = new PartidaService(repositorio);
    }

    @Test
    void buscar_mayusculasYMinusculasUsanElMismoPatron_criterio1() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of(rodamientos()));

        List<PartidaResponse> minusculas = servicio.buscar("rodamiento");
        List<PartidaResponse> mayusculas = servicio.buscar("RODAMIENTO");

        assertThat(minusculas).hasSize(1);
        assertThat(mayusculas).isEqualTo(minusculas);
        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        verify(repositorio, org.mockito.Mockito.times(2)).buscar(patron.capture(), any(), any());
        assertThat(patron.getAllValues()).containsOnly("%rodamiento%");
    }

    @Test
    void buscar_ignoraTildes() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of());

        servicio.buscar("Cónicos");

        verify(repositorio).buscar(eq("%conicos%"), eq("#"), any(Pageable.class));
    }

    @Test
    void buscar_variasPalabrasSeBuscanEnOrden() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of());

        servicio.buscar("  Rodamientos   de bolas ");

        verify(repositorio).buscar(eq("%rodamientos%de%bolas%"), eq("#"), any(Pageable.class));
    }

    @Test
    void buscar_conDigitosBuscaTambienPorElInicioDelCodigo() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of());

        servicio.buscar("8482.10");

        verify(repositorio).buscar(eq("%8482%10%"), eq("848210%"), any(Pageable.class));
    }

    @Test
    void buscar_limitaA20Resultados() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of());

        servicio.buscar("motor");

        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscar(any(), any(), limite.capture());
        assertThat(limite.getValue().getPageNumber()).isZero();
        assertThat(limite.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void buscar_conTextoVacioONuloNoConsultaLaBase() {
        assertThat(servicio.buscar(null)).isEmpty();
        assertThat(servicio.buscar("   ")).isEmpty();
        assertThat(servicio.buscar("%_")).isEmpty();

        verify(repositorio, never()).buscar(any(), any(), any());
    }

    @Test
    void buscar_losComodinesDeLikeNoLlegaranALaConsulta() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of());

        servicio.buscar("50%_motor");

        verify(repositorio).buscar(eq("%50%motor%"), eq("#"), any(Pageable.class));
    }

    @Test
    void respuesta_incluyeCodigoFormateadoYTasa() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(List.of(rodamientos()));

        PartidaResponse partida = servicio.buscar("rodam").get(0);

        assertThat(partida.id()).isEqualTo(1L);
        assertThat(partida.codigo()).isEqualTo("8482100000");
        assertThat(partida.codigoFormateado()).isEqualTo("8482.10.00.00");
        assertThat(partida.descripcion()).isEqualTo("Rodamientos de bolas");
        assertThat(partida.tasaAdValorem()).isEqualByComparingTo("6.00");
    }

    private static PartidaReferencial rodamientos() {
        PartidaReferencial partida = new PartidaReferencial("8482100000", "Rodamientos de bolas",
                new BigDecimal("6.00"), "rodamientos de bolas");
        ReflectionTestUtils.setField(partida, "id", 1L);
        return partida;
    }
}
