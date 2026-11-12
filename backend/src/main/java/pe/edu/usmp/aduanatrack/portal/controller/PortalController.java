package pe.edu.usmp.aduanatrack.portal.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.portal.dto.MiEmpresaResponse;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;
import pe.edu.usmp.aduanatrack.portal.service.PortalService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Portal del cliente (HU-16, contrato C11). Solo el perfil CLIENTE (lo exige SecurityConfig). */
@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/mi-empresa")
    public MiEmpresaResponse miEmpresa(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return portalService.miEmpresa(usuario);
    }

    @GetMapping("/despachos")
    public List<PortalDespachoResponse> despachos(@RequestParam(required = false) String numero,
            @RequestParam(required = false) String estado, @RequestParam(required = false) String etaDesde,
            @RequestParam(required = false) String etaHasta, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return portalService.despachos(usuario, numero, estado, etaDesde, etaHasta);
    }

    /** Cabecera del detalle (BL, ETA, última actualización) aunque el despacho no esté en la lista filtrada. */
    @GetMapping("/despachos/{id}")
    public PortalDespachoResponse despacho(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return portalService.despacho(usuario, id);
    }
}
