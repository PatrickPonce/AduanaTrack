package pe.edu.usmp.aduanatrack.seguridad;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import pe.edu.usmp.aduanatrack.config.SeguridadProperties;

/** Construye la cookie de sesión AT_SESSION (HttpOnly, SameSite=Strict). */
@Component
public class SesionCookieService {

    public static final String NOMBRE_COOKIE = "AT_SESSION";
    public static final Duration DURACION_NORMAL = Duration.ofHours(2);
    public static final Duration DURACION_RECORDARME = Duration.ofHours(8);

    private final boolean secure;

    public SesionCookieService(SeguridadProperties propiedades) {
        this.secure = propiedades.cookieSecure();
    }

    public static Duration duracion(boolean recordarme) {
        return recordarme ? DURACION_RECORDARME : DURACION_NORMAL;
    }

    public ResponseCookie crear(String token, Duration duracion) {
        return base(token).maxAge(duracion).build();
    }

    public ResponseCookie eliminar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE_COOKIE, valor)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/");
    }
}
