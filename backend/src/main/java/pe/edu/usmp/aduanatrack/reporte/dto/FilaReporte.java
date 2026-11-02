package pe.edu.usmp.aduanatrack.reporte.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/** Un despacho del rango tal como sale de la consulta del reporte (HU-15 · T-15.2). */
public record FilaReporte(String numero, String cliente, String aduana, EstadoDespacho estado, String canalControl,
        LocalDateTime creadoEn, LocalDate fechaLevante) {

    public LocalDate fechaApertura() {
        return creadoEn.toLocalDate();
    }
}
