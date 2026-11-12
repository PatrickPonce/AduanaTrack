package pe.edu.usmp.aduanatrack.notificacion.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.notificacion.dto.PreferenciasRequest;
import pe.edu.usmp.aduanatrack.notificacion.dto.PreferenciasResponse;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Canal de notificación del usuario en sesión (HU-14, contrato C9). Todos los perfiles lo cambian. */
@RestController
@RequestMapping("/api/v1/usuarios/me/preferencias")
public class PreferenciasController {

    private final NotificacionService notificacionService;

    public PreferenciasController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public PreferenciasResponse obtener(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacionService.preferencias(usuario.id());
    }

    @PutMapping
    public PreferenciasResponse actualizar(@Valid @RequestBody PreferenciasRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacionService.actualizarPreferencias(usuario.id(), solicitud.canalNotificacion());
    }
}
