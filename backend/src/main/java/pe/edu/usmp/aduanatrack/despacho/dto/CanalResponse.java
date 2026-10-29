package pe.edu.usmp.aduanatrack.despacho.dto;

import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/**
 * Respuesta de PUT /despachos/{id}/canal (contrato C5 · T-10.2). {@code hitoSugerido} es la
 * acción que el sistema propone al agente («Programar inspección física» en canal rojo) y es
 * nulo en los demás canales.
 */
public record CanalResponse(CanalControl canal, EstadoDespacho estado, boolean levanteHabilitado,
        String hitoSugerido) {
}
