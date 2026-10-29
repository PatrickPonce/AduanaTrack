package pe.edu.usmp.aduanatrack.clasificacion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.clasificacion.dto.ClasificacionResponse;
import pe.edu.usmp.aduanatrack.clasificacion.service.ClasificacionService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(ClasificacionController.class)
@Import(SeguridadWebTestConfig.class)
class ClasificacionControllerTest {

    private static final String RUTA = "/api/v1/despachos/1/clasificacion";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ClasificacionService clasificacionService;

    @Test
    void guardar_respondeLaFormaDelContratoC2() throws Exception {
        when(clasificacionService.guardar(1L, 9L)).thenReturn(new ClasificacionResponse(EstadoDespacho.CLASIFICADO,
                "Agente Demo", LocalDateTime.of(2026, 10, 28, 10, 0)));

        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CLASIFICADO"))
                .andExpect(jsonPath("$.clasificadoPor").value("Agente Demo"))
                .andExpect(jsonPath("$.clasificadoEn").value("2026-10-28T10:00:00"));
    }

    @Test
    void itemsSinPartida_responde409ConLosItems() throws Exception {
        when(clasificacionService.guardar(1L, 9L)).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.ITEMS_SIN_PARTIDA, "Faltan ítems por clasificar: 1.", Map.of("items", List.of(2))));

        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ITEMS_SIN_PARTIDA"))
                .andExpect(jsonPath("$.items[0]").value(2));
    }

    @Test
    void documentacionIncompleta_responde409ConLosPendientes() throws Exception {
        when(clasificacionService.guardar(1L, 9L)).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.DOCUMENTACION_INCOMPLETA, "Faltan documentos obligatorios: Póliza de seguro.",
                Map.of("pendientes", List.of("POLIZA_SEGURO"))));

        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTACION_INCOMPLETA"))
                .andExpect(jsonPath("$.pendientes[0]").value("POLIZA_SEGURO"));
    }

    @Test
    void comoCliente_responde403() throws Exception {
        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(post(RUTA).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
