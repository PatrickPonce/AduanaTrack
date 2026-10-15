package pe.edu.usmp.aduanatrack.seguridad;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.common.ErrorResponse;

@Configuration
public class SecurityConfig {

    public static final String RUTA_LOGIN = "/api/v1/auth/login";
    private static final String[] RUTAS_REGISTRO = { "/api/v1/clientes/**", "/api/v1/despachos/**" };
    private static final String[] ROLES_REGISTRO = { "ADMIN", "AGENTE" };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtCookieFilter jwtCookieFilter,
            ObjectMapper objectMapper) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF activo (cookie XSRF-TOKEN legible por Axios); solo se exceptúa el login,
                // que aún no tiene sesión que proteger.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(RUTA_LOGIN))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, RUTA_LOGIN).permitAll()
                        .requestMatchers("/error").permitAll()
                        // HU-03, HU-04 y HU-05: solo el administrador y el agente de aduana registran
                        // clientes, cartas poder, despachos y embarques; el perfil Cliente solo consulta.
                        .requestMatchers(HttpMethod.POST, RUTAS_REGISTRO).hasAnyRole(ROLES_REGISTRO)
                        .requestMatchers(HttpMethod.PUT, RUTAS_REGISTRO).hasAnyRole(ROLES_REGISTRO)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> escribirError(response, objectMapper,
                                HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, "Debe iniciar sesión."))
                        .accessDeniedHandler((request, response, e) -> escribirError(response, objectMapper,
                                HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO, "Acceso denegado.")))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** El filtro solo debe correr dentro de la cadena de Spring Security, no como filtro de servlet. */
    @Bean
    public FilterRegistrationBean<JwtCookieFilter> jwtCookieFilterRegistro(JwtCookieFilter filtro) {
        FilterRegistrationBean<JwtCookieFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void escribirError(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
            String codigo, String mensaje) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(codigo, mensaje));
    }
}
