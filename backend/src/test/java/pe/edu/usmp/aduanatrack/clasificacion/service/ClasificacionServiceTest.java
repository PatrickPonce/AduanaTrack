package pe.edu.usmp.aduanatrack.clasificacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ClasificacionResponse;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.service.DocumentacionDespachoService;

/** Guardar la clasificación de HU-07 · T-07.3 (SCRUM-60); CA3 y CA4 son los criterios de HU-07. */
class ClasificacionServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    private ItemFacturaRepository itemRepository;
    private DocumentacionDespachoService documentacion;
    private HitoDespachoService hitos;
    private Despacho despacho;
    private Usuario agente;
    private PartidaReferencial partida;
    private ClasificacionService servicio;

    @BeforeEach
    void preparar() {
        DespachoRepository despachoRepository = mock(DespachoRepository.class);
        itemRepository = mock(ItemFacturaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        documentacion = mock(DocumentacionDespachoService.class);
        hitos = mock(HitoDespachoService.class);

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.cambiarEstado(EstadoDespacho.DOCUMENTACION_COMPLETA);
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        partida = new PartidaReferencial("8482100000", "Rodamientos de bolas", new BigDecimal("6.00"),
                "rodamientos de bolas");

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);
        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ClasificacionService(despachoRepository, itemRepository, usuarioRepository, documentacion,
                hitos, reloj);
    }

    // CA4: con todos los ítems clasificados, el despacho pasa a CLASIFICADO

    @Test
    void conTodosLosItemsClasificados_pasaAClasificadoYRegistraElHito() {
        items(conPartida(1), conPartida(2));

        ClasificacionResponse respuesta = servicio.guardar(DESPACHO, AGENTE);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CLASIFICADO);
        assertThat(respuesta.estado()).isEqualTo(EstadoDespacho.CLASIFICADO);
        assertThat(respuesta.clasificadoPor()).isEqualTo("Agente Demo");
        assertThat(respuesta.clasificadoEn()).isEqualTo(AHORA);
        verify(hitos).registrar(DESPACHO, TipoHito.CLASIFICADO, "Clasificación arancelaria guardada (2 ítems)");
    }

    @Test
    void conUnSoloItem_elHitoVaEnSingular() {
        items(conPartida(1));

        servicio.guardar(DESPACHO, AGENTE);

        verify(hitos).registrar(DESPACHO, TipoHito.CLASIFICADO, "Clasificación arancelaria guardada (1 ítem)");
    }

    @Test
    void guardarDeNuevoUnDespachoYaClasificado_noRepiteElHito() {
        items(conPartida(1));
        servicio.guardar(DESPACHO, AGENTE);

        ClasificacionResponse repetida = servicio.guardar(DESPACHO, AGENTE);

        assertThat(repetida.estado()).isEqualTo(EstadoDespacho.CLASIFICADO);
        verify(hitos, times(1)).registrar(any(), any(), anyString());
    }

    // CA3: con un ítem sin partida, el guardado responde 409

    @Test
    void conUnItemSinPartida_responde409ConLosNumerosPendientes() {
        items(conPartida(1), sinPartida(2));

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getCodigo()).isEqualTo(CodigoError.ITEMS_SIN_PARTIDA);
            assertThat(e.getDatos()).containsEntry("items", List.of(2));
        });
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DOCUMENTACION_COMPLETA);
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void conVariosItemsSinPartida_losListaTodos() {
        items(sinPartida(1), conPartida(2), sinPartida(3));

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getDatos()).containsEntry("items", List.of(1, 3)));
    }

    @Test
    void sinItems_responde409SinItems() {
        items();

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.SIN_ITEMS));
    }

    // Bloqueo de HU-06 y estados

    @Test
    void sinDocumentacionCompleta_propagaElBloqueoDeHu06() {
        doThrow(new ApiException(HttpStatus.CONFLICT, CodigoError.DOCUMENTACION_INCOMPLETA,
                "Faltan documentos obligatorios: Póliza de seguro.")).when(documentacion)
                .exigirDocumentacionCompleta(DESPACHO);
        items(conPartida(1));

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DOCUMENTACION_INCOMPLETA));
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DOCUMENTACION_COMPLETA);
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void despachoYaLiquidado_responde409ClasificacionCerrada() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);
        items(conPartida(1));

        assertThatThrownBy(() -> servicio.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CLASIFICACION_CERRADA));
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LIQUIDADO);
    }

    @Test
    void despachoEnBorrador_responde409() {
        Despacho borrador = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        DespachoRepository otro = mock(DespachoRepository.class);
        when(otro.findById(DESPACHO)).thenReturn(Optional.of(borrador));
        ClasificacionService sobreBorrador = new ClasificacionService(otro, itemRepository,
                mock(UsuarioRepository.class), documentacion, hitos, Clock.systemUTC());

        assertThatThrownBy(() -> sobreBorrador.guardar(DESPACHO, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR));
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.guardar(99L, AGENTE)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void items(ItemFactura... items) {
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(items));
    }

    private ItemFactura conPartida(int numero) {
        ItemFactura item = sinPartida(numero);
        item.asignarPartida(partida, agente, AHORA);
        return item;
    }

    private static ItemFactura sinPartida(int numero) {
        return new ItemFactura(DESPACHO, numero, "Ítem " + numero, new BigDecimal("10.000"), "UND",
                new BigDecimal("100.00"));
    }
}
