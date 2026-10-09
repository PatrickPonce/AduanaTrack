package pe.edu.usmp.aduanatrack.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación (hora de Lima). Se inyecta en los servicios para que
 * las reglas con fechas (bloqueo de cuenta, plazos, vencimientos) se puedan probar.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${aduanatrack.zona-horaria:America/Lima}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
