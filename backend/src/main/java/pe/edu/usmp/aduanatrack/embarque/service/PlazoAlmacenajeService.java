package pe.edu.usmp.aduanatrack.embarque.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.config.service.ParametroService;

/**
 * Plazo de almacenaje (HU-05 · T-05.3): fecha límite = ETA + DIAS_ALMACENAJE días calendario;
 * alerta si quedan DIAS_ALERTA_ALMACENAJE días o menos. Ambos valores vienen de parametro_sistema.
 */
@Service
public class PlazoAlmacenajeService {

    private final ParametroService parametros;
    private final Clock clock;

    public PlazoAlmacenajeService(ParametroService parametros, Clock clock) {
        this.parametros = parametros;
        this.clock = clock;
    }

    public LocalDate fechaLimite(LocalDate fechaEta) {
        return fechaEta.plusDays(parametros.entero(ParametroService.DIAS_ALMACENAJE));
    }

    /** Días que faltan para la fecha límite (negativo si ya venció) y si corresponde la alerta. */
    public Plazo plazo(LocalDate fechaLimite) {
        return plazo(fechaLimite, LocalDate.now(clock), parametros.entero(ParametroService.DIAS_ALERTA_ALMACENAJE));
    }

    public static Plazo plazo(LocalDate fechaLimite, LocalDate hoy, int diasAlerta) {
        if (fechaLimite == null) {
            return new Plazo(null, false);
        }
        long dias = ChronoUnit.DAYS.between(hoy, fechaLimite);
        return new Plazo(dias, dias <= diasAlerta);
    }

    public record Plazo(Long diasRestantes, boolean alerta) {
    }
}
