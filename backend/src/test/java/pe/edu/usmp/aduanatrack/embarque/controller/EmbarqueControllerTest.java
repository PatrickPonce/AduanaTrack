package pe.edu.usmp.aduanatrack.embarque.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.embarque.service.EmbarqueService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(EmbarqueController.class)
@Import(SeguridadWebTestConfig.class)
class EmbarqueControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private EmbarqueService embarqueService;

    @Test
    void sinNumeroDeBl_responde400() throws Exception {
        guardar("""
                {"tipoBl":"HOUSE","naviera":"Maersk","puertoEmbarque":"Shanghai","fechaEta":"2026-10-20",
                 "contenedores":[{"numero":"CSQU3054383"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.numeroBl").value("Ingrese el N° de BL"));
    }

    @Test
    void sinContenedores_responde400() throws Exception {
        guardar("""
                {"numeroBl":"HBL-1","tipoBl":"HOUSE","naviera":"Maersk","puertoEmbarque":"Shanghai",
                 "fechaEta":"2026-10-20","contenedores":[]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contenedores").value("Registre al menos un contenedor"));
    }

    @Test
    void contenedorMalFormado_indicaLaFilaConError() throws Exception {
        guardar("""
                {"numeroBl":"HBL-1","tipoBl":"HOUSE","naviera":"Maersk","puertoEmbarque":"Shanghai",
                 "fechaEta":"2026-10-20","contenedores":[{"numero":"CSQU3054383"},{"numero":"CSQ3054383"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['contenedores[1].numero']").exists());
    }

    private org.springframework.test.web.servlet.ResultActions guardar(String json) throws Exception {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "agente", "Agente", "AGENTE"),
                Duration.ofHours(2));
        return mvc.perform(put("/api/v1/despachos/1/embarque").with(csrf())
                .cookie(new Cookie(SesionCookieService.NOMBRE_COOKIE, token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
