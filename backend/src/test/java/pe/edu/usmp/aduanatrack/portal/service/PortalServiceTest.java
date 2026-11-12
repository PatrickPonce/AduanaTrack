package pe.edu.usmp.aduanatrack.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.acceso.service.AccesoClienteService;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;
import pe.edu.usmp.aduanatrack.portal.repository.PortalRepository;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

class PortalServiceTest {

    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(50L, "jperez", "Jorge Pérez", "CLIENTE");

    private AccesoClienteService acceso;
    private ClienteRepository clientes;
    private PortalRepository portal;
    private PortalService servicio;

    @BeforeEach
    void preparar() {
        acceso = mock(AccesoClienteService.class);
        clientes = mock(ClienteRepository.class);
        portal = mock(PortalRepository.class);
        servicio = new PortalService(acceso, clientes, portal);
        when(acceso.clienteIdDe(CLIENTE)).thenReturn(1L);
        when(portal.despachosDe(1L, EstadoDespacho.BORRADOR)).thenReturn(List.of(
                fila(318L, "DESP-2026-0318", "MSCU-BL-55120", LocalDate.of(2026, 10, 21),
                        EstadoDespacho.INSPECCION_PROGRAMADA),
                fila(301L, "DESP-2026-0301", "HLCU-BL-88213", LocalDate.of(2026, 10, 14),
                        EstadoDespacho.LEVANTE_OTORGADO),
                fila(355L, "DESP-2026-0355", null, null, EstadoDespacho.DOCUMENTACION_COMPLETA)));
    }

    @Test
    void miEmpresa_devuelveRucYRazonSocial() {
        Cliente empresa = new Cliente("20512345671", LocalDateTime.now());
        empresa.setRazonSocial("Importadora Andina S.A.C.");
        ReflectionTestUtils.setField(empresa, "id", 1L);
        when(clientes.findById(1L)).thenReturn(Optional.of(empresa));

        var respuesta = servicio.miEmpresa(CLIENTE);

        assertThat(respuesta.clienteId()).isEqualTo(1L);
        assertThat(respuesta.ruc()).isEqualTo("20512345671");
        assertThat(respuesta.razonSocial()).isEqualTo("Importadora Andina S.A.C.");
    }

    @Test
    void miEmpresa_sinEmpresaAsignada_vieneVacia() {
        when(acceso.clienteIdDe(CLIENTE)).thenReturn(null);

        var respuesta = servicio.miEmpresa(CLIENTE);

        assertThat(respuesta.clienteId()).isNull();
        assertThat(respuesta.razonSocial()).isNull();
    }

    @Test
    void despachos_sinFiltros_devuelveTodosLosDeLaEmpresa() {
        assertThat(servicio.despachos(CLIENTE, null, null, null, null)).hasSize(3);
    }

    @Test
    void despachos_sinEmpresa_listaVaciaSinConsultarDespachos() {
        when(acceso.clienteIdDe(CLIENTE)).thenReturn(null);

        assertThat(servicio.despachos(CLIENTE, null, null, null, null)).isEmpty();
        verifyNoInteractions(portal);
    }

    @Test
    void despachos_buscarPorNumero_ignoraMayusculasYBuscaParcial() {
        assertThat(servicio.despachos(CLIENTE, " desp-2026-0301 ", null, null, null))
                .extracting(PortalDespachoResponse::numero).containsExactly("DESP-2026-0301");
        assertThat(servicio.despachos(CLIENTE, "DESP-2026-0342", null, null, null)).isEmpty();
    }

    @Test
    void despachos_filtraPorEstado() {
        assertThat(servicio.despachos(CLIENTE, null, "levante_otorgado", null, null))
                .extracting(PortalDespachoResponse::numero).containsExactly("DESP-2026-0301");
    }

    @Test
    void despachos_filtraPorRangoDeEta_yExcluyeLosSinEta() {
        assertThat(servicio.despachos(CLIENTE, null, null, "2026-10-01", "2026-10-31"))
                .extracting(PortalDespachoResponse::numero).containsExactly("DESP-2026-0318", "DESP-2026-0301");
        assertThat(servicio.despachos(CLIENTE, null, null, "2026-10-15", null))
                .extracting(PortalDespachoResponse::numero).containsExactly("DESP-2026-0318");
    }

    @Test
    void despachos_filtrosInvalidos_responde400ConTodosLosCampos() {
        assertThatThrownBy(() -> servicio.despachos(CLIENTE, null, "NO_EXISTE", "31/10/2026", "x"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos())
                        .containsKeys("estado", "etaDesde", "etaHasta"));
        assertThatThrownBy(() -> servicio.despachos(CLIENTE, null, "BORRADOR", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("estado"));
    }

    @Test
    void despachos_rangoInvertido_responde400() {
        assertThatThrownBy(() -> servicio.despachos(CLIENTE, null, null, "2026-11-30", "2026-10-01"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("etaHasta"));
    }

    private static PortalDespachoResponse fila(Long id, String numero, String bl, LocalDate eta,
            EstadoDespacho estado) {
        return new PortalDespachoResponse(id, numero, bl, eta, estado, null, LocalDateTime.of(2026, 11, 5, 18, 40));
    }
}
