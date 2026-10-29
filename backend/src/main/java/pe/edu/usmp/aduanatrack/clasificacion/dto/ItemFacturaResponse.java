package pe.edu.usmp.aduanatrack.clasificacion.dto;

import java.math.BigDecimal;

/**
 * Ítem de la factura comercial en GET y PUT /despachos/{id}/items (contrato C2 · T-07.2).
 * {@code numero} lo asigna el backend según el orden de la lista (1, 2, 3…). {@code partida}
 * repite los datos de la partida asignada para que la tabla la muestre sin otra consulta;
 * es nula mientras el ítem no tiene partida.
 */
public record ItemFacturaResponse(
        Long id,
        int numero,
        String descripcion,
        BigDecimal cantidad,
        String unidad,
        BigDecimal valorFob,
        Long partidaId,
        PartidaRef partida) {

    public record PartidaRef(Long id, String codigoFormateado, String descripcion) {
    }
}
