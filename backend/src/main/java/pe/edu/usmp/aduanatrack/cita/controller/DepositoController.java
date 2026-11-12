package pe.edu.usmp.aduanatrack.cita.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.cita.dto.DepositoResponse;
import pe.edu.usmp.aduanatrack.cita.service.CitaDespachoService;

/** Catálogo de depósitos temporales y almacenes (HU-11, contrato C6). */
@RestController
@RequestMapping("/api/v1/catalogos/depositos")
public class DepositoController {

    private final CitaDespachoService citaService;

    public DepositoController(CitaDespachoService citaService) {
        this.citaService = citaService;
    }

    @GetMapping
    public List<DepositoResponse> depositos() {
        return citaService.depositos();
    }
}
