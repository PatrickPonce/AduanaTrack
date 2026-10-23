package pe.edu.usmp.aduanatrack.despacho.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.despacho.entity.HitoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;

/** Fila de la «Línea de tiempo del despacho» (contrato C5, GET /despachos/{id}/hitos). */
public record HitoResponse(TipoHito tipo, String descripcion, boolean sugerido, String usuario,
        LocalDateTime fecha) {

    /** Nombre que se muestra cuando el hito no tiene usuario. */
    public static final String SISTEMA = "Sistema";

    public static HitoResponse de(HitoDespacho h) {
        return new HitoResponse(h.getTipo(), h.getDescripcion(), h.isSugerido(),
                h.getUsuario() == null ? SISTEMA : h.getUsuario().getNombre(), h.getFecha());
    }
}
