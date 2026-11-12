package pe.edu.usmp.aduanatrack.acceso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository.DespachoAcceso;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

class AccesoClienteServiceTest {

    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(50L, "jperez", "Jorge Pérez", "CLIENTE");
    private static final UsuarioAutenticado AGENTE = new UsuarioAutenticado(2L, "agente", "Agente", "AGENTE");
    private static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(1L, "admin", "Admin", "ADMIN");

    private UsuarioRepository usuarios;
    private DespachoRepository despachos;
    private AccesoDenegadoService denegados;
    private AccesoClienteService servicio;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        despachos = mock(DespachoRepository.class);
        denegados = mock(AccesoDenegadoService.class);
        servicio = new AccesoClienteService(usuarios, despachos, denegados);
    }

    @Test
    void despachoDeSuEmpresa_pasaYNoRegistraNada() {
        empresaDelCliente(1L);
        despacho(10L, 1L, EstadoDespacho.APERTURA);

        assertThatCode(() -> servicio.exigirDespacho(CLIENTE, 10L)).doesNotThrowAnyException();
        verifyNoInteractions(denegados);
    }

    @Test
    void despachoDeOtraEmpresa_responde404ConMensajeGenericoYRegistraElIntento() {
        empresaDelCliente(1L);
        despacho(20L, 2L, EstadoDespacho.CANAL_ASIGNADO);

        assertThatThrownBy(() -> servicio.exigirDespacho(CLIENTE, 20L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.NO_ENCONTRADO);
                    assertThat(e.getMessage()).isEqualTo("Despacho no encontrado");
                });
        verify(denegados).registrar(50L, 20L, 2L, MotivoAccesoDenegado.OTRA_EMPRESA);
    }

    @Test
    void despachoInexistente_dalaMismaRespuestaQueUnoAjenoYNoRegistra() {
        empresaDelCliente(1L);
        when(despachos.accesoDe(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.exigirDespacho(CLIENTE, 999L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("Despacho no encontrado");
                });
        verifyNoInteractions(denegados);
    }

    @Test
    void clienteSinEmpresa_noVeNingunDespachoYQuedaRegistrado() {
        empresaDelCliente(null);
        despacho(10L, 1L, EstadoDespacho.APERTURA);

        assertThatThrownBy(() -> servicio.exigirDespacho(CLIENTE, 10L)).isInstanceOf(ApiException.class)
                .hasMessage("Despacho no encontrado");
        verify(denegados).registrar(50L, 10L, 1L, MotivoAccesoDenegado.SIN_EMPRESA);
    }

    @Test
    void borradorDeSuPropiaEmpresa_noSeMuestraAlCliente() {
        empresaDelCliente(1L);
        despacho(30L, 1L, EstadoDespacho.BORRADOR);

        assertThatThrownBy(() -> servicio.exigirDespacho(CLIENTE, 30L)).isInstanceOf(ApiException.class)
                .hasMessage("Despacho no encontrado");
        verify(denegados).registrar(50L, 30L, 1L, MotivoAccesoDenegado.BORRADOR);
    }

    @Test
    void agenteYAdmin_noTienenRestriccionNiConsultanLaBase() {
        assertThatCode(() -> servicio.exigirDespacho(AGENTE, 20L)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.exigirDespacho(ADMIN, 20L)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.exigirCliente(AGENTE, 2L)).doesNotThrowAnyException();
        verifyNoInteractions(despachos, usuarios, denegados);
    }

    @Test
    void filtroCliente_elClienteSiempreRecibeSuEmpresaAunqueEscojaOtra() {
        empresaDelCliente(1L);

        assertThat(servicio.filtroCliente(CLIENTE, 2L)).isEqualTo(1L);
        assertThat(servicio.filtroCliente(CLIENTE, null)).isEqualTo(1L);
    }

    @Test
    void filtroCliente_sinEmpresaNoCoincideConNingunaFila() {
        empresaDelCliente(null);

        assertThat(servicio.filtroCliente(CLIENTE, null)).isEqualTo(AccesoClienteService.SIN_RESULTADOS);
        assertThat(servicio.filtroCliente(CLIENTE, 2L)).isEqualTo(AccesoClienteService.SIN_RESULTADOS);
    }

    @Test
    void filtroCliente_agenteYAdminConservanElFiltroQuePidieron() {
        assertThat(servicio.filtroCliente(AGENTE, 7L)).isEqualTo(7L);
        assertThat(servicio.filtroCliente(ADMIN, null)).isNull();
        verify(usuarios, never()).clienteIdDe(any());
    }

    @Test
    void exigirCliente_soloSuPropiaEmpresa() {
        empresaDelCliente(1L);

        assertThatCode(() -> servicio.exigirCliente(CLIENTE, 1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> servicio.exigirCliente(CLIENTE, 2L)).isInstanceOf(ApiException.class)
                .hasMessage("Cliente no encontrado");
    }

    private void empresaDelCliente(Long clienteId) {
        when(usuarios.clienteIdDe(50L)).thenReturn(Optional.ofNullable(clienteId));
    }

    private void despacho(Long id, Long clienteId, EstadoDespacho estado) {
        when(despachos.accesoDe(id)).thenReturn(Optional.of(new DespachoAcceso() {
            @Override
            public Long getClienteId() {
                return clienteId;
            }

            @Override
            public EstadoDespacho getEstado() {
                return estado;
            }
        }));
    }
}
