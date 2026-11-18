package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResumen;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;

/** La búsqueda de clientes ignora mayúsculas y tildes («pacifico» encuentra «Pacífico Trade»). */
class ClienteServiceBusquedaTest {

    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        ClienteRepository repositorio = mock(ClienteRepository.class);
        CartaPoderEstadoService cartas = mock(CartaPoderEstadoService.class);
        when(cartas.estados(anyCollection())).thenReturn(Map.of());
        when(repositorio.findByActivoTrueOrderByRazonSocialAsc()).thenReturn(List.of(
                cliente(1L, "20512345671", "Importadora Andina S.A.C."),
                cliente(2L, "20605544330", "Pacífico Trade E.I.R.L."),
                cliente(3L, "20123456786", "Muñoz e Hijos SAC")));
        servicio = new ClienteService(repositorio, mock(ClienteHistorialRepository.class),
                mock(UsuarioRepository.class), cartas, Clock.systemUTC());
    }

    @Test
    void quitaTildesYPasaAMinusculas() {
        assertThat(ClienteService.sinTildes("Pacífico")).isEqualTo("pacifico");
        assertThat(ClienteService.sinTildes("PERÚ Andina")).isEqualTo("peru andina");
        assertThat(ClienteService.sinTildes("Muñoz")).isEqualTo("munoz");
    }

    @Test
    void sinTildesEncuentraNombreConTilde() {
        assertThat(servicio.buscar("pacifico", 0, 20).contenido()).extracting(ClienteResumen::id).containsExactly(2L);
        assertThat(servicio.buscar("PACÍFICO", 0, 20).contenido()).extracting(ClienteResumen::id).containsExactly(2L);
        assertThat(servicio.buscar("munoz", 0, 20).contenido()).extracting(ClienteResumen::id).containsExactly(3L);
    }

    @Test
    void buscaPorInicioDeRuc() {
        assertThat(servicio.buscar("20512", 0, 20).contenido()).extracting(ClienteResumen::id).containsExactly(1L);
        // El RUC se busca por prefijo, no por coincidencia en cualquier parte.
        assertThat(servicio.buscar("45671", 0, 20).contenido()).isEmpty();
    }

    @Test
    void sinTextoDevuelveTodosYPagina() {
        var primera = servicio.buscar("", 0, 2);
        assertThat(primera.contenido()).hasSize(2);
        assertThat(primera.totalElementos()).isEqualTo(3);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(servicio.buscar(null, 1, 2).contenido()).hasSize(1);
        assertThat(servicio.buscar("", 9, 2).contenido()).isEmpty();
    }

    private static Cliente cliente(Long id, String ruc, String razonSocial) {
        Cliente c = new Cliente(ruc, LocalDateTime.now());
        c.setRazonSocial(razonSocial);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
