package pe.edu.usmp.aduanatrack.catalogo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.catalogo.dto.AduanaResponse;
import pe.edu.usmp.aduanatrack.catalogo.dto.RegimenResponse;
import pe.edu.usmp.aduanatrack.catalogo.service.CatalogoService;

@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/regimenes")
    public List<RegimenResponse> regimenes() {
        return catalogoService.regimenes();
    }

    @GetMapping("/aduanas")
    public List<AduanaResponse> aduanas() {
        return catalogoService.aduanas();
    }
}
