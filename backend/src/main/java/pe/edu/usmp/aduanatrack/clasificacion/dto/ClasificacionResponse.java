package pe.edu.usmp.aduanatrack.clasificacion.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/** Respuesta de POST /despachos/{id}/clasificacion (contrato C2 · T-07.3): estado y quién clasificó. */
public record ClasificacionResponse(EstadoDespacho estado, String clasificadoPor, LocalDateTime clasificadoEn) {
}
