package pe.edu.usmp.aduanatrack.clasificacion.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaRequest;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ItemFacturaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.service.ItemFacturaService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Ítems de la factura comercial del despacho (HU-07, contrato C2). */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/items")
public class ItemFacturaController {

    private final ItemFacturaService itemService;

    public ItemFacturaController(ItemFacturaService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemFacturaResponse> listar(@PathVariable Long despachoId) {
        return itemService.listar(despachoId);
    }

    /** Recibe la lista completa; las filas se validan en el servicio para marcar {@code items[i].campo}. */
    @PutMapping
    public List<ItemFacturaResponse> guardar(@PathVariable Long despachoId,
            @RequestBody List<ItemFacturaRequest> filas, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return itemService.guardar(despachoId, filas, usuario.id());
    }
}
