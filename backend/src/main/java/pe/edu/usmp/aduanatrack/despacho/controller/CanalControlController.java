package pe.edu.usmp.aduanatrack.despacho.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.despacho.dto.CanalRequest;
import pe.edu.usmp.aduanatrack.despacho.dto.CanalResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.service.CanalControlService;

/** PUT /api/v1/despachos/{id}/canal (contrato C5 · T-10.2), solo ADMIN y AGENTE. */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/canal")
public class CanalControlController {

    private final CanalControlService canalService;

    public CanalControlController(CanalControlService canalService) {
        this.canalService = canalService;
    }

    @PutMapping
    public CanalResponse guardar(@PathVariable Long despachoId, @Valid @RequestBody CanalRequest solicitud) {
        return canalService.guardar(despachoId, CanalControl.valueOf(solicitud.canal()));
    }
}
