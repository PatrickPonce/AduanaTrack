package pe.edu.usmp.aduanatrack.portal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/** Fila de «Mis despachos». {@code ultimaActualizacion} es la fecha del último hito del despacho. */
public record PortalDespachoResponse(
        Long id,
        String numero,
        String numeroBl,
        LocalDate fechaEta,
        EstadoDespacho estado,
        String canalControl,
        LocalDateTime ultimaActualizacion) {
}
