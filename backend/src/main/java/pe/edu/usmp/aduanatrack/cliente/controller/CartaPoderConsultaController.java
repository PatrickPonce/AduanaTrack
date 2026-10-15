package pe.edu.usmp.aduanatrack.cliente.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResumen;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderConsultaService;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderConsultaService.ArchivoCartaPoder;

/** Consulta de las cartas poder del cliente (la carga está en {@link CartaPoderController}). */
@RestController
@RequestMapping("/api/v1/clientes/{clienteId}/carta-poder")
public class CartaPoderConsultaController {

    private final CartaPoderConsultaService consultaService;

    public CartaPoderConsultaController(CartaPoderConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    public List<CartaPoderResumen> listar(@PathVariable Long clienteId) {
        return consultaService.listar(clienteId);
    }

    @GetMapping("/{cartaId}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable Long clienteId, @PathVariable Long cartaId) {
        ArchivoCartaPoder archivo = consultaService.archivo(clienteId, cartaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(archivo.ruta()));
    }
}
