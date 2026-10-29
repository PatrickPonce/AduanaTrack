package pe.edu.usmp.aduanatrack.despacho.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.dto.CanalResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.service.CanalControlService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(CanalControlController.class)
@Import(SeguridadWebTestConfig.class)
class CanalControlControllerTest {

    private static final String RUTA = "/api/v1/despachos/1/canal";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CanalControlService canalService;

    @Test
    void canalRojo_respondeLaFormaDelContratoC5() throws Exception {
        when(canalService.guardar(1L, CanalControl.ROJO)).thenReturn(new CanalResponse(CanalControl.ROJO,
                EstadoDespacho.CANAL_ASIGNADO, false, "Programar inspección física"));

        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"ROJO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value("ROJO"))
                .andExpect(jsonPath("$.estado").value("CANAL_ASIGNADO"))
                .andExpect(jsonPath("$.levanteHabilitado").value(false))
                .andExpect(jsonPath("$.hitoSugerido").value("Programar inspección física"));
    }

    @Test
    void canalVerde_respondeLevanteHabilitadoSinHitoSugerido() throws Exception {
        when(canalService.guardar(1L, CanalControl.VERDE)).thenReturn(new CanalResponse(CanalControl.VERDE,
                EstadoDespacho.CANAL_ASIGNADO, true, null));

        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"VERDE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levanteHabilitado").value(true))
                .andExpect(jsonPath("$.hitoSugerido").doesNotExist());
    }

    @Test
    void sinDamNumerada_responde409() throws Exception {
        when(canalService.guardar(1L, CanalControl.NARANJA)).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.DAM_NO_NUMERADA, "Registre la numeración de la DAM antes de asignar el canal."));

        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"NARANJA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DAM_NO_NUMERADA"));
    }

    @Test
    void canalInvalido_responde400ConElCampo() throws Exception {
        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"AZUL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.canal").exists());

        verify(canalService, never()).guardar(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void sinCanal_responde400() throws Exception {
        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.canal").exists());
    }

    @Test
    void comoCliente_responde403() throws Exception {
        mvc.perform(put(RUTA).with(csrf()).cookie(sesion("CLIENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"VERDE\"}"))
                .andExpect(status().isForbidden());

        verify(canalService, never()).guardar(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void sinTokenCsrf_responde403() throws Exception {
        mvc.perform(put(RUTA).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"canal\":\"VERDE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(put(RUTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"canal\":\"VERDE\"}"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
