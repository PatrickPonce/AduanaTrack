package pe.edu.usmp.aduanatrack.acceso;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import pe.edu.usmp.aduanatrack.acceso.service.AccesoClienteService;

/** Registra el aislamiento por empresa (HU-16) en las rutas de despacho y de cliente. */
@Configuration
public class AccesoWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<AccesoClienteService> acceso;

    public AccesoWebConfig(ObjectProvider<AccesoClienteService> acceso) {
        this.acceso = acceso;
    }

    /**
     * {@code ifAvailable}: las pruebas de un controlador (@WebMvcTest) que no necesitan el aislamiento
     * no cargan el servicio; en la aplicación real siempre existe.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        acceso.ifAvailable(servicio -> registry.addInterceptor(new AccesoClienteInterceptor(servicio))
                .addPathPatterns("/api/v1/despachos/**", "/api/v1/clientes/**"));
    }
}
