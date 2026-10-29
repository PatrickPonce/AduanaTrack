package pe.edu.usmp.aduanatrack.documento.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoCargadoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoRequeridoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentosDespachoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumentacion;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumento;
import pe.edu.usmp.aduanatrack.documento.dto.VersionDocumentoResponse;
import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;
import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;
import pe.edu.usmp.aduanatrack.documento.repository.DocumentoDespachoRepository;
import pe.edu.usmp.aduanatrack.documento.repository.TipoDocumentoRepository;

/**
 * Carga, reemplazo con versión histórica y descarga de los documentos del despacho
 * (HU-06 · T-06.2, contrato C1). Los PDF se guardan en {@code {directorio}/{despachoId}/{uuid}.pdf}
 * con la misma validación que la carta poder ({@link AlmacenPdf}).
 */
@Service
public class DocumentoDespachoService {

    private final DocumentoDespachoRepository documentoRepository;
    private final TipoDocumentoRepository tipoRepository;
    private final DespachoRepository despachoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentacionDespachoService documentacion;
    private final Clock clock;
    private final Path directorio;

    public DocumentoDespachoService(DocumentoDespachoRepository documentoRepository,
            TipoDocumentoRepository tipoRepository, DespachoRepository despachoRepository,
            UsuarioRepository usuarioRepository, DocumentacionDespachoService documentacion, Clock clock,
            DocumentoProperties propiedades) {
        this.documentoRepository = documentoRepository;
        this.tipoRepository = tipoRepository;
        this.despachoRepository = despachoRepository;
        this.usuarioRepository = usuarioRepository;
        this.documentacion = documentacion;
        this.clock = clock;
        this.directorio = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    /** Tabla «Documentos requeridos del despacho» con el estado general (CA4). */
    @Transactional(readOnly = true)
    public DocumentosDespachoResponse listar(Long despachoId) {
        buscarDespacho(despachoId);
        Map<Long, DocumentoDespacho> vigentes = documentoRepository.vigentes(despachoId).stream()
                .collect(Collectors.toMap(d -> d.getTipo().getId(), Function.identity()));
        List<TipoDocumento> tipos = tipoRepository.findByActivoTrueOrderByOrden();
        List<DocumentoRequeridoResponse> filas = tipos.stream()
                .map(t -> fila(t, vigentes.get(t.getId())))
                .toList();
        boolean completo = tipos.stream()
                .filter(TipoDocumento::isObligatorio)
                .allMatch(t -> vigentes.containsKey(t.getId()));
        return new DocumentosDespachoResponse(completo ? EstadoDocumentacion.COMPLETO : EstadoDocumentacion.PENDIENTE,
                filas);
    }

    /**
     * Carga la primera versión del documento o reemplaza la vigente (CA1 y CA3): la anterior
     * deja de estar vigente pero conserva su fila y su PDF. 404 si el despacho o el tipo no
     * existen, 409 si el despacho es borrador, 400 sin archivo, 413 si pasa de 10 MB y 415 si
     * no es PDF.
     */
    @Transactional
    public DocumentoCargadoResponse cargar(Long despachoId, String codigoTipo, MultipartFile archivo, Long usuarioId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho con \"Guardar y continuar\" antes de cargar documentos.");
        }
        TipoDocumento tipo = buscarTipo(codigoTipo);
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.validacion(Map.of("archivo", "Seleccione el PDF del documento"));
        }

        ArchivoGuardado guardado = AlmacenPdf.guardar(directorio.resolve(despachoId.toString()), archivo);
        try {
            documentoRepository.findByDespachoIdAndTipoIdAndVigenteTrue(despachoId, tipo.getId())
                    .ifPresent(anterior -> {
                        anterior.reemplazar();
                        // Antes de insertar la nueva: solo puede haber una vigente por tipo.
                        documentoRepository.saveAndFlush(anterior);
                    });
            int version = documentoRepository.ultimaVersion(despachoId, tipo.getId()) + 1;
            DocumentoDespacho nuevo = documentoRepository.saveAndFlush(new DocumentoDespacho(despachoId, tipo, version,
                    AlmacenPdf.nombreOriginal(archivo, tipo.getCodigo().toLowerCase(Locale.ROOT) + ".pdf"),
                    despachoId + "/" + guardado.ruta().getFileName(), guardado.tamanoBytes(), guardado.sha256(),
                    usuarioRepository.getReferenceById(usuarioId), LocalDateTime.now(clock)));
            documentacion.actualizarEstado(despachoId);
            return new DocumentoCargadoResponse(nuevo.getId(), tipo.getCodigo(), nuevo.getVersion(),
                    EstadoDocumento.CARGADO);
        } catch (DataIntegrityViolationException e) {
            AlmacenPdf.borrar(guardado.ruta());
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.CARGA_SIMULTANEA,
                    "Otra carga del mismo documento terminó primero. Actualice la página y vuelva a intentarlo.");
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            AlmacenPdf.borrar(guardado.ruta());
            throw e;
        }
    }

    /** Historial de versiones de un tipo de documento, de la más reciente a la más antigua (CA3). */
    @Transactional(readOnly = true)
    public List<VersionDocumentoResponse> versiones(Long despachoId, String codigoTipo) {
        buscarDespacho(despachoId);
        TipoDocumento tipo = buscarTipo(codigoTipo);
        return documentoRepository.versiones(despachoId, tipo.getId()).stream()
                .map(d -> new VersionDocumentoResponse(d.getId(), d.getVersion(), d.isVigente(), d.getNombreArchivo(),
                        d.getTamanoBytes(), d.getUsuario().getNombre(), d.getCargadoEn()))
                .toList();
    }

    /**
     * PDF de cualquier versión del documento. La ruta guardada se resuelve dentro del directorio
     * configurado y nunca fuera de él.
     */
    @Transactional(readOnly = true)
    public ArchivoDocumento archivo(Long despachoId, Long documentoId) {
        DocumentoDespacho documento = documentoRepository.findByIdAndDespachoId(documentoId, despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el documento solicitado."));
        Path ruta = directorio.resolve(documento.getRutaArchivo()).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("El archivo del documento no está disponible.");
        }
        return new ArchivoDocumento(ruta, documento.getNombreArchivo());
    }

    private static DocumentoRequeridoResponse fila(TipoDocumento tipo, DocumentoDespacho vigente) {
        if (vigente == null) {
            return DocumentoRequeridoResponse.pendiente(tipo.getCodigo(), tipo.getNombre(), tipo.isObligatorio());
        }
        return new DocumentoRequeridoResponse(tipo.getCodigo(), tipo.getNombre(), tipo.isObligatorio(),
                EstadoDocumento.CARGADO, vigente.getVersion(), vigente.getId(), vigente.getNombreArchivo(),
                vigente.getTamanoBytes(), vigente.getUsuario().getNombre(), vigente.getCargadoEn());
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    private TipoDocumento buscarTipo(String codigo) {
        return tipoRepository.findByCodigoAndActivoTrue(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.noEncontrado("No existe el tipo de documento " + codigo + "."));
    }

    public record ArchivoDocumento(Path ruta, String nombre) {
    }
}
