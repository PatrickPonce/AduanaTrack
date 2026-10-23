package pe.edu.usmp.aduanatrack.despacho.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.despacho.dto.HitoResponse;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;

@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/hitos")
public class HitoDespachoController {

    private final HitoDespachoService hitoService;

    public HitoDespachoController(HitoDespachoService hitoService) {
        this.hitoService = hitoService;
    }

    @GetMapping
    public List<HitoResponse> listar(@PathVariable Long despachoId) {
        return hitoService.listar(despachoId);
    }
}
