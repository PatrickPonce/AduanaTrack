package pe.edu.usmp.aduanatrack.liquidacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionConsultaResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoResponse;
import pe.edu.usmp.aduanatrack.liquidacion.entity.Liquidacion;
import pe.edu.usmp.aduanatrack.liquidacion.entity.PagoTributos;
import pe.edu.usmp.aduanatrack.liquidacion.repository.LiquidacionRepository;
import pe.edu.usmp.aduanatrack.liquidacion.repository.PagoTributosRepository;

class LiquidacionServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);

    private DespachoRepository despachoRepository;
    private ItemFacturaRepository itemRepository;
    private LiquidacionRepository liquidacionRepository;
    private PagoTributosRepository pagoRepository;
    private HitoDespachoService hitoService;
    private Despacho despacho;
    private Usuario agente;
    private LiquidacionService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        itemRepository = mock(ItemFacturaRepository.class);
        liquidacionRepository = mock(LiquidacionRepository.class);
        pagoRepository = mock(PagoTributosRepository.class);
        hitoService = mock(HitoDespachoService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.decimal(ParametroService.TASA_IGV)).thenReturn(new BigDecimal("16"));
        when(parametros.decimal(ParametroService.TASA_IPM)).thenReturn(new BigDecimal("2"));
        when(parametros.decimal(ParametroService.TASA_PERCEPCION)).thenReturn(new BigDecimal("3.5"));

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.cambiarEstado(EstadoDespacho.CLASIFICADO);
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);
        when(liquidacionRepository.findByDespachoId(DESPACHO)).thenReturn(Optional.empty());
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(item("10000.00", true)));

        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new LiquidacionService(despachoRepository, itemRepository, liquidacionRepository, pagoRepository,
                usuarioRepository, new CalculoTributosService(parametros), hitoService, reloj);
    }

    // HU-08 CA1 y CA2: CIF y tributos referenciales

    @Test
    void generar_calculaElCasoDelContratoC3() {
        LiquidacionResponse respuesta = servicio.generar(DESPACHO,
                new LiquidacionRequest(new BigDecimal("800.00"), new BigDecimal("150.00")), AGENTE);

        assertThat(respuesta.fob()).isEqualByComparingTo("10000.00");
        assertThat(respuesta.cif()).isEqualByComparingTo("10950.00");
        assertThat(respuesta.adValorem()).isEqualByComparingTo("657.00");
        assertThat(respuesta.igv()).isEqualByComparingTo("1857.12");
        assertThat(respuesta.ipm()).isEqualByComparingTo("232.14");
        assertThat(respuesta.percepcion()).isEqualByComparingTo("479.37");
        assertThat(respuesta.total()).isEqualByComparingTo("3225.63");
        assertThat(respuesta.moneda()).isEqualTo("USD");
        assertThat(respuesta.estado()).isEqualTo("LIQUIDADO");
    }

    @Test
    void generar_guardaLaLiquidacionConLasTasasUsadasYPasaALiquidado() {
        servicio.generar(DESPACHO, new LiquidacionRequest(new BigDecimal("800.00"), new BigDecimal("150.00")), AGENTE);

        ArgumentCaptor<Liquidacion> guardada = ArgumentCaptor.forClass(Liquidacion.class);
        verify(liquidacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getDespachoId()).isEqualTo(DESPACHO);
        assertThat(guardada.getValue().getTotal()).isEqualByComparingTo("3225.63");
        assertThat(guardada.getValue().getTasaIgv()).isEqualByComparingTo("16");
        assertThat(guardada.getValue().getTasaPercepcion()).isEqualByComparingTo("3.5");
        assertThat(guardada.getValue().getGeneradoPor()).isSameAs(agente);
        assertThat(guardada.getValue().getGeneradoEn()).isEqualTo(AHORA);
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LIQUIDADO);
        verify(hitoService).registrar(eq(DESPACHO), eq(TipoHito.LIQUIDADO), any());
    }

    @Test
    void generarOtraVezSinPago_reemplazaLaLiquidacionAnterior() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);
        Liquidacion anterior = mock(Liquidacion.class);
        when(liquidacionRepository.findByDespachoId(DESPACHO)).thenReturn(Optional.of(anterior));

        servicio.generar(DESPACHO, new LiquidacionRequest(new BigDecimal("0.00"), new BigDecimal("0.00")), AGENTE);

        verify(anterior).reemplazar(any(), eq(agente), eq(AHORA));
        verify(liquidacionRepository, never()).save(any());
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LIQUIDADO);
    }

    @Test
    void generarSinClasificar_responde409() {
        despacho.cambiarEstado(EstadoDespacho.DOCUMENTACION_COMPLETA);

        assertThatThrownBy(() -> servicio.generar(DESPACHO,
                new LiquidacionRequest(BigDecimal.ZERO, BigDecimal.ZERO), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_NO_CLASIFICADO);
                });
        verify(liquidacionRepository, never()).save(any());
    }

    @Test
    void generarConUnItemSinPartida_responde409() {
        when(itemRepository.deDespacho(DESPACHO)).thenReturn(List.of(item("10000.00", true), item("500.00", false)));

        assertThatThrownBy(() -> servicio.generar(DESPACHO,
                new LiquidacionRequest(BigDecimal.ZERO, BigDecimal.ZERO), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_NO_CLASIFICADO));
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CLASIFICADO);
    }

    @Test
    void generarDespachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.generar(99L, new LiquidacionRequest(BigDecimal.ZERO, BigDecimal.ZERO), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // HU-08 CA3 y CA4: pago realizado fuera del sistema

    @Test
    void registrarPago_guardaFechaMontoYMedioDePagoYPasaATributosPagados() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);

        PagoResponse respuesta = servicio.registrarPago(DESPACHO, new PagoRequest(LocalDate.of(2026, 10, 28),
                new BigDecimal("3225.63"), " TRANSFERENCIA ", "00123456"), AGENTE);

        assertThat(respuesta.estado()).isEqualTo("TRIBUTOS_PAGADOS");
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.TRIBUTOS_PAGADOS);
        ArgumentCaptor<PagoTributos> pago = ArgumentCaptor.forClass(PagoTributos.class);
        verify(pagoRepository).save(pago.capture());
        assertThat(pago.getValue().getFecha()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(pago.getValue().getMonto()).isEqualByComparingTo("3225.63");
        assertThat(pago.getValue().getMedioPago()).isEqualTo("TRANSFERENCIA");
        assertThat(pago.getValue().getNumeroOperacion()).isEqualTo("00123456");
        assertThat(pago.getValue().getRegistradoPor()).isSameAs(agente);
        verify(hitoService).registrar(eq(DESPACHO), eq(TipoHito.TRIBUTOS_PAGADOS), any());
    }

    @Test
    void registrarPagoSinLiquidacion_responde409() {
        assertThatThrownBy(() -> servicio.registrarPago(DESPACHO, new PagoRequest(LocalDate.of(2026, 10, 28),
                new BigDecimal("100.00"), "TRANSFERENCIA", "1"), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.LIQUIDACION_PENDIENTE);
                });
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPagoDosVeces_laSegundaResponde409() {
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);

        assertThatThrownBy(() -> servicio.registrarPago(DESPACHO, new PagoRequest(LocalDate.of(2026, 10, 28),
                new BigDecimal("100.00"), "TRANSFERENCIA", "1"), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.LIQUIDACION_PENDIENTE);
                    assertThat(e.getMessage()).contains("ya están pagados");
                });
    }

    @Test
    void registrarPagoConFechaFutura_responde400() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);

        assertThatThrownBy(() -> servicio.registrarPago(DESPACHO, new PagoRequest(LocalDate.of(2026, 10, 29),
                new BigDecimal("100.00"), "TRANSFERENCIA", "1"), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("fecha"));
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LIQUIDADO);
    }

    // Consulta de la liquidación guardada (GET, contrato C3)

    @Test
    void consultar_devuelveElDesgloseGuardadoYElPagoRegistrado() {
        servicio.generar(DESPACHO, new LiquidacionRequest(new BigDecimal("800.00"), new BigDecimal("150.00")), AGENTE);
        ArgumentCaptor<Liquidacion> guardada = ArgumentCaptor.forClass(Liquidacion.class);
        verify(liquidacionRepository).save(guardada.capture());
        when(liquidacionRepository.findByDespachoId(DESPACHO)).thenReturn(Optional.of(guardada.getValue()));
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);
        when(pagoRepository.findByDespachoId(DESPACHO)).thenReturn(Optional.of(new PagoTributos(DESPACHO,
                LocalDate.of(2026, 10, 28), new BigDecimal("3225.63"), "BANCA_POR_CONVENIO", "00123456", agente, AHORA)));

        LiquidacionConsultaResponse consulta = servicio.consultar(DESPACHO);

        assertThat(consulta.liquidacion().cif()).isEqualByComparingTo("10950.00");
        assertThat(consulta.liquidacion().total()).isEqualByComparingTo("3225.63");
        assertThat(consulta.liquidacion().tasas().igv()).isEqualByComparingTo("16");
        assertThat(consulta.liquidacion().estado()).isEqualTo("TRIBUTOS_PAGADOS");
        assertThat(consulta.generadoPor()).isEqualTo("Agente Demo");
        assertThat(consulta.generadoEn()).isEqualTo(AHORA);
        assertThat(consulta.pago().monto()).isEqualByComparingTo("3225.63");
        assertThat(consulta.pago().medioPago()).isEqualTo("BANCA_POR_CONVENIO");
        assertThat(consulta.pago().registradoPor()).isEqualTo("Agente Demo");
    }

    @Test
    void consultarSinLiquidacion_devuelveTodoNulo() {
        LiquidacionConsultaResponse consulta = servicio.consultar(DESPACHO);

        assertThat(consulta.liquidacion()).isNull();
        assertThat(consulta.generadoPor()).isNull();
        assertThat(consulta.pago()).isNull();
    }

    @Test
    void consultarDespachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.consultar(99L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private ItemFactura item(String fob, boolean conPartida) {
        ItemFactura item = new ItemFactura(DESPACHO, 1, "Rodamiento 6205", new BigDecimal("200"), "UND",
                new BigDecimal(fob));
        if (conPartida) {
            PartidaReferencial partida = new PartidaReferencial("8482100000", "Rodamientos de bolas",
                    new BigDecimal("6.00"), "rodamientos de bolas");
            ReflectionTestUtils.setField(partida, "id", 1L);
            item.asignarPartida(partida, agente, AHORA);
        }
        return item;
    }
}
