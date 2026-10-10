package pe.edu.usmp.aduanatrack.despacho.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoRequest;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoResponse;
import pe.edu.usmp.aduanatrack.despacho.service.DespachoService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/despachos")
public class DespachoController {

    private final DespachoService despachoService;

    public DespachoController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @PostMapping
    public ResponseEntity<DespachoResponse> crear(@Valid @RequestBody DespachoRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        DespachoResponse creado = despachoService.crear(solicitud, usuario.id());
        return ResponseEntity.created(URI.create("/api/v1/despachos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public DespachoResponse actualizar(@PathVariable Long id, @Valid @RequestBody DespachoRequest solicitud) {
        return despachoService.actualizar(id, solicitud);
    }

    @GetMapping("/{id}")
    public DespachoResponse obtener(@PathVariable Long id) {
        return despachoService.obtener(id);
    }
}
