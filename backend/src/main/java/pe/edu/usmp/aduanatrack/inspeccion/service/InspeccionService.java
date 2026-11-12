package pe.edu.usmp.aduanatrack.inspeccion.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf.ArchivoGuardado;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionRequest;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionResponse;
import pe.edu.usmp.aduanatrack.inspeccion.entity.InspeccionDespacho;
import pe.edu.usmp.aduanatrack.inspeccion.entity.ItemInspeccion;
import pe.edu.usmp.aduanatrack.inspeccion.entity.ResultadoInspeccion;
import pe.edu.usmp.aduanatrack.inspeccion.repository.InspeccionDespachoRepository;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.service.AvisoClienteService;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;

/**
 * Resultado de la inspección aduanera (HU-12 · T-12.1 y T-12.2, contrato C7). El agente marca cada ítem del
 * checklist verificado en el depósito; el sistema calcula el resultado (CA1), avisa si hay observaciones
 * (CA2), habilita el levante si es conforme (CA3) y guarda el acta en PDF opcional (CA4).
 */
@Service
public class InspeccionService {

    /** Catálogo fijo de ítems que el agente verifica, en el orden del checklist (lo confirma el PO). */
    public static final List<String> ITEMS = List.of(
            "Cantidad de bultos",
            "Peso y volumen declarados",
            "Descripción de la mercancía",
            "Estado y embalaje de la mercancía",
            "Rotulado y marcas",
            "Precintos y contenedor");

    static final int MAX_OBSERVACION_ITEM = 255;
    static final int MAX_OBSERVACIONES = 500;
    private static final String SUBCARPETA_ACTA = "acta-inspeccion";

    private final DespachoRepository despachoRepository;
    private final InspeccionDespachoRepository inspeccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final HitoDespachoService hitos;
    private final NotificacionService notificaciones;
    private final AvisoClienteService avisosCliente;
    private final Clock clock;
    private final Path directorio;

    public InspeccionService(DespachoRepository despachoRepository,
            InspeccionDespachoRepository inspeccionRepository, UsuarioRepository usuarioRepository,
            HitoDespachoService hitos, NotificacionService notificaciones, AvisoClienteService avisosCliente,
            Clock clock,
            DocumentoProperties propiedades) {
        this.despachoRepository = despachoRepository;
        this.inspeccionRepository = inspeccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.hitos = hitos;
        this.notificaciones = notificaciones;
        this.avisosCliente = avisosCliente;
        this.clock = clock;
        this.directorio = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    /** Última inspección del despacho; 404 si el despacho no existe o todavía no tiene ninguna. */
    @Transactional(readOnly = true)
    public InspeccionResponse ultima(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        InspeccionDespacho inspeccion = inspeccionRepository.ultima(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("El despacho todavía no tiene inspección registrada."));
        return InspeccionResponse.de(inspeccion, despacho.getEstado());
    }

    /**
     * Guarda la inspección. 400 si el checklist no es válido, 404 si el despacho no existe y 409
     * {@code INSPECCION_NO_PROGRAMADA} si no está en {@code INSPECCION_PROGRAMADA}. Con todos los ítems
     * conformes pasa a {@code INSPECCION_CONFORME}; si no, el estado se mantiene y se puede repetir.
     */
    @Transactional
    public InspeccionResponse registrar(Long despachoId, Long usuarioId, InspeccionRequest solicitud,
            MultipartFile acta) {
        Despacho despacho = buscarDespacho(despachoId);
        validarEstado(despacho);
        List<ItemInspeccion> items = validarItems(solicitud == null ? null : solicitud.items());
        String observaciones = validarObservaciones(solicitud == null ? null : solicitud.observaciones());

        ResultadoInspeccion resultado = items.stream().allMatch(ItemInspeccion::isConforme)
                ? ResultadoInspeccion.CONFORME
                : ResultadoInspeccion.CON_OBSERVACIONES;
        InspeccionDespacho inspeccion = new InspeccionDespacho(despachoId, resultado, observaciones,
                usuarioRepository.getReferenceById(usuarioId), LocalDateTime.now(clock), items);

        ArchivoGuardado guardado = null;
        if (acta != null && !acta.isEmpty()) {
            guardado = AlmacenPdf.guardar(directorio.resolve(despachoId.toString()).resolve(SUBCARPETA_ACTA), acta);
            inspeccion.adjuntarActa(AlmacenPdf.nombreOriginal(acta, "acta-inspeccion.pdf"),
                    despachoId + "/" + SUBCARPETA_ACTA + "/" + guardado.ruta().getFileName());
        }
        try {
            inspeccionRepository.saveAndFlush(inspeccion);
            registrarEfectos(despachoId, despacho, resultado, items);
            return InspeccionResponse.de(inspeccion, despacho.getEstado());
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            if (guardado != null) {
                AlmacenPdf.borrar(guardado.ruta());
            }
            throw e;
        }
    }

    /** PDF del acta de la última inspección; la ruta se resuelve dentro del directorio configurado. */
    @Transactional(readOnly = true)
    public ArchivoActa acta(Long despachoId) {
        buscarDespacho(despachoId);
        InspeccionDespacho inspeccion = inspeccionRepository.ultima(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("El despacho todavía no tiene inspección registrada."));
        if (inspeccion.getActaRuta() == null) {
            throw ApiException.noEncontrado("La inspección no tiene acta adjunta.");
        }
        Path ruta = directorio.resolve(inspeccion.getActaRuta()).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("El archivo del acta no está disponible.");
        }
        return new ArchivoActa(ruta, inspeccion.getActaNombre());
    }

    /** CA2 y CA3: el hito, el aviso al agente y, si es conforme, el paso a {@code INSPECCION_CONFORME}. */
    private void registrarEfectos(Long despachoId, Despacho despacho, ResultadoInspeccion resultado,
            List<ItemInspeccion> items) {
        String numero = despacho.getNumero();
        if (resultado == ResultadoInspeccion.CONFORME) {
            despacho.cambiarEstado(EstadoDespacho.INSPECCION_CONFORME);
            despachoRepository.saveAndFlush(despacho);
            hitos.registrar(despachoId, TipoHito.INSPECCION_CONFORME,
                    "Inspección conforme: el levante queda habilitado.");
            notificaciones.notificarAgente(despacho, TipoNotificacion.INSPECCION_CONFORME, Severidad.INFO,
                    "Inspección conforme", numero + ": el levante quedó habilitado.");
            avisosCliente.inspeccionConforme(despacho);
            return;
        }
        List<String> noConformes = items.stream().filter(i -> !i.isConforme())
                .map(i -> i.getDescripcion() + " (" + i.getObservacion() + ")").toList();
        String detalle = String.join("; ", noConformes);
        hitos.registrar(despachoId, TipoHito.INSPECCION_CON_OBSERVACIONES, "Inspección con observaciones: " + detalle);
        notificaciones.notificarAgente(despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones", numero + ": " + detalle);
        avisosCliente.inspeccionConObservaciones(despacho, detalle);
    }

    private static void validarEstado(Despacho despacho) {
        EstadoDespacho estado = despacho.getEstado();
        if (estado == EstadoDespacho.INSPECCION_PROGRAMADA) {
            return;
        }
        String mensaje;
        if (despacho.esBorrador()) {
            mensaje = "Confirme el despacho antes de registrar la inspección.";
        } else if (estado.alcanzo(EstadoDespacho.INSPECCION_CONFORME)) {
            mensaje = "La inspección ya se registró como conforme: continúe con el levante.";
        } else {
            mensaje = "Programe la cita de inspección antes de registrar su resultado.";
        }
        throw new ApiException(HttpStatus.CONFLICT, CodigoError.INSPECCION_NO_PROGRAMADA, mensaje);
    }

    /** El checklist trae todos los ítems del catálogo, una vez cada uno; el no conforme exige observación. */
    private static List<ItemInspeccion> validarItems(List<InspeccionRequest.Item> recibidos) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (recibidos == null || recibidos.isEmpty()) {
            throw ApiException.validacion(Map.of("items", "Complete el checklist de la inspección"));
        }
        Set<String> vistos = new LinkedHashSet<>();
        List<ItemInspeccion> items = new ArrayList<>();
        for (int i = 0; i < recibidos.size(); i++) {
            InspeccionRequest.Item item = recibidos.get(i);
            String descripcion = item == null || item.descripcion() == null ? "" : item.descripcion().trim();
            String clave = "item-" + i;
            if (!ITEMS.contains(descripcion)) {
                errores.put(clave, "Ítem no válido");
                continue;
            }
            if (!vistos.add(descripcion)) {
                errores.put(clave, "Ítem repetido");
                continue;
            }
            if (item.conforme() == null) {
                errores.put(clave, "Indique si el ítem es conforme");
                continue;
            }
            String observacion = item.observacion() == null ? "" : item.observacion().trim();
            if (!item.conforme() && observacion.isEmpty()) {
                errores.put(clave, "Indique la observación del ítem");
            } else if (!item.conforme() && observacion.length() > MAX_OBSERVACION_ITEM) {
                errores.put(clave, "Máximo " + MAX_OBSERVACION_ITEM + " caracteres");
            }
            items.add(new ItemInspeccion(ITEMS.indexOf(descripcion), descripcion, item.conforme(),
                    observacion.isEmpty() ? null : observacion));
        }
        if (errores.isEmpty() && vistos.size() != ITEMS.size()) {
            errores.put("items", "Faltan ítems del checklist");
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
        items.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        return items;
    }

    private static String validarObservaciones(String observaciones) {
        String texto = observaciones == null ? "" : observaciones.trim();
        if (texto.length() > MAX_OBSERVACIONES) {
            throw ApiException.validacion(Map.of("observaciones", "Máximo " + MAX_OBSERVACIONES + " caracteres"));
        }
        return texto.isEmpty() ? null : texto;
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    public record ArchivoActa(Path ruta, String nombre) {
    }
}
