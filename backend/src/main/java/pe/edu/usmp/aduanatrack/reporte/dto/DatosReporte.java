package pe.edu.usmp.aduanatrack.reporte.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Lo que llevan el Excel y el PDF del reporte de gestión (HU-15 · T-15.2): los indicadores del rango y la
 * tabla de despachos. {@code tiempoPromedioDias} es nulo si ningún despacho del rango tiene levante.
 */
public record DatosReporte(LocalDate desde, LocalDate hasta, long totalDespachos, long conLevante,
        Double tiempoPromedioDias, Map<YearMonth, Long> despachosPorMes, Map<String, Long> porCanal, long sinCanal,
        List<FilaReporte> despachos) {
}
