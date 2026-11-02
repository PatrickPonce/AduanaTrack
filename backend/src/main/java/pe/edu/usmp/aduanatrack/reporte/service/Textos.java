package pe.edu.usmp.aduanatrack.reporte.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Textos en español que comparten el Excel y el PDF del reporte de gestión. */
final class Textos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.of("es", "PE");

    private Textos() {
    }

    static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    /** {@code 2026-10} → «octubre 2026». */
    static String mes(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.FULL, ES) + " " + mes.getYear();
    }

    static String canal(String canal) {
        return switch (canal) {
            case "VERDE" -> "Canal verde";
            case "NARANJA" -> "Canal naranja";
            case "ROJO" -> "Canal rojo";
            default -> canal;
        };
    }

    /** Tiempo promedio con coma decimal, o un guion si ningún despacho del rango tiene levante. */
    static String dias(Double dias) {
        return dias == null ? "—" : String.format(ES, "%.1f", dias);
    }
}
