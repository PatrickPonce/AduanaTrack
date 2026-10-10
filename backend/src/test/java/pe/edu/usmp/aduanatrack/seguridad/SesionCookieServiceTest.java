package pe.edu.usmp.aduanatrack.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import pe.edu.usmp.aduanatrack.config.SeguridadProperties;

class SesionCookieServiceTest {

    private final SesionCookieService servicio = new SesionCookieService(
            new SeguridadProperties("secreto-de-pruebas-de-al-menos-32-caracteres", false));

    @Test
    void sinRecordarme_cookieDura7200Segundos() {
        ResponseCookie cookie = servicio.crear("token", SesionCookieService.duracion(false));

        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(7200);
    }

    @Test
    void conRecordarme_cookieDura28800Segundos() {
        ResponseCookie cookie = servicio.crear("token", SesionCookieService.duracion(true));

        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(28800);
    }

    @Test
    void cookieEsHttpOnlySameSiteStrictConPathRaiz() {
        ResponseCookie cookie = servicio.crear("token", SesionCookieService.duracion(false));

        assertThat(cookie.getName()).isEqualTo("AT_SESSION");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void logout_borraLaCookie() {
        ResponseCookie cookie = servicio.eliminar();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }
}
