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

    /**
     * Tiempo promedio con coma decimal, igual que la página «Reportes», o un guion si ningún despacho del rango
     * tiene levante. No se usa {@code ES}: en es-PE el separador decimal de Java es el punto.
     */
    static String dias(Double dias) {
        return dias == null ? "—" : String.format(Locale.ROOT, "%.1f", dias).replace('.', ',');
    }
}
