package pe.edu.usmp.aduanatrack.acceso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse;
import pe.edu.usmp.aduanatrack.acceso.repository.AccesoDenegadoRepository;
import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;

class UsuariosClienteServiceTest {

    private static final String RUC_ANDINA = "20787789324";
    private static final String RUC_INVALIDO = "20787789325";
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 5, 15, 15);

    private UsuarioRepository usuarios;
    private ClienteRepository clientes;
    private ClienteHistorialRepository historial;
    private AccesoDenegadoRepository denegados;
    private UsuariosClienteService servicio;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        clientes = mock(ClienteRepository.class);
        historial = mock(ClienteHistorialRepository.class);
        denegados = mock(AccesoDenegadoRepository.class);
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new UsuariosClienteService(usuarios, clientes, historial, denegados, reloj);
    }

    @Test
    void listar_muestraLaEmpresaDeCadaUsuarioYMarcaLosSinAsignar() {
        Usuario asignado = usuario(5L, "jperez", "CLIENTE", 1L);
        Usuario sinEmpresa = usuario(6L, "cliente", "CLIENTE", null);
        when(usuarios.listarClientes()).thenReturn(List.of(asignado, sinEmpresa));
        when(clientes.findAllById(List.of(1L))).thenReturn(List.of(empresa(1L, RUC_ANDINA, "Importadora Andina SAC")));

        var filas = servicio.listar();

        assertThat(filas).hasSize(2);
        assertThat(filas.get(0).estado()).isEqualTo("ASIGNADO");
        assertThat(filas.get(0).razonSocial()).isEqualTo("Importadora Andina SAC");
        assertThat(filas.get(0).ruc()).isEqualTo(RUC_ANDINA);
        assertThat(filas.get(1).estado()).isEqualTo("SIN_ASIGNAR");
        assertThat(filas.get(1).clienteId()).isNull();
        assertThat(filas.get(1).razonSocial()).isNull();
    }

    @Test
    void empresasAsignables_soloActivasConRucValido() {
        Cliente inactiva = empresa(3L, RUC_ANDINA, "Inactiva SAC");
        inactiva.setActivo(false);
        when(clientes.findAll()).thenReturn(List.of(
                empresa(1L, RUC_ANDINA, "Zeta SAC"), empresa(2L, RUC_INVALIDO, "RUC malo SAC"), inactiva,
                empresa(4L, "20605544330", "Alfa SAC")));

        var lista = servicio.empresasAsignables();

        assertThat(lista).extracting("razonSocial").containsExactly("Alfa SAC", "Zeta SAC");
    }

    @Test
    void asignar_primeraVez_guardaLaEmpresaYAuditaEnElHistorialDeLaEmpresa() {
        Usuario usuario = usuario(6L, "cliente", "CLIENTE", null);
        when(usuarios.findById(6L)).thenReturn(Optional.of(usuario));
        when(usuarios.getReferenceById(1L)).thenReturn(usuario(1L, "admin", "ADMIN", null));
        when(clientes.findById(1L)).thenReturn(Optional.of(empresa(1L, RUC_ANDINA, "Importadora Andina SAC")));

        var respuesta = servicio.asignar(6L, 1L, 1L);

        assertThat(usuario.getClienteId()).isEqualTo(1L);
        assertThat(usuario.getClienteAsignadoEn()).isEqualTo(AHORA);
        assertThat(respuesta.estado()).isEqualTo("ASIGNADO");
        ArgumentCaptor<ClienteHistorial> fila = ArgumentCaptor.forClass(ClienteHistorial.class);
        verify(historial).save(fila.capture());
        assertThat(fila.getValue().getClienteId()).isEqualTo(1L);
        assertThat(fila.getValue().getCampo()).isEqualTo("usuarioAsignado");
        assertThat(fila.getValue().getValorAnterior()).isNull();
        assertThat(fila.getValue().getValorNuevo()).isEqualTo("cliente");
        assertThat(fila.getValue().getFecha()).isEqualTo(AHORA);
    }

    @Test
    void asignar_cambioDeEmpresa_dejaUnaFilaEnCadaEmpresa() {
        Usuario usuario = usuario(5L, "jperez", "CLIENTE", 1L);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarios.getReferenceById(1L)).thenReturn(usuario(1L, "admin", "ADMIN", null));
        when(clientes.findById(2L)).thenReturn(Optional.of(empresa(2L, "20605544330", "Pacífico Trade EIRL")));

        servicio.asignar(5L, 2L, 1L);

        assertThat(usuario.getClienteId()).isEqualTo(2L);
        ArgumentCaptor<ClienteHistorial> filas = ArgumentCaptor.forClass(ClienteHistorial.class);
        verify(historial, org.mockito.Mockito.times(2)).save(filas.capture());
        assertThat(filas.getAllValues()).extracting(ClienteHistorial::getClienteId).containsExactly(1L, 2L);
        assertThat(filas.getAllValues().get(0).getValorAnterior()).isEqualTo("jperez");
        assertThat(filas.getAllValues().get(0).getValorNuevo()).isNull();
        assertThat(filas.getAllValues().get(1).getValorNuevo()).isEqualTo("jperez");
    }

    @Test
    void asignar_laMismaEmpresa_noCambiaNadaNiAudita() {
        Usuario usuario = usuario(5L, "jperez", "CLIENTE", 1L);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(clientes.findById(1L)).thenReturn(Optional.of(empresa(1L, RUC_ANDINA, "Importadora Andina SAC")));

        servicio.asignar(5L, 1L, 1L);

        verify(historial, never()).save(any());
        assertThat(usuario.getClienteAsignadoEn()).isNull();
    }

    @Test
    void asignar_empresaConRucInvalido_responde400EnClienteId() {
        when(usuarios.findById(6L)).thenReturn(Optional.of(usuario(6L, "cliente", "CLIENTE", null)));
        when(clientes.findById(2L)).thenReturn(Optional.of(empresa(2L, RUC_INVALIDO, "RUC malo SAC")));

        assertThatThrownBy(() -> servicio.asignar(6L, 2L, 1L)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION);
            assertThat(e.getCampos()).containsKey("clienteId");
        });
        verify(historial, never()).save(any());
    }

    @Test
    void asignar_empresaInexistenteOInactiva_responde400() {
        when(usuarios.findById(6L)).thenReturn(Optional.of(usuario(6L, "cliente", "CLIENTE", null)));
        Cliente inactiva = empresa(3L, RUC_ANDINA, "Inactiva SAC");
        inactiva.setActivo(false);
        when(clientes.findById(3L)).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> servicio.asignar(6L, 3L, 1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCampos()).containsKey("clienteId"));
        assertThatThrownBy(() -> servicio.asignar(6L, 99L, 1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCampos()).containsKey("clienteId"));
    }

    @Test
    void asignar_aUnUsuarioQueNoEsCliente_responde400() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(2L, "agente", "AGENTE", null)));

        assertThatThrownBy(() -> servicio.asignar(2L, 1L, 1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCampos()).containsKey("usuarioId"));
    }

    @Test
    void asignar_usuarioInexistente_responde404() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(99L, 1L, 1L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void accesosDenegados_pidePaginaAcotadaYDevuelveElContenido() {
        var fila = new AccesoDenegadoResponse(1L, AHORA, "jperez", 20L, "DESP-2026-0342", 2L, "Pacífico Trade EIRL",
                MotivoAccesoDenegado.OTRA_EMPRESA);
        when(denegados.listar(PageRequest.of(0, 50))).thenReturn(new PageImpl<>(List.of(fila), PageRequest.of(0, 50), 1));

        var pagina = servicio.accesosDenegados(-3, 500);

        assertThat(pagina.contenido()).containsExactly(fila);
        assertThat(pagina.contenido().get(0).mensaje()).isEqualTo("Despacho no encontrado");
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    private static Usuario usuario(Long id, String nombre, String rol, Long clienteId) {
        Usuario u = new Usuario(nombre, nombre + "@aduanatrack.pe", nombre, "hash", new Rol(rol, rol));
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "clienteId", clienteId);
        return u;
    }

    private static Cliente empresa(Long id, String ruc, String razonSocial) {
        Cliente c = new Cliente(ruc, AHORA);
        c.setRazonSocial(razonSocial);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
