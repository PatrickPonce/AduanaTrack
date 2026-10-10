package pe.edu.usmp.aduanatrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Carpeta donde se guardan los PDF de las cartas poder (volumen {@code cartas-poder} en Docker Compose). */
@ConfigurationProperties(prefix = "aduanatrack.cartas-poder")
public record CartaPoderProperties(@DefaultValue("/data/cartas-poder") String directorio) {
}
