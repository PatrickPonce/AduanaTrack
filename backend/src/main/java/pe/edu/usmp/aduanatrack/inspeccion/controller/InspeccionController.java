package pe.edu.usmp.aduanatrack.inspeccion.controller;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionRequest;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionResponse;
import pe.edu.usmp.aduanatrack.inspeccion.service.InspeccionService;
import pe.edu.usmp.aduanatrack.inspeccion.service.InspeccionService.ArchivoActa;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * Inspección del despacho (HU-12, contrato C7) y su catálogo de ítems. El POST lo restringe SecurityConfig a
 * ADMIN y AGENTE junto con el resto de {@code /despachos/**}.
 */
@RestController
@RequestMapping("/api/v1")
public class InspeccionController {

    private final InspeccionService inspeccionService;

    public InspeccionController(InspeccionService inspeccionService) {
        this.inspeccionService = inspeccionService;
    }

    @GetMapping("/catalogos/items-inspeccion")
    public List<String> items() {
        return InspeccionService.ITEMS;
    }

    @GetMapping("/despachos/{despachoId}/inspeccion")
    public InspeccionResponse ultima(@PathVariable Long despachoId) {
        return inspeccionService.ultima(despachoId);
    }

    /**
     * Multipart: {@code datos} (JSON con {@code items} y {@code observaciones}) y {@code acta} (PDF,
     * opcional). {@code datos} es opcional aquí para que su falta responda 400 con los campos marcados.
     */
    @PostMapping(value = "/despachos/{despachoId}/inspeccion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public InspeccionResponse registrar(@PathVariable Long despachoId,
            @RequestPart(value = "datos", required = false) InspeccionRequest datos,
            @RequestPart(value = "acta", required = false) MultipartFile acta,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return inspeccionService.registrar(despachoId, usuario.id(), datos, acta);
    }

    @GetMapping("/despachos/{despachoId}/inspeccion/acta")
    public ResponseEntity<Resource> acta(@PathVariable Long despachoId) {
        ArchivoActa archivo = inspeccionService.acta(despachoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(archivo.ruta()));
    }
}
