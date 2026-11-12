package pe.edu.usmp.aduanatrack.reporte.dto;

import java.util.List;
import java.util.Map;

/**
 * Respuesta de {@code GET /api/v1/reportes/indicadores} (contrato C10 · HU-15 · T-15.1). Son los mismos
 * indicadores que lleva el reporte exportado: {@code tiempoPromedioDias} es nulo si ningún despacho del rango
 * tiene levante, {@code despachosPorMes} va del mes más antiguo al más reciente (solo meses con despachos) y
 * {@code porCanal} siempre trae VERDE, NARANJA y ROJO.
 */
public record IndicadoresGestionResponse(long totalDespachos, long conLevante, Double tiempoPromedioDias,
        List<DespachosPorMes> despachosPorMes, Map<String, Long> porCanal) {

    /** {@code mes} en formato AAAA-MM. */
    public record DespachosPorMes(String mes, long cantidad) {
    }

    public static IndicadoresGestionResponse de(DatosReporte datos) {
        List<DespachosPorMes> porMes = datos.despachosPorMes().entrySet().stream()
                .map(e -> new DespachosPorMes(e.getKey().toString(), e.getValue())).toList();
        return new IndicadoresGestionResponse(datos.totalDespachos(), datos.conLevante(), datos.tiempoPromedioDias(),
                porMes, datos.porCanal());
    }
}
