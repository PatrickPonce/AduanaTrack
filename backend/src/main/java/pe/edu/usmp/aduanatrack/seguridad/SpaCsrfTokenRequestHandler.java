package pe.edu.usmp.aduanatrack.seguridad;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Manejo de CSRF para una SPA (patrón documentado por Spring Security 6): Axios lee la
 * cookie XSRF-TOKEN y la reenvía tal cual en la cabecera X-XSRF-TOKEN, por lo que el
 * valor de la cabecera se resuelve sin la máscara XOR; los formularios usan la máscara.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plano = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        // Fuerza la carga del token para que la cookie XSRF-TOKEN se envíe en cada respuesta.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String cabecera = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(cabecera) ? plano : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
