package pe.edu.usmp.aduanatrack.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.config.SeguridadProperties;

class JwtServiceTest {

    private static final SeguridadProperties PROPIEDADES =
            new SeguridadProperties("secreto-de-pruebas-de-al-menos-32-caracteres", false);
    private static final Instant INICIO = Instant.parse("2026-10-06T14:00:00Z");
    private static final UsuarioAutenticado USUARIO = new UsuarioAutenticado(1L, "admin", "Administrador", "ADMIN");

    @Test
    void tokenValido_devuelveElUsuario() {
        JwtService servicio = new JwtService(PROPIEDADES, Clock.fixed(INICIO, ZoneOffset.UTC));

        String token = servicio.generar(USUARIO, Duration.ofHours(2));

        assertThat(servicio.validar(token)).contains(USUARIO);
    }

    @Test
    void tokenExpirado_esRechazado() {
        String token = new JwtService(PROPIEDADES, Clock.fixed(INICIO, ZoneOffset.UTC))
                .generar(USUARIO, Duration.ofHours(2));
        JwtService despues = new JwtService(PROPIEDADES,
                Clock.fixed(INICIO.plus(Duration.ofHours(2)).plusSeconds(1), ZoneOffset.UTC));

        assertThat(despues.validar(token)).isEmpty();
    }

    @Test
    void tokenFirmadoConOtraClave_esRechazado() {
        Clock reloj = Clock.fixed(INICIO, ZoneOffset.UTC);
        String token = new JwtService(new SeguridadProperties("otra-clave-distinta-de-al-menos-32-caracteres", false),
                reloj).generar(USUARIO, Duration.ofHours(2));

        assertThat(new JwtService(PROPIEDADES, reloj).validar(token)).isEmpty();
    }

    @Test
    void tokenMalformado_esRechazado() {
        JwtService servicio = new JwtService(PROPIEDADES, Clock.fixed(INICIO, ZoneOffset.UTC));

        assertThat(servicio.validar("no-es-un-jwt")).isEmpty();
    }
}
