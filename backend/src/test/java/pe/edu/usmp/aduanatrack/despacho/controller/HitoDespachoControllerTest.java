package pe.edu.usmp.aduanatrack.despacho.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.dto.HitoResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(HitoDespachoController.class)
@Import(SeguridadWebTestConfig.class)
class HitoDespachoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private HitoDespachoService hitoService;

    @Test
    void hitos_respondeElContratoC5() throws Exception {
        when(hitoService.listar(1L)).thenReturn(List.of(
                new HitoResponse(TipoHito.APERTURA, "Despacho abierto", false, "Agente Demo",
                        LocalDateTime.of(2026, 10, 10, 20, 36)),
                new HitoResponse(TipoHito.HITO_SUGERIDO, "Programar inspección física", true, "Sistema",
                        LocalDateTime.of(2026, 10, 30, 9, 0))));

        mvc.perform(get("/api/v1/despachos/1/hitos").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("APERTURA"))
                .andExpect(jsonPath("$[0].descripcion").value("Despacho abierto"))
                .andExpect(jsonPath("$[0].sugerido").value(false))
                .andExpect(jsonPath("$[0].usuario").value("Agente Demo"))
                .andExpect(jsonPath("$[0].fecha").value("2026-10-10T20:36:00"))
                .andExpect(jsonPath("$[1].sugerido").value(true))
                .andExpect(jsonPath("$[1].usuario").value("Sistema"));
    }

    @Test
    void despachoInexistente_responde404() throws Exception {
        when(hitoService.listar(99L)).thenThrow(ApiException.noEncontrado("No existe el despacho solicitado."));

        mvc.perform(get("/api/v1/despachos/99/hitos").cookie(sesion("AGENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/despachos/1/hitos"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
