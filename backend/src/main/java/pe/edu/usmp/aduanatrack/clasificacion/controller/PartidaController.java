package pe.edu.usmp.aduanatrack.clasificacion.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.clasificacion.dto.PartidaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.service.PartidaService;

/** GET /api/v1/catalogos/partidas?q= (contrato C2 · T-07.1), para los tres perfiles. */
@RestController
@RequestMapping("/api/v1/catalogos/partidas")
public class PartidaController {

    private final PartidaService partidaService;

    public PartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @GetMapping
    public List<PartidaResponse> buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        return partidaService.buscar(q);
    }
}
