package pe.edu.usmp.aduanatrack.clasificacion.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ClasificacionResponse;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.service.DocumentacionDespachoService;

/**
 * Guardar la clasificación arancelaria (HU-07 · T-07.3 · SCRUM-60): exige la documentación completa
 * (HU-06) y que todos los ítems de la factura tengan partida; entonces el despacho pasa a
 * {@code CLASIFICADO} y se registra el hito.
 */
@Service
public class ClasificacionService {

    private final DespachoRepository despachoRepository;
    private final ItemFacturaRepository itemRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentacionDespachoService documentacion;
    private final HitoDespachoService hitos;
    private final Clock clock;

    public ClasificacionService(DespachoRepository despachoRepository, ItemFacturaRepository itemRepository,
            UsuarioRepository usuarioRepository, DocumentacionDespachoService documentacion,
            HitoDespachoService hitos, Clock clock) {
        this.despachoRepository = despachoRepository;
        this.itemRepository = itemRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentacion = documentacion;
        this.hitos = hitos;
        this.clock = clock;
    }

    /**
     * 404 si el despacho no existe; 409 {@code DESPACHO_EN_BORRADOR} si es borrador;
     * 409 {@code DOCUMENTACION_INCOMPLETA} (con {@code pendientes}) si falta un documento obligatorio;
     * 409 {@code CLASIFICACION_CERRADA} si el despacho ya fue liquidado o avanzó más;
     * 409 {@code SIN_ITEMS} si no hay ítems; 409 {@code ITEMS_SIN_PARTIDA} (con {@code items}, los
     * números de los ítems sin partida). Guardar de nuevo un despacho ya clasificado no repite el hito.
     */
    @Transactional
    public ClasificacionResponse guardar(Long despachoId, Long usuarioId) {
        Despacho despacho = despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho con \"Guardar y continuar\" antes de clasificar.");
        }
        documentacion.exigirDocumentacionCompleta(despachoId);
        if (despacho.getEstado().alcanzo(EstadoDespacho.LIQUIDADO)) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.CLASIFICACION_CERRADA,
                    "El despacho ya fue liquidado: la clasificación no se puede modificar.");
        }

        List<ItemFactura> items = itemRepository.deDespacho(despachoId);
        if (items.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.SIN_ITEMS,
                    "Registre al menos un ítem de la factura comercial antes de clasificar.");
        }
        List<Integer> sinPartida = items.stream().filter(i -> i.getPartida() == null).map(ItemFactura::getNumero)
                .toList();
        if (!sinPartida.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.ITEMS_SIN_PARTIDA,
                    "Faltan ítems por clasificar: " + sinPartida.size() + ".", Map.of("items", sinPartida));
        }

        if (despacho.getEstado() != EstadoDespacho.CLASIFICADO) {
            despacho.cambiarEstado(EstadoDespacho.CLASIFICADO);
            hitos.registrar(despachoId, TipoHito.CLASIFICADO,
                    "Clasificación arancelaria guardada (" + items.size() + (items.size() == 1 ? " ítem)" : " ítems)"));
        }
        return new ClasificacionResponse(despacho.getEstado(), usuarioRepository.getReferenceById(usuarioId).getNombre(),
                LocalDateTime.now(clock));
    }
}
