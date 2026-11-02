package pe.edu.usmp.aduanatrack.cita.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.cita.dto.CitaRequest;
import pe.edu.usmp.aduanatrack.cita.dto.CitaResponse;
import pe.edu.usmp.aduanatrack.cita.service.CitaDespachoService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Citas en depósito del despacho (HU-11, contrato C6). El POST lo restringe SecurityConfig a ADMIN y AGENTE. */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/citas")
public class CitaDespachoController {

    private final CitaDespachoService citaService;

    public CitaDespachoController(CitaDespachoService citaService) {
        this.citaService = citaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse registrar(@PathVariable Long despachoId, @Valid @RequestBody CitaRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return citaService.registrar(despachoId, solicitud, usuario.id());
    }

    @GetMapping
    public List<CitaResponse> listar(@PathVariable Long despachoId) {
        return citaService.listar(despachoId);
    }
}
