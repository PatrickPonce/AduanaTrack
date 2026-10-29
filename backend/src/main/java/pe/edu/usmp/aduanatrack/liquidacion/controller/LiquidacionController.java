package pe.edu.usmp.aduanatrack.liquidacion.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoRequest;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoResponse;
import pe.edu.usmp.aduanatrack.liquidacion.service.LiquidacionService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Liquidación referencial y pago de tributos del despacho (HU-08, contrato C3). */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}")
public class LiquidacionController {

    private final LiquidacionService liquidacionService;

    public LiquidacionController(LiquidacionService liquidacionService) {
        this.liquidacionService = liquidacionService;
    }

    @PostMapping("/liquidacion")
    @ResponseStatus(HttpStatus.CREATED)
    public LiquidacionResponse generar(@PathVariable Long despachoId, @Valid @RequestBody LiquidacionRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return liquidacionService.generar(despachoId, solicitud, usuario.id());
    }

    @PostMapping("/pago")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse registrarPago(@PathVariable Long despachoId, @Valid @RequestBody PagoRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return liquidacionService.registrarPago(despachoId, solicitud, usuario.id());
    }
}
