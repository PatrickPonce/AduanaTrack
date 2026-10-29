package pe.edu.usmp.aduanatrack.documento.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;
import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;
import pe.edu.usmp.aduanatrack.documento.repository.DocumentoDespachoRepository;
import pe.edu.usmp.aduanatrack.documento.repository.TipoDocumentoRepository;

/**
 * Estado general de la documentación y bloqueo de HU-06 · T-06.3 (SCRUM-55). La documentación está
 * completa cuando todos los tipos obligatorios tienen una versión vigente: en ese momento el
 * despacho pasa de {@code APERTURA} a {@code DOCUMENTACION_COMPLETA} y se registra el hito (CA4).
 * HU-07 llama a {@link #exigirDocumentacionCompleta} antes de clasificar (CA2).
 */
@Service
public class DocumentacionDespachoService {

    private final DocumentoDespachoRepository documentoRepository;
    private final TipoDocumentoRepository tipoRepository;
    private final DespachoRepository despachoRepository;
    private final HitoDespachoService hitos;

    public DocumentacionDespachoService(DocumentoDespachoRepository documentoRepository,
            TipoDocumentoRepository tipoRepository, DespachoRepository despachoRepository,
            HitoDespachoService hitos) {
        this.documentoRepository = documentoRepository;
        this.tipoRepository = tipoRepository;
        this.despachoRepository = despachoRepository;
        this.hitos = hitos;
    }

    /**
     * Se llama después de cada carga: si ya están todos los obligatorios y el despacho sigue en
     * {@code APERTURA}, lo pasa a {@code DOCUMENTACION_COMPLETA} y registra el hito. En cualquier otro
     * estado no hace nada (reemplazar un documento no retrocede ni repite el hito).
     */
    @Transactional
    public void actualizarEstado(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.getEstado() != EstadoDespacho.APERTURA || !pendientes(despachoId).isEmpty()) {
            return;
        }
        despacho.cambiarEstado(EstadoDespacho.DOCUMENTACION_COMPLETA);
        hitos.registrar(despachoId, TipoHito.DOCUMENTACION_COMPLETA,
                "Documentación completa: se cargaron todos los documentos obligatorios");
    }

    /**
     * Bloqueo de HU-07: 409 {@code DOCUMENTACION_INCOMPLETA} con {@code pendientes} (códigos de tipo)
     * si falta algún documento obligatorio; no hace nada si están todos. 404 si el despacho no existe.
     */
    @Transactional(readOnly = true)
    public void exigirDocumentacionCompleta(Long despachoId) {
        buscarDespacho(despachoId);
        List<TipoDocumento> pendientes = pendientes(despachoId);
        if (pendientes.isEmpty()) {
            return;
        }
        String nombres = pendientes.stream().map(TipoDocumento::getNombre).collect(Collectors.joining(", "));
        throw new ApiException(HttpStatus.CONFLICT, CodigoError.DOCUMENTACION_INCOMPLETA,
                "Faltan documentos obligatorios: " + nombres + ".",
                Map.of("pendientes", pendientes.stream().map(TipoDocumento::getCodigo).toList()));
    }

    /** Tipos obligatorios activos sin versión vigente en el despacho, en el orden del catálogo. */
    private List<TipoDocumento> pendientes(Long despachoId) {
        Set<Long> cargados = documentoRepository.vigentes(despachoId).stream()
                .map(DocumentoDespacho::getTipo)
                .map(TipoDocumento::getId)
                .collect(Collectors.toSet());
        return tipoRepository.findByActivoTrueOrderByOrden().stream()
                .filter(TipoDocumento::isObligatorio)
                .filter(tipo -> !cargados.contains(tipo.getId()))
                .toList();
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }
}
