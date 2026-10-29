package pe.edu.usmp.aduanatrack.clasificacion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.clasificacion.dto.PartidaResponse;
import pe.edu.usmp.aduanatrack.clasificacion.service.PartidaService;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(PartidaController.class)
@Import(SeguridadWebTestConfig.class)
class PartidaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PartidaService partidaService;

    @Test
    void buscar_respondeLaFormaDelContratoC2() throws Exception {
        when(partidaService.buscar("rodam")).thenReturn(List.of(new PartidaResponse(1L, "8482100000",
                "8482.10.00.00", "Rodamientos de bolas", new BigDecimal("6.00"))));

        mvc.perform(get("/api/v1/catalogos/partidas").param("q", "rodam").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigo").value("8482100000"))
                .andExpect(jsonPath("$[0].codigoFormateado").value("8482.10.00.00"))
                .andExpect(jsonPath("$[0].descripcion").value("Rodamientos de bolas"))
                .andExpect(jsonPath("$[0].tasaAdValorem").value(6.00));
    }

    @Test
    void buscar_sinParametroConsultaConTextoVacio() throws Exception {
        when(partidaService.buscar("")).thenReturn(List.of());

        mvc.perform(get("/api/v1/catalogos/partidas").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscar_loPuedenConsultarLosTresPerfiles() throws Exception {
        when(partidaService.buscar("motor")).thenReturn(List.of());

        for (String rol : List.of("ADMIN", "AGENTE", "CLIENTE")) {
            mvc.perform(get("/api/v1/catalogos/partidas").param("q", "motor").cookie(sesion(rol)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/catalogos/partidas").param("q", "motor"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTENTICADO));
    }

    private Cookie sesion(String rol) {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(
                new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2)));
    }
}
