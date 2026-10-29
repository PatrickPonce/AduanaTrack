package pe.edu.usmp.aduanatrack.liquidacion.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ItemCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionConsultaResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo.Tasas;
import pe.edu.usmp.aduanatrack.liquidacion.entity.Liquidacion;
import pe.edu.usmp.aduanatrack.liquidacion.entity.PagoTributos;
import pe.edu.usmp.aduanatrack.liquidacion.repository.LiquidacionRepository;
import pe.edu.usmp.aduanatrack.liquidacion.repository.PagoTributosRepository;

/**
 * Liquidación referencial y registro del pago de tributos (HU-08 · T-08.2, contrato C3). El cálculo
 * lo hace {@link CalculoTributosService}; aquí se validan los estados, se guardan las tablas y se
 * registra el hito en la misma transacción que el cambio de estado.
 */
@Service
public class LiquidacionService {

    private final DespachoRepository despachoRepository;
    private final ItemFacturaRepository itemRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final PagoTributosRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculoTributosService calculoService;
    private final HitoDespachoService hitoService;
    private final Clock clock;

    public LiquidacionService(DespachoRepository despachoRepository, ItemFacturaRepository itemRepository,
            LiquidacionRepository liquidacionRepository, PagoTributosRepository pagoRepository,
            UsuarioRepository usuarioRepository, CalculoTributosService calculoService,
            HitoDespachoService hitoService, Clock clock) {
        this.despachoRepository = despachoRepository;
        this.itemRepository = itemRepository;
        this.liquidacionRepository = liquidacionRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculoService = calculoService;
        this.hitoService = hitoService;
        this.clock = clock;
    }

    /**
     * Calcula y guarda la liquidación (CA1 y CA2) y deja el despacho en {@code LIQUIDADO}. Se puede
     * volver a generar mientras no haya pago: reemplaza la anterior. 409 {@code DESPACHO_NO_CLASIFICADO}
     * si el despacho no está clasificado.
     */
    @Transactional
    public LiquidacionResponse generar(Long despachoId, LiquidacionRequest solicitud, Long usuarioId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.getEstado() != EstadoDespacho.CLASIFICADO && despacho.getEstado() != EstadoDespacho.LIQUIDADO) {
            throw noClasificado("Clasifique todos los ítems del despacho antes de generar la liquidación.");
        }

        List<ItemFactura> items = itemRepository.deDespacho(despachoId);
        if (items.isEmpty() || items.stream().anyMatch(i -> i.getPartida() == null)) {
            throw noClasificado("Todos los ítems de la factura deben tener partida arancelaria.");
        }
        ResultadoCalculo calculo = calculoService.calcular(
                items.stream().map(i -> new ItemCalculo(i.getValorFob(), i.getPartida().getTasaAdValorem())).toList(),
                solicitud.flete(), solicitud.seguro());

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);
        liquidacionRepository.findByDespachoId(despachoId).ifPresentOrElse(
                existente -> existente.reemplazar(calculo, usuario, ahora),
                () -> liquidacionRepository.save(new Liquidacion(despachoId, calculo, usuario, ahora)));

        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);
        hitoService.registrar(despachoId, TipoHito.LIQUIDADO,
                "Liquidación referencial generada (total USD " + calculo.total().toPlainString() + ")");
        return LiquidacionResponse.de(calculo, EstadoDespacho.LIQUIDADO.name());
    }

    /**
     * Registra el pago hecho fuera del sistema (CA3) y pasa el despacho a {@code TRIBUTOS_PAGADOS}
     * (CA4). 409 {@code LIQUIDACION_PENDIENTE} si el despacho no está en {@code LIQUIDADO}.
     */
    @Transactional
    public PagoResponse registrarPago(Long despachoId, PagoRequest solicitud, Long usuarioId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.getEstado() != EstadoDespacho.LIQUIDADO) {
            String mensaje = despacho.getEstado().alcanzo(EstadoDespacho.TRIBUTOS_PAGADOS)
                    ? "Los tributos de este despacho ya están pagados."
                    : "Genere la liquidación antes de registrar el pago.";
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.LIQUIDACION_PENDIENTE, mensaje);
        }
        LocalDate hoy = LocalDate.now(clock);
        if (solicitud.fecha().isAfter(hoy)) {
            throw ApiException.validacion(Map.of("fecha", "La fecha del pago no puede ser futura"));
        }

        pagoRepository.save(new PagoTributos(despachoId, solicitud.fecha(), solicitud.monto(),
                solicitud.medioPago().trim(), solicitud.numeroOperacion().trim(),
                usuarioRepository.getReferenceById(usuarioId), LocalDateTime.now(clock)));
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);
        hitoService.registrar(despachoId, TipoHito.TRIBUTOS_PAGADOS, "Pago de tributos registrado (USD "
                + solicitud.monto().toPlainString() + ", " + solicitud.medioPago().trim() + ")");
        return new PagoResponse(EstadoDespacho.TRIBUTOS_PAGADOS.name());
    }

    /** Liquidación y pago guardados del despacho; todo nulo si aún no se generó. 404 si el despacho no existe. */
    @Transactional(readOnly = true)
    public LiquidacionConsultaResponse consultar(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        LiquidacionConsultaResponse.PagoConsulta pago = pagoRepository.findByDespachoId(despachoId)
                .map(p -> new LiquidacionConsultaResponse.PagoConsulta(p.getFecha(), p.getMonto(), p.getMedioPago(),
                        p.getNumeroOperacion(), p.getRegistradoPor().getNombre(), p.getRegistradoEn()))
                .orElse(null);
        return liquidacionRepository.findByDespachoId(despachoId)
                .map(l -> new LiquidacionConsultaResponse(
                        new LiquidacionResponse(l.getFob(), l.getFlete(), l.getSeguro(), l.getCif(), l.getAdValorem(),
                                l.getIgv(), l.getIpm(), l.getPercepcion(), l.getTotal(), LiquidacionResponse.MONEDA,
                                new Tasas(l.getTasaIgv(), l.getTasaIpm(), l.getTasaPercepcion()),
                                despacho.getEstado().name()),
                        l.getGeneradoPor().getNombre(), l.getGeneradoEn(), pago))
                .orElse(new LiquidacionConsultaResponse(null, null, null, pago));
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    private static ApiException noClasificado(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_NO_CLASIFICADO, mensaje);
    }
}
