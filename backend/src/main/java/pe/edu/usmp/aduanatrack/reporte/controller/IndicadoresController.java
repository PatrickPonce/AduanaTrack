package pe.edu.usmp.aduanatrack.reporte.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.reporte.dto.IndicadoresGestionResponse;
import pe.edu.usmp.aduanatrack.reporte.service.DatosReporteService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * {@code GET /api/v1/reportes/indicadores?desde=&hasta=} (contrato C10 · HU-15 · T-15.1, CA1 y CA2). Calcula
 * los indicadores de gestión del rango con la misma regla que el reporte exportado (T-15.2). El perfil CLIENTE
 * no accede: los indicadores reúnen los despachos de todos los clientes.
 */
@RestController
@RequestMapping("/api/v1/reportes/indicadores")
public class IndicadoresController {

    private final DatosReporteService datosService;

    public IndicadoresController(DatosReporteService datosService) {
        this.datosService = datosService;
    }

    /** Las dos fechas son opcionales aquí para que el servicio responda 400 con el campo marcado. */
    @GetMapping
    public IndicadoresGestionResponse indicadores(@RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario == null || "CLIENTE".equals(usuario.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO,
                    "Solo el administrador y el agente de aduana consultan los indicadores.");
        }
        return IndicadoresGestionResponse.de(datosService.generar(desde, hasta));
    }
}
