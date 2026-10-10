package pe.edu.usmp.aduanatrack.embarque.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueRequest;
import pe.edu.usmp.aduanatrack.embarque.dto.EmbarqueResponse;
import pe.edu.usmp.aduanatrack.embarque.service.EmbarqueService;

@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/embarque")
public class EmbarqueController {

    private final EmbarqueService embarqueService;

    public EmbarqueController(EmbarqueService embarqueService) {
        this.embarqueService = embarqueService;
    }

    @GetMapping
    public EmbarqueResponse obtener(@PathVariable Long despachoId) {
        return embarqueService.obtener(despachoId);
    }

    @PutMapping
    public EmbarqueResponse guardar(@PathVariable Long despachoId, @Valid @RequestBody EmbarqueRequest solicitud) {
        return embarqueService.guardar(despachoId, solicitud);
    }
}
