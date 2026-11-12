package pe.edu.usmp.aduanatrack.inspeccion.dto;

import java.util.List;

/**
 * Parte JSON {@code datos} de {@code POST /despachos/{id}/inspeccion} (contrato C7 · HU-12). El acta en PDF
 * viaja aparte, en la parte {@code acta}. El resultado no se envía: lo calcula el sistema (CA1).
 */
public record InspeccionRequest(List<Item> items, String observaciones) {

    public record Item(String descripcion, Boolean conforme, String observacion) {
    }
}
