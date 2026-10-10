package pe.edu.usmp.aduanatrack.despacho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;
import pe.edu.usmp.aduanatrack.catalogo.entity.RegimenAduanero;
import pe.edu.usmp.aduanatrack.catalogo.repository.AduanaRepository;
import pe.edu.usmp.aduanatrack.catalogo.repository.RegimenAduaneroRepository;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoRequest;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.repository.CorrelativoDespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

class DespachoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 8);

    private DespachoRepository despachoRepository;
    private CorrelativoDespachoRepository correlativo;
    private CartaPoderService cartaPoder;
    private DespachoService servicio;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        correlativo = mock(CorrelativoDespachoRepository.class);
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        RegimenAduaneroRepository regimenRepository = mock(RegimenAduaneroRepository.class);
        AduanaRepository aduanaRepository = mock(AduanaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        cartaPoder = mock(CartaPoderService.class);
        CartaPoderEstadoService cartaPoderEstado = mock(CartaPoderEstadoService.class);

        cliente = new Cliente("20787789324", LocalDateTime.of(2026, 10, 1, 9, 0));
        ReflectionTestUtils.setField(cliente, "id", 1L);
        cliente.setRazonSocial("Importadora Andina SAC");
        RegimenAduanero regimen = new RegimenAduanero("10", "Importación para el consumo");
        ReflectionTestUtils.setField(regimen, "id", 10L);
        Aduana aduana = new Aduana("118", "Marítima del Callao");
        ReflectionTestUtils.setField(aduana, "id", 118L);
        Usuario agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash",
                new Rol("AGENTE", "Agente"));

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());
        when(regimenRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(regimen));
        when(aduanaRepository.findByIdAndActivoTrue(118L)).thenReturn(Optional.of(aduana));
        when(usuarioRepository.getReferenceById(anyLong())).thenReturn(agente);
        when(cartaPoderEstado.estado(anyLong())).thenReturn(EstadoCartaPoder.VIGENTE);
        when(despachoRepository.save(any(Despacho.class))).thenAnswer(i -> i.getArgument(0));
        when(correlativo.siguiente(2026)).thenReturn(1, 2);

        Clock reloj = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new DespachoService(despachoRepository, correlativo, clienteRepository, regimenRepository,
                aduanaRepository, usuarioRepository, cartaPoder, cartaPoderEstado, reloj);
    }

    @Test
    void dosDespachosSeguidos_recibenNumerosCorrelativosYEstadoApertura() {
        DespachoResponse primero = servicio.crear(completo(false), 9L);
        DespachoResponse segundo = servicio.crear(completo(false), 9L);

        assertThat(primero.numero()).isEqualTo("DESP-2026-0001");
        assertThat(segundo.numero()).isEqualTo("DESP-2026-0002");
        assertThat(primero.estado()).isEqualTo(EstadoDespacho.APERTURA);
        assertThat(primero.aduana().codigo()).isEqualTo("118");
        assertThat(primero.agente()).isEqualTo("Agente Demo");
    }

    @Test
    void borrador_noConsumeNumeroNiValidaCartaPoder() {
        DespachoResponse borrador = servicio.crear(new DespachoRequest(1L, null, null, null, null, true), 9L);

        assertThat(borrador.numero()).isNull();
        assertThat(borrador.estado()).isEqualTo(EstadoDespacho.BORRADOR);
        verify(correlativo, never()).siguiente(anyInt());
        verify(cartaPoder, never()).exigirVigente(anyLong(), any());
    }

    @Test
    void borradorSinCliente_responde400() {
        assertThatThrownBy(() -> servicio.crear(new DespachoRequest(null, null, null, null, null, true), 9L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsOnlyKeys("clienteId"));
    }

    @Test
    void obligatoriosVacios_responde400ConTodosLosCampos() {
        assertThatThrownBy(() -> servicio.crear(new DespachoRequest(1L, null, null, null, " ", false), 9L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsOnlyKeys("regimenId", "aduanaId", "descripcionMercancia");
                });
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void clienteSinCartaPoderVigente_responde422SinGuardarNiConsumirNumero() {
        doThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, CodigoError.CARTA_PODER_NO_VIGENTE, "x"))
                .when(cartaPoder).exigirVigente(1L, HOY);

        assertThatThrownBy(() -> servicio.crear(completo(false), 9L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CARTA_PODER_NO_VIGENTE));
        verify(correlativo, never()).siguiente(anyInt());
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void clienteInexistente_responde400EnElCampoCliente() {
        assertThatThrownBy(() -> servicio.crear(new DespachoRequest(2L, 10L, 118L, null, "Repuestos", false), 9L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("clienteId"));
    }

    @Test
    void confirmarBorrador_generaNumeroYPasaAApertura() {
        Despacho borrador = new Despacho(cliente, null, LocalDateTime.of(2026, 10, 8, 9, 0));
        ReflectionTestUtils.setField(borrador, "agente",
                new Usuario("agente", "a@a.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente")));
        when(despachoRepository.buscarDetalle(5L)).thenReturn(Optional.of(borrador));

        DespachoResponse confirmado = servicio.actualizar(5L, completo(false));

        assertThat(confirmado.numero()).isEqualTo("DESP-2026-0001");
        assertThat(confirmado.estado()).isEqualTo(EstadoDespacho.APERTURA);
        verify(cartaPoder).exigirVigente(1L, HOY);
    }

    @Test
    void despachoAbierto_noVuelveABorradorNiCambiaDeNumero() {
        Despacho abierto = new Despacho(cliente, null, LocalDateTime.of(2026, 10, 8, 9, 0));
        ReflectionTestUtils.setField(abierto, "agente",
                new Usuario("agente", "a@a.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente")));
        abierto.abrir("DESP-2026-0007");
        when(despachoRepository.buscarDetalle(5L)).thenReturn(Optional.of(abierto));

        DespachoResponse resultado = servicio.actualizar(5L, completo(true));

        assertThat(resultado.numero()).isEqualTo("DESP-2026-0007");
        assertThat(resultado.estado()).isEqualTo(EstadoDespacho.APERTURA);
        verify(correlativo, never()).siguiente(anyInt());
    }

    @Test
    void formatoDelNumero() {
        assertThat(DespachoService.formatearNumero(2026, 1)).isEqualTo("DESP-2026-0001");
        assertThat(DespachoService.formatearNumero(2026, 1234)).isEqualTo("DESP-2026-1234");
    }

    private static DespachoRequest completo(boolean borrador) {
        return new DespachoRequest(1L, 10L, 118L, "PO-4471", "Repuestos automotrices", borrador);
    }
}
