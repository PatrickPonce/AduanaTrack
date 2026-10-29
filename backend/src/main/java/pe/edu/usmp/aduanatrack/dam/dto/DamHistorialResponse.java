package pe.edu.usmp.aduanatrack.dam.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.dam.entity.DamHistorial;

/** Una rectificación del N° de DAM (contrato C4, GET {@code /dam/historial}). */
public record DamHistorialResponse(String numeroAnterior, LocalDate fechaAnterior, String numeroNuevo,
        LocalDate fechaNueva, String motivo, String usuario, LocalDateTime fecha) {

    public static DamHistorialResponse de(DamHistorial h) {
        return new DamHistorialResponse(h.getNumeroAnterior(), h.getFechaAnterior(), h.getNumeroNuevo(),
                h.getFechaNueva(), h.getMotivo(), h.getRectificadoPor().getNombre(), h.getRectificadoEn());
    }
}
