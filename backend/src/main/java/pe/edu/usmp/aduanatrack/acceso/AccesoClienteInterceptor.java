package pe.edu.usmp.aduanatrack.acceso;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.usmp.aduanatrack.acceso.service.AccesoClienteService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * Aislamiento por empresa de todas las rutas de despacho y de cliente (HU-16). Toma el id de la
 * variable de ruta que Spring ya resolvió (no del texto de la URL, así que no se esquiva con
 * codificaciones) y, para un usuario CLIENTE, delega en {@link AccesoClienteService}. Cualquier
 * ruta nueva bajo {@code /api/v1/despachos/{id}/...} o {@code /api/v1/clientes/{id}/...} queda
 * protegida sin tocar su controlador.
 */
public class AccesoClienteInterceptor implements HandlerInterceptor {

    private static final Pattern RUTA_DESPACHO = Pattern.compile("^/api/v1/despachos/\\{(\\w+)}");
    private static final Pattern RUTA_CLIENTE = Pattern.compile("^/api/v1/clientes/\\{(\\w+)}");

    private final AccesoClienteService acceso;

    public AccesoClienteInterceptor(AccesoClienteService acceso) {
        this.acceso = acceso;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)
                || !AccesoClienteService.esCliente(usuario)) {
            return true;
        }
        String patron = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Map<String, String> variables = (Map<String, String>) request
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (patron == null || variables == null) {
            return true;
        }
        Matcher despacho = RUTA_DESPACHO.matcher(patron);
        if (despacho.find()) {
            Long id = aLong(variables.get(despacho.group(1)));
            if (id != null) {
                acceso.exigirDespacho(usuario, id);
            }
            return true;
        }
        Matcher cliente = RUTA_CLIENTE.matcher(patron);
        if (cliente.find()) {
            Long id = aLong(variables.get(cliente.group(1)));
            if (id != null) {
                acceso.exigirCliente(usuario, id);
            }
        }
        return true;
    }

    /** Un id que no es numérico lo rechaza el controlador con 400; aquí no hay nada que proteger. */
    private static Long aLong(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
