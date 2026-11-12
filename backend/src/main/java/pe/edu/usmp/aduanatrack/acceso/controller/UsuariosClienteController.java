package pe.edu.usmp.aduanatrack.acceso.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.AsignarEmpresaRequest;
import pe.edu.usmp.aduanatrack.acceso.dto.EmpresaAsignableResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.UsuarioClienteResponse;
import pe.edu.usmp.aduanatrack.acceso.service.UsuariosClienteService;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Administración del acceso por empresa (HU-16, contrato C11). Solo ADMIN (lo exige SecurityConfig). */
@RestController
@RequestMapping("/api/v1/administracion")
public class UsuariosClienteController {

    private final UsuariosClienteService servicio;

    public UsuariosClienteController(UsuariosClienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios-cliente")
    public List<UsuarioClienteResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/empresas-asignables")
    public List<EmpresaAsignableResponse> empresasAsignables() {
        return servicio.empresasAsignables();
    }

    @PutMapping("/usuarios-cliente/{usuarioId}/empresa")
    public UsuarioClienteResponse asignar(@PathVariable Long usuarioId,
            @Valid @RequestBody AsignarEmpresaRequest solicitud, @AuthenticationPrincipal UsuarioAutenticado admin) {
        return servicio.asignar(usuarioId, solicitud.clienteId(), admin.id());
    }

    @GetMapping("/accesos-denegados")
    public PaginaResponse<AccesoDenegadoResponse> accesosDenegados(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicio.accesosDenegados(pagina, tamano);
    }
}
