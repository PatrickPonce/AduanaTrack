package pe.edu.usmp.aduanatrack.reporte.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;
import pe.edu.usmp.aduanatrack.reporte.repository.ReporteExportRepository;

/**
 * Datos del reporte de gestión que se exporta (HU-15 · T-15.2): los despachos abiertos en el rango y sus
 * indicadores. El tiempo promedio es la cantidad de días entre la apertura y el levante, solo de los
 * despachos que ya tienen levante (CA1 de HU-15).
 */
@Service
public class DatosReporteService {

    /** Máximo del rango, en meses (regla de HU-15). */
    static final int MAX_MESES = 12;
    static final List<String> CANALES = List.of("VERDE", "NARANJA", "ROJO");

    private final ReporteExportRepository repositorio;

    public DatosReporteService(ReporteExportRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * 400 con el campo marcado si falta una fecha, no tiene el formato AAAA-MM-DD, {@code desde} es mayor
     * que {@code hasta} o el rango pasa de 12 meses.
     */
    @Transactional(readOnly = true)
    public DatosReporte generar(String desdeTexto, String hastaTexto) {
        LocalDate desde = fecha("desde", desdeTexto);
        LocalDate hasta = fecha("hasta", hastaTexto);
        if (desde.isAfter(hasta)) {
            throw ApiException.validacion(Map.of("desde", "«Desde» no puede ser posterior a «Hasta»"));
        }
        if (hasta.isAfter(desde.plusMonths(MAX_MESES))) {
            throw ApiException.validacion(Map.of("hasta", "El rango no puede pasar de " + MAX_MESES + " meses"));
        }
        List<FilaReporte> filas = repositorio.despachosEnRango(desde.atStartOfDay(),
                hasta.plusDays(1).atStartOfDay(), EstadoDespacho.BORRADOR);
        return calcular(desde, hasta, filas);
    }

    static DatosReporte calcular(LocalDate desde, LocalDate hasta, List<FilaReporte> filas) {
        long conLevante = filas.stream().filter(f -> f.fechaLevante() != null).count();
        OptionalDouble dias = filas.stream().filter(f -> f.fechaLevante() != null)
                .mapToLong(f -> ChronoUnit.DAYS.between(f.fechaApertura(), f.fechaLevante())).average();
        Double promedio = dias.isPresent() ? unDecimal(dias.getAsDouble()) : null;

        Map<YearMonth, Long> porMes = new TreeMap<>();
        for (FilaReporte f : filas) {
            porMes.merge(YearMonth.from(f.fechaApertura()), 1L, Long::sum);
        }
        Map<String, Long> porCanal = new LinkedHashMap<>();
        CANALES.forEach(canal -> porCanal.put(canal, 0L));
        long sinCanal = 0;
        for (FilaReporte f : filas) {
            String canal = f.canalControl() == null ? null : f.canalControl().toUpperCase();
            if (canal != null && porCanal.containsKey(canal)) {
                porCanal.merge(canal, 1L, Long::sum);
            } else {
                sinCanal++;
            }
        }
        return new DatosReporte(desde, hasta, filas.size(), conLevante, promedio, porMes, porCanal, sinCanal, filas);
    }

    private static double unDecimal(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static LocalDate fecha(String campo, String texto) {
        if (texto == null || texto.isBlank()) {
            throw ApiException.validacion(Map.of(campo, "Indique la fecha «" + etiqueta(campo) + "»"));
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.validacion(Map.of(campo, "Fecha no válida (use AAAA-MM-DD)"));
        }
    }

    private static String etiqueta(String campo) {
        return "desde".equals(campo) ? "Desde" : "Hasta";
    }
}
