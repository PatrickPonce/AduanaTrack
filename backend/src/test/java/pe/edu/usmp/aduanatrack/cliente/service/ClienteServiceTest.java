package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteRequest;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;

class ClienteServiceTest {

    private static final String RUC = "20787789324";

    private ClienteRepository clienteRepository;
    private ClienteHistorialRepository historialRepository;
    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        clienteRepository = mock(ClienteRepository.class);
        historialRepository = mock(ClienteHistorialRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        CartaPoderEstadoService cartaPoder = mock(CartaPoderEstadoService.class);
        when(cartaPoder.estado(anyLong())).thenReturn(EstadoCartaPoder.SIN_CARTA);
        when(usuarioRepository.getReferenceById(anyLong())).thenReturn(mock(Usuario.class));
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            if (c.getId() == null) {
                ReflectionTestUtils.setField(c, "id", 1L);
            }
            return c;
        });
        Clock reloj = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ClienteService(clienteRepository, historialRepository, usuarioRepository, cartaPoder, reloj);
    }

    @Test
    void crear_guardaElClienteConLosTextosNormalizados() {
        when(clienteRepository.findByRuc(RUC)).thenReturn(Optional.empty());

        ClienteResponse creado = servicio.crear(new ClienteRequest(RUC, "  Importadora Andina SAC ", "Av. Lima 123",
                "", "987654321", null));

        assertThat(creado.id()).isEqualTo(1L);
        assertThat(creado.razonSocial()).isEqualTo("Importadora Andina SAC");
        assertThat(creado.correo()).isNull();
        assertThat(creado.estadoCartaPoder()).isEqualTo(EstadoCartaPoder.SIN_CARTA);
    }

    @Test
    void crear_rucDuplicado_responde409ConElClienteExistente() {
        Cliente existente = cliente("Importadora Andina SAC");
        when(clienteRepository.findByRuc(RUC)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicio.crear(new ClienteRequest(RUC, "Otra", null, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.CLIENTE_DUPLICADO);
                    assertThat(e.getDatos()).containsEntry("clienteId", 5L)
                            .containsEntry("razonSocial", "Importadora Andina SAC");
                });
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void actualizar_telefonoYCorreo_generaDosFilasDeHistorial() {
        Cliente existente = cliente("Importadora Andina SAC");
        existente.setTelefono("987654321");
        existente.setCorreo("compras@andina.pe");
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(existente));

        servicio.actualizar(5L, new ClienteRequest(RUC, "Importadora Andina SAC", null, "logistica@andina.pe",
                "014445566", null), 9L);

        ArgumentCaptor<List<ClienteHistorial>> captor = ArgumentCaptor.forClass(List.class);
        verify(historialRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(ClienteHistorial::getCampo, ClienteHistorial::getValorAnterior,
                        ClienteHistorial::getValorNuevo)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("correo", "compras@andina.pe", "logistica@andina.pe"),
                        org.assertj.core.groups.Tuple.tuple("telefono", "987654321", "014445566"));
        assertThat(existente.getTelefono()).isEqualTo("014445566");
    }

    @Test
    void actualizar_sinCambios_noGeneraHistorial() {
        Cliente existente = cliente("Importadora Andina SAC");
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(existente));

        servicio.actualizar(5L, new ClienteRequest(RUC, "Importadora Andina SAC", null, null, null, null), 9L);

        verify(historialRepository, never()).saveAll(any());
    }

    @Test
    void actualizar_aUnRucDeOtroCliente_responde409() {
        Cliente existente = cliente("Importadora Andina SAC");
        Cliente otro = new Cliente("20230972151", existente.getCreadoEn());
        ReflectionTestUtils.setField(otro, "id", 6L);
        otro.setRazonSocial("Comercial Lima SA");
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByRuc("20230972151")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> servicio.actualizar(5L,
                new ClienteRequest("20230972151", "Importadora Andina SAC", null, null, null, null), 9L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void actualizar_clienteInexistente_responde404() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(99L,
                new ClienteRequest(RUC, "X", null, null, null, null), 9L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static Cliente cliente(String razonSocial) {
        Cliente c = new Cliente(RUC, java.time.LocalDateTime.of(2026, 10, 1, 9, 0));
        ReflectionTestUtils.setField(c, "id", 5L);
        c.setRazonSocial(razonSocial);
        return c;
    }
}
