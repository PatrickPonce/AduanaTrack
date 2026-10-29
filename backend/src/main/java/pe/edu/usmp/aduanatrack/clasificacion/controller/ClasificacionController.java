package pe.edu.usmp.aduanatrack.clasificacion.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.clasificacion.dto.ClasificacionResponse;
import pe.edu.usmp.aduanatrack.clasificacion.service.ClasificacionService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** POST /api/v1/despachos/{id}/clasificacion (contrato C2 · T-07.3), solo ADMIN y AGENTE. */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/clasificacion")
public class ClasificacionController {

    private final ClasificacionService clasificacionService;

    public ClasificacionController(ClasificacionService clasificacionService) {
        this.clasificacionService = clasificacionService;
    }

    @PostMapping
    public ClasificacionResponse guardar(@PathVariable Long despachoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return clasificacionService.guardar(despachoId, usuario.id());
    }
}
