package pe.edu.usmp.aduanatrack.clasificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaRequest;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.clasificacion.repository.PartidaReferencialRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

class ItemFacturaServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    private ItemFacturaRepository itemRepository;
    private PartidaReferencialRepository partidaRepository;
    private Despacho despacho;
    private Usuario agente;
    private PartidaReferencial rodamientos;
    private ItemFacturaService servicio;

    @BeforeEach
    void preparar() {
        itemRepository = mock(ItemFacturaRepository.class);
        partidaRepository = mock(PartidaReferencialRepository.class);
        DespachoRepository despachoRepository = mock(DespachoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        rodamientos = new PartidaReferencial("8482100000", "Rodamientos de bolas", new BigDecimal("6.00"),
                "rodamientos de bolas");
        ReflectionTestUtils.setField(rodamientos, "id", 1L);

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of());
        when(partidaRepository.findAllById(any())).thenReturn(List.of(rodamientos));
        when(itemRepository.saveAllAndFlush(any())).thenAnswer(i -> {
            List<ItemFactura> items = i.getArgument(0);
            long siguiente = 100;
            for (ItemFactura item : items) {
                if (item.getId() == null) {
                    ReflectionTestUtils.setField(item, "id", siguiente++);
                }
            }
            return items;
        });

        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ItemFacturaService(itemRepository, partidaRepository, despachoRepository, usuarioRepository,
                Validation.buildDefaultValidatorFactory().getValidator(), reloj);
    }

    @Test
    void filasNuevas_seCreanNumeradasEnElOrdenDeLaLista() {
        List<ItemFacturaResponse> guardados = servicio.guardar(DESPACHO,
                List.of(fila(null, "Rodamiento 6205", "4200.00", 1L), fila(null, "Filtro de aceite", "5800.00", null)),
                AGENTE);

        assertThat(guardados).extracting(ItemFacturaResponse::numero).containsExactly(1, 2);
        assertThat(guardados.get(0).partida().codigoFormateado()).isEqualTo("8482.10.00.00");
        assertThat(guardados.get(1).partida()).isNull();
        assertThat(guardados.get(1).partidaId()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void asignarPartida_dejaConstanciaDeQuienYCuandoClasifico() {
        servicio.guardar(DESPACHO, List.of(fila(null, "Rodamiento 6205", "4200.00", 1L)), AGENTE);

        org.mockito.ArgumentCaptor<List<ItemFactura>> captura = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAllAndFlush(captura.capture());
        ItemFactura item = captura.getValue().get(0);
        assertThat(item.getClasificadoPor()).isSameAs(agente);
        assertThat(item.getClasificadoEn()).isEqualTo(AHORA);
    }

    @Test
    void filaConId_actualizaElItemYConservaLaClasificacionSiLaPartidaNoCambia() {
        ItemFactura existente = new ItemFactura(DESPACHO, 1, "Rodamiento", new BigDecimal("10"), "UND",
                new BigDecimal("100.00"));
        ReflectionTestUtils.setField(existente, "id", 10L);
        Usuario otro = new Usuario("otro", "otro@aduanatrack.pe", "Otro", "hash", new Rol("AGENTE", "Agente"));
        LocalDateTime antes = AHORA.minusDays(1);
        existente.asignarPartida(rodamientos, otro, antes);
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(existente));

        servicio.guardar(DESPACHO, List.of(fila(10L, "Rodamiento 6205 SKF", "4200.00", 1L)), AGENTE);

        assertThat(existente.getDescripcion()).isEqualTo("Rodamiento 6205 SKF");
        assertThat(existente.getValorFob()).isEqualByComparingTo("4200.00");
        assertThat(existente.getClasificadoPor()).isSameAs(otro);
        assertThat(existente.getClasificadoEn()).isEqualTo(antes);
    }

    @Test
    void quitarLaPartida_limpiaLaClasificacion() {
        ItemFactura existente = new ItemFactura(DESPACHO, 1, "Rodamiento", new BigDecimal("10"), "UND",
                new BigDecimal("100.00"));
        ReflectionTestUtils.setField(existente, "id", 10L);
        existente.asignarPartida(rodamientos, agente, AHORA);
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(existente));

        servicio.guardar(DESPACHO, List.of(fila(10L, "Rodamiento", "100.00", null)), AGENTE);

        assertThat(existente.getPartida()).isNull();
        assertThat(existente.getClasificadoPor()).isNull();
        assertThat(existente.getClasificadoEn()).isNull();
    }

    @Test
    void itemQueNoVieneEnLaLista_seQuita() {
        ItemFactura uno = itemGuardado(10L, 1);
        ItemFactura dos = itemGuardado(11L, 2);
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(uno, dos));

        List<ItemFacturaResponse> guardados = servicio.guardar(DESPACHO,
                List.of(fila(11L, "Filtro", "100.00", null)), AGENTE);

        verify(itemRepository).delete(uno);
        verify(itemRepository, never()).delete(dos);
        assertThat(guardados).hasSize(1);
        assertThat(guardados.get(0).numero()).isEqualTo(1);
    }

    @Test
    void campoInvalido_marcaLaFilaYElCampoYNoGuarda() {
        assertThatThrownBy(() -> servicio.guardar(DESPACHO, List.of(fila(null, "Rodamiento", "4200.00", null),
                fila(null, "Filtro", "0", null), fila(null, "", "10.123", null)), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION);
                    assertThat(e.getCampos()).containsKeys("items[1].valorFob", "items[2].descripcion",
                            "items[2].valorFob");
                    assertThat(e.getCampos()).doesNotContainKey("items[0].valorFob");
                });
        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void partidaQueNoExisteEnElCatalogo_marcaElCampoPartidaId() {
        assertThatThrownBy(() -> servicio.guardar(DESPACHO,
                List.of(fila(null, "Rodamiento", "4200.00", 1L), fila(null, "Filtro", "100.00", 77L)), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos())
                        .containsOnlyKeys("items[1].partidaId"));
        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void idDeOtroDespacho_responde404() {
        assertThatThrownBy(() -> servicio.guardar(DESPACHO, List.of(fila(999L, "Rodamiento", "100.00", null)), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void despachoEnBorrador_responde409() {
        Despacho borrador = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        DespachoRepository repo = mock(DespachoRepository.class);
        when(repo.findById(DESPACHO)).thenReturn(Optional.of(borrador));
        ItemFacturaService conBorrador = new ItemFacturaService(itemRepository, partidaRepository, repo,
                mock(UsuarioRepository.class), Validation.buildDefaultValidatorFactory().getValidator(),
                Clock.systemUTC());

        assertThatThrownBy(() -> conBorrador.guardar(DESPACHO, List.of(fila(null, "Rodamiento", "100.00", null)), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR));
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.listar(99L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private ItemFactura itemGuardado(Long id, int numero) {
        ItemFactura item = new ItemFactura(DESPACHO, numero, "Ítem " + numero, new BigDecimal("1"), "UND",
                new BigDecimal("100.00"));
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static ItemFacturaRequest fila(Long id, String descripcion, String fob, Long partidaId) {
        return new ItemFacturaRequest(id, descripcion, new BigDecimal("200"), "UND", new BigDecimal(fob), partidaId);
    }
}
