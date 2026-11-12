package pe.edu.usmp.aduanatrack.notificacion.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.CantidadNoLeidasResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.NotificacionResponse;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Bandeja de notificaciones del usuario en sesión (HU-14, contrato C9). Todos los perfiles la usan. */
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public PaginaResponse<NotificacionResponse> listar(@RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacionService.listar(usuario.id(), estado, pagina, tamano);
    }

    @GetMapping("/no-leidas/cantidad")
    public CantidadNoLeidasResponse cantidadNoLeidas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacionService.cantidadNoLeidas(usuario.id());
    }

    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notificacionService.marcarLeida(id, usuario.id());
    }
}
