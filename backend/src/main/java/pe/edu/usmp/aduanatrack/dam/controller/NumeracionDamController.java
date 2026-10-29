package pe.edu.usmp.aduanatrack.dam.controller;

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

import pe.edu.usmp.aduanatrack.dam.dto.DamHistorialResponse;
import pe.edu.usmp.aduanatrack.dam.dto.DamResponse;
import pe.edu.usmp.aduanatrack.dam.service.NumeracionDamService;
import pe.edu.usmp.aduanatrack.dam.service.NumeracionDamService.ArchivoCargo;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Numeración de la DAM del despacho (HU-09, contrato C4). */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/dam")
public class NumeracionDamController {

    private final NumeracionDamService damService;

    public NumeracionDamController(NumeracionDamService damService) {
        this.damService = damService;
    }

    @GetMapping
    public DamResponse obtener(@PathVariable Long despachoId) {
        return damService.obtener(despachoId);
    }

    /**
     * Multipart: {@code numeroDam} (obligatorio), {@code fechaNumeracion} (AAAA-MM-DD, opcional) y
     * {@code cargo} (PDF, opcional). Los parámetros son opcionales aquí para que el servicio
     * responda 400 con el campo marcado.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DamResponse registrar(@PathVariable Long despachoId, @RequestParam(required = false) String numeroDam,
            @RequestParam(required = false) String fechaNumeracion,
            @RequestParam(required = false) MultipartFile cargo) {
        return damService.registrar(despachoId, numeroDam, fechaNumeracion, cargo);
    }

    /**
     * Rectifica el N° de DAM o su fecha (HU-09, regla 3). Multipart: {@code numeroDam}, {@code motivo}
     * (obligatorios), {@code fechaNumeracion} (opcional; si falta se conserva la actual) y {@code cargo}
     * (PDF, opcional). El estado no cambia y queda un registro en el historial.
     */
    @PostMapping("/rectificacion")
    public DamResponse rectificar(@PathVariable Long despachoId, @RequestParam(required = false) String numeroDam,
            @RequestParam(required = false) String fechaNumeracion, @RequestParam(required = false) String motivo,
            @RequestParam(required = false) MultipartFile cargo, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return damService.rectificar(despachoId, numeroDam, fechaNumeracion, motivo, cargo, usuario.id());
    }

    @GetMapping("/historial")
    public List<DamHistorialResponse> historial(@PathVariable Long despachoId) {
        return damService.historial(despachoId);
    }

    @GetMapping("/cargo")
    public ResponseEntity<Resource> cargo(@PathVariable Long despachoId) {
        ArchivoCargo archivo = damService.cargo(despachoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(archivo.ruta()));
    }
}
