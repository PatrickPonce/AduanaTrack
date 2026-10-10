package pe.edu.usmp.aduanatrack.cliente.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteExisteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteHistorialResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteRequest;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResumen;
import pe.edu.usmp.aduanatrack.cliente.service.ClienteService;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public PaginaResponse<ClienteResumen> buscar(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return clienteService.buscar(q, page, size);
    }

    @GetMapping("/existe")
    public ClienteExisteResponse existe(@RequestParam String ruc) {
        return clienteService.existe(ruc);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest solicitud) {
        ClienteResponse creado = clienteService.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return clienteService.actualizar(id, solicitud, usuario.id());
    }

    @GetMapping("/{id}/historial")
    public List<ClienteHistorialResponse> historial(@PathVariable Long id) {
        return clienteService.historial(id);
    }
}
