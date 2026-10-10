package pe.edu.usmp.aduanatrack.cliente.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResponse;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderCargaService;

@RestController
@RequestMapping("/api/v1/clientes/{clienteId}/carta-poder")
public class CartaPoderController {

    private final CartaPoderCargaService cargaService;

    public CartaPoderController(CartaPoderCargaService cargaService) {
        this.cargaService = cargaService;
    }

    /**
     * Multipart con {@code archivo}, {@code fechaEmision} y {@code fechaVencimiento}. Los tres
     * son opcionales aquí para que el servicio responda 400 con el campo que falta.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartaPoderResponse cargar(@PathVariable Long clienteId,
            @RequestParam(required = false) MultipartFile archivo,
            @RequestParam(required = false) String fechaEmision,
            @RequestParam(required = false) String fechaVencimiento) {
        return cargaService.cargar(clienteId, archivo, fechaEmision, fechaVencimiento);
    }
}
