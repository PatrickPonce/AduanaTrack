package pe.edu.usmp.aduanatrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Carpeta de los documentos del despacho (HU-06, volumen {@code documentos} en Docker Compose). */
@ConfigurationProperties(prefix = "aduanatrack.documentos")
public record DocumentoProperties(@DefaultValue("/data/documentos") String directorio) {
}
