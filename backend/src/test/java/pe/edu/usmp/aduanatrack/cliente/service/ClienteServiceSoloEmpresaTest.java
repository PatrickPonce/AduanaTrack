package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;

/** HU-16: GET /clientes del perfil CLIENTE devuelve solo su empresa. */
class ClienteServiceSoloEmpresaTest {

    private ClienteRepository repositorio;
    private CartaPoderEstadoService cartas;
    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ClienteRepository.class);
        cartas = mock(CartaPoderEstadoService.class);
        servicio = new ClienteService(repositorio, mock(ClienteHistorialRepository.class),
                mock(UsuarioRepository.class), cartas, Clock.systemUTC());
    }

    @Test
    void soloEmpresa_devuelveUnaSolaFilaConSuEstadoDeCartaPoder() {
        Cliente empresa = cliente(1L, true);
        when(repositorio.findById(1L)).thenReturn(Optional.of(empresa));
        when(cartas.estados(List.of(1L))).thenReturn(Map.of(1L, EstadoCartaPoder.VIGENTE));

        var pagina = servicio.soloEmpresa(1L);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.contenido().get(0).id()).isEqualTo(1L);
        assertThat(pagina.contenido().get(0).estadoCartaPoder()).isEqualTo(EstadoCartaPoder.VIGENTE);
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    void soloEmpresa_sinEmpresaAsignada_devuelveListaVaciaSinConsultar() {
        when(cartas.estados(anyCollection())).thenReturn(Map.of());

        var pagina = servicio.soloEmpresa(null);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        verifyNoInteractions(repositorio);
    }

    @Test
    void soloEmpresa_empresaInactiva_noSeMuestra() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(cliente(1L, false)));
        when(cartas.estados(anyCollection())).thenReturn(Map.of());

        assertThat(servicio.soloEmpresa(1L).contenido()).isEmpty();
        verify(repositorio).findById(1L);
    }

    private static Cliente cliente(Long id, boolean activo) {
        Cliente c = new Cliente("20787789324", LocalDateTime.now());
        c.setRazonSocial("Importadora Andina SAC");
        c.setActivo(activo);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
