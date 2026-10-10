package pe.edu.usmp.aduanatrack.catalogo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.catalogo.dto.AduanaResponse;
import pe.edu.usmp.aduanatrack.catalogo.dto.RegimenResponse;
import pe.edu.usmp.aduanatrack.catalogo.service.CatalogoService;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(CatalogoController.class)
@Import(SeguridadWebTestConfig.class)
class CatalogoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CatalogoService catalogoService;

    @Test
    void regimenes_respondeIdCodigoYDescripcion() throws Exception {
        when(catalogoService.regimenes()).thenReturn(List.of(
                new RegimenResponse(1L, "10", "Importación para el consumo"),
                new RegimenResponse(4L, "70", "Depósito aduanero")));

        mvc.perform(get("/api/v1/catalogos/regimenes").cookie(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigo").value("10"))
                .andExpect(jsonPath("$[0].descripcion").value("Importación para el consumo"))
                .andExpect(jsonPath("$[1].codigo").value("70"));
    }

    @Test
    void aduanas_respondeIdCodigoYNombre() throws Exception {
        when(catalogoService.aduanas()).thenReturn(List.of(
                new AduanaResponse(2L, "046", "Paita"),
                new AduanaResponse(1L, "118", "Marítima del Callao")));

        mvc.perform(get("/api/v1/catalogos/aduanas").cookie(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].codigo").value("046"))
                .andExpect(jsonPath("$[0].nombre").value("Paita"))
                .andExpect(jsonPath("$[1].codigo").value("118"));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/catalogos/aduanas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_AUTENTICADO));
    }

    private Cookie sesion() {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(
                new UsuarioAutenticado(9L, "agente", "Agente", "AGENTE"), Duration.ofHours(2)));
    }
}
