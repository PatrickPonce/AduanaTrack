package pe.edu.usmp.aduanatrack.levante.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/**
 * Levante del despacho (contrato C8). {@code numeroLevante}, {@code fechaLevante} y
 * {@code constanciaNombre} son nulos mientras no se registra el levante; {@code retiroHabilitado} es
 * {@code true} desde que el despacho llegó a LEVANTE_OTORGADO (ya se puede registrar la cita de retiro).
 */
public record LevanteResponse(String numeroLevante, LocalDate fechaLevante, String constanciaNombre,
        boolean conAdvertencia, boolean retiroHabilitado, EstadoDespacho estadoDespacho) {

    public static LevanteResponse de(Despacho despacho) {
        return new LevanteResponse(despacho.getNumeroLevante(), despacho.getFechaLevante(),
                despacho.getConstanciaLevanteNombre(), despacho.isLevanteConAdvertencia(),
                despacho.getEstado().alcanzo(EstadoDespacho.LEVANTE_OTORGADO), despacho.getEstado());
    }
}
