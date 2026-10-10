package pe.edu.usmp.aduanatrack.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.auth.dto.LoginRequest;
import pe.edu.usmp.aduanatrack.auth.service.AuthService;
import pe.edu.usmp.aduanatrack.auth.service.SesionIniciada;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.SeguridadProperties;
import pe.edu.usmp.aduanatrack.seguridad.JwtCookieFilter;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SecurityConfig;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtCookieFilter.class, JwtService.class, SesionCookieService.class,
        AuthControllerTest.Config.class })
class AuthControllerTest {

    private static final UsuarioAutenticado AGENTE = new UsuarioAutenticado(7L, "agente", "Agente Demo", "AGENTE");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @TestConfiguration
    static class Config {
        @Bean
        SeguridadProperties seguridadProperties() {
            return new SeguridadProperties("secreto-de-pruebas-de-al-menos-32-caracteres", false);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.now(), ZoneOffset.UTC);
        }
    }

    @Test
    void login_esPublicoSinCsrfYDevuelveCookieHttpOnly() throws Exception {
        when(authService.login(any(LoginRequest.class), anyString(), any()))
                .thenReturn(new SesionIniciada(AGENTE, "jwt", Duration.ofHours(8)));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"agente\",\"password\":\"x\",\"recordarme\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("agente"))
                .andExpect(jsonPath("$.rol").value("AGENTE"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("AT_SESSION=jwt")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=28800")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void login_camposVacios_responde400ConCampos() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.usuario").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void login_cuentaBloqueada_responde423ConFormatoUnico() throws Exception {
        when(authService.login(any(LoginRequest.class), anyString(), any())).thenThrow(new ApiException(
                HttpStatus.LOCKED, CodigoError.CUENTA_BLOQUEADA, "Cuenta bloqueada temporalmente."));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"agente\",\"password\":\"x\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.codigo").value(CodigoError.CUENTA_BLOQUEADA));
    }

    @Test
    void me_sinCookie_responde401NoAutenticado() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTENTICADO));
    }

    @Test
    void me_conCookieValida_devuelveElUsuario() throws Exception {
        String token = jwtService.generar(AGENTE, Duration.ofHours(2));

        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie(SesionCookieService.NOMBRE_COOKIE, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("agente"))
                .andExpect(jsonPath("$.nombre").value("Agente Demo"))
                .andExpect(jsonPath("$.rol").value("AGENTE"));
    }

    @Test
    void logout_sinTokenCsrf_esRechazado() throws Exception {
        String token = jwtService.generar(AGENTE, Duration.ofHours(2));

        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(SesionCookieService.NOMBRE_COOKIE, token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_conCsrf_responde204YBorraLaCookie() throws Exception {
        String token = jwtService.generar(AGENTE, Duration.ofHours(2));

        mvc.perform(post("/api/v1/auth/logout").with(csrf())
                .cookie(new Cookie(SesionCookieService.NOMBRE_COOKIE, token)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }
}
