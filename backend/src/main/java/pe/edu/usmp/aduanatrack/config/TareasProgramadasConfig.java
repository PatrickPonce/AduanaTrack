package pe.edu.usmp.aduanatrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas {@code @Scheduled} (alerta diaria de cartas poder, T-04.2). */
@Configuration
@EnableScheduling
public class TareasProgramadasConfig {
}
