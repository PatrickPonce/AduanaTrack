package pe.edu.usmp.aduanatrack.documento.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.documento.dto.DocumentoCargadoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentosDespachoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.VersionDocumentoResponse;
import pe.edu.usmp.aduanatrack.documento.service.DocumentoDespachoService;
import pe.edu.usmp.aduanatrack.documento.service.DocumentoDespachoService.ArchivoDocumento;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Documentos del despacho (HU-06, contrato C1). */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/documentos")
public class DocumentoDespachoController {

    private final DocumentoDespachoService documentoService;

    public DocumentoDespachoController(DocumentoDespachoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public DocumentosDespachoResponse listar(@PathVariable Long despachoId) {
        return documentoService.listar(despachoId);
    }

    /** Multipart con {@code archivo}; es opcional aquí para que el servicio responda 400 en el campo. */
    @PostMapping("/{tipo}")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoCargadoResponse cargar(@PathVariable Long despachoId, @PathVariable String tipo,
            @RequestParam(required = false) MultipartFile archivo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return documentoService.cargar(despachoId, tipo, archivo, usuario.id());
    }

    @GetMapping("/{tipo}/versiones")
    public List<VersionDocumentoResponse> versiones(@PathVariable Long despachoId, @PathVariable String tipo) {
        return documentoService.versiones(despachoId, tipo);
    }

    @GetMapping("/{documentoId}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long despachoId, @PathVariable Long documentoId) {
        ArchivoDocumento archivo = documentoService.archivo(despachoId, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(archivo.ruta()));
    }
}
