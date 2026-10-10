package pe.edu.usmp.aduanatrack.support;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import pe.edu.usmp.aduanatrack.config.SeguridadProperties;
import pe.edu.usmp.aduanatrack.seguridad.JwtCookieFilter;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SecurityConfig;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;

/** Seguridad real (cookie JWT + CSRF) para los @WebMvcTest de los controladores. */
@TestConfiguration
@Import({ SecurityConfig.class, JwtCookieFilter.class, JwtService.class, SesionCookieService.class })
public class SeguridadWebTestConfig {

    @Bean
    SeguridadProperties seguridadProperties() {
        return new SeguridadProperties("secreto-de-pruebas-de-al-menos-32-caracteres", false);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
