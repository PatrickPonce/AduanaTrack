package pe.edu.usmp.aduanatrack.clasificacion.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaRequest;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaResponse.PartidaRef;
import pe.edu.usmp.aduanatrack.clasificacion.entity.ItemFactura;
import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;
import pe.edu.usmp.aduanatrack.clasificacion.repository.ItemFacturaRepository;
import pe.edu.usmp.aduanatrack.clasificacion.repository.PartidaReferencialRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

/**
 * Ítems de la factura comercial del despacho (HU-07 · T-07.2, contrato C2). El PUT recibe la
 * lista completa: crea las filas sin {@code id}, actualiza las que traen {@code id} y quita las
 * que ya no vienen. La validación fila por fila se hace aquí para devolver los campos como
 * {@code items[2].valorFob}.
 */
@Service
public class ItemFacturaService {

    private final ItemFacturaRepository itemRepository;
    private final PartidaReferencialRepository partidaRepository;
    private final DespachoRepository despachoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Validator validator;
    private final Clock clock;

    public ItemFacturaService(ItemFacturaRepository itemRepository, PartidaReferencialRepository partidaRepository,
            DespachoRepository despachoRepository, UsuarioRepository usuarioRepository, Validator validator,
            Clock clock) {
        this.itemRepository = itemRepository;
        this.partidaRepository = partidaRepository;
        this.despachoRepository = despachoRepository;
        this.usuarioRepository = usuarioRepository;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ItemFacturaResponse> listar(Long despachoId) {
        buscarDespacho(despachoId);
        return itemRepository.deDespacho(despachoId).stream().map(ItemFacturaService::respuesta).toList();
    }

    /**
     * Guarda la tabla completa. 400 si alguna fila no valida o su partida no existe, 404 si el
     * despacho no existe o un {@code id} no es de este despacho, 409 si el despacho es borrador.
     */
    @Transactional
    public List<ItemFacturaResponse> guardar(Long despachoId, List<ItemFacturaRequest> filas, Long usuarioId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho con \"Guardar y continuar\" antes de registrar los ítems.");
        }

        Map<Long, PartidaReferencial> partidas = validar(filas);

        Map<Long, ItemFactura> guardados = itemRepository.deDespacho(despachoId).stream()
                .collect(Collectors.toMap(ItemFactura::getId, Function.identity()));
        Set<Long> conservados = new HashSet<>();
        for (ItemFacturaRequest fila : filas) {
            if (fila.id() != null) {
                if (!guardados.containsKey(fila.id())) {
                    throw ApiException.noEncontrado("El ítem " + fila.id() + " no pertenece a este despacho.");
                }
                conservados.add(fila.id());
            }
        }
        guardados.values().stream().filter(i -> !conservados.contains(i.getId())).forEach(itemRepository::delete);

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<ItemFactura> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            ItemFacturaRequest fila = filas.get(i);
            int numero = i + 1;
            ItemFactura item;
            if (fila.id() == null) {
                item = new ItemFactura(despachoId, numero, fila.descripcion().trim(), fila.cantidad(),
                        fila.unidad().trim(), fila.valorFob());
            } else {
                item = guardados.get(fila.id());
                item.actualizar(numero, fila.descripcion().trim(), fila.cantidad(), fila.unidad().trim(),
                        fila.valorFob());
            }
            asignarPartida(item, fila.partidaId() == null ? null : partidas.get(fila.partidaId()), usuario, ahora);
            resultado.add(item);
        }
        // La restricción (despacho_id, numero) es diferida, así que renumerar en la misma transacción es seguro.
        return itemRepository.saveAllAndFlush(resultado).stream().map(ItemFacturaService::respuesta).toList();
    }

    /** Solo cambia quién y cuándo clasificó si la partida es distinta de la que ya tenía (CA2). */
    private static void asignarPartida(ItemFactura item, PartidaReferencial nueva, Usuario usuario,
            LocalDateTime ahora) {
        PartidaReferencial actual = item.getPartida();
        if (nueva == null) {
            if (actual != null) {
                item.quitarPartida();
            }
        } else if (actual == null || !actual.getId().equals(nueva.getId())) {
            item.asignarPartida(nueva, usuario, ahora);
        }
    }

    /** Valida cada fila y comprueba que las partidas existan; devuelve las partidas por id. */
    private Map<Long, PartidaReferencial> validar(List<ItemFacturaRequest> filas) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (int i = 0; i < filas.size(); i++) {
            ItemFacturaRequest fila = filas.get(i);
            if (fila == null) {
                campos.put("items[" + i + "]", "Complete los datos del ítem");
                continue;
            }
            for (ConstraintViolation<ItemFacturaRequest> v : validator.validate(fila)) {
                campos.putIfAbsent("items[" + i + "]." + v.getPropertyPath(), v.getMessage());
            }
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }

        Set<Long> ids = filas.stream().map(ItemFacturaRequest::partidaId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PartidaReferencial> partidas = partidaRepository.findAllById(ids).stream()
                .filter(PartidaReferencial::isActivo)
                .collect(Collectors.toMap(PartidaReferencial::getId, Function.identity()));
        for (int i = 0; i < filas.size(); i++) {
            Long partidaId = filas.get(i).partidaId();
            if (partidaId != null && !partidas.containsKey(partidaId)) {
                campos.put("items[" + i + "].partidaId", "La partida seleccionada no existe en el catálogo");
            }
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }
        return partidas;
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    static ItemFacturaResponse respuesta(ItemFactura item) {
        PartidaReferencial partida = item.getPartida();
        return new ItemFacturaResponse(item.getId(), item.getNumero(), item.getDescripcion(), item.getCantidad(),
                item.getUnidad(), item.getValorFob(), partida == null ? null : partida.getId(),
                partida == null ? null
                        : new PartidaRef(partida.getId(), partida.getCodigoFormateado(), partida.getDescripcion()));
    }
}
