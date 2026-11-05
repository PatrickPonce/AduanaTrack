package pe.edu.usmp.aduanatrack.notificacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.CantidadNoLeidasResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.NotificacionResponse;
import pe.edu.usmp.aduanatrack.notificacion.dto.PreferenciasResponse;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest({ NotificacionController.class, PreferenciasController.class })
@Import(SeguridadWebTestConfig.class)
class NotificacionControllerTest {

    private static final String RUTA = "/api/v1/notificaciones";
    private static final String RUTA_PREFERENCIAS = "/api/v1/usuarios/me/preferencias";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificacionService notificacionService;

    private static NotificacionResponse noLeida() {
        return new NotificacionResponse(1L, TipoNotificacion.INSPECCION_CON_OBSERVACIONES, Severidad.ALTA,
                "Inspección con observaciones", "DESP-2026-0001: rotulado incompleto", 5L, "DESP-2026-0001", false,
                LocalDateTime.of(2026, 11, 6, 9, 0), null);
    }

    @Test
    void listar_respondeLaPaginaDelUsuarioDeLaSesionConLaFormaDelContratoC9() throws Exception {
        when(notificacionService.listar(9L, "NO_LEIDAS", 0, 20))
                .thenReturn(new PaginaResponse<>(List.of(noLeida()), 0, 20, 1, 1));

        mvc.perform(get(RUTA).param("estado", "NO_LEIDAS").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(1))
                .andExpect(jsonPath("$.contenido[0].tipo").value("INSPECCION_CON_OBSERVACIONES"))
                .andExpect(jsonPath("$.contenido[0].severidad").value("ALTA"))
                .andExpect(jsonPath("$.contenido[0].despachoId").value(5))
                .andExpect(jsonPath("$.contenido[0].numeroDespacho").value("DESP-2026-0001"))
                .andExpect(jsonPath("$.contenido[0].leida").value(false))
                .andExpect(jsonPath("$.contenido[0].creadaEn").value("2026-11-06T09:00:00"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listarConPagina_pasaLaPaginaYElTamano() throws Exception {
        when(notificacionService.listar(9L, null, 2, 5)).thenReturn(new PaginaResponse<>(List.of(), 2, 5, 0, 0));

        mvc.perform(get(RUTA).param("pagina", "2").param("tamano", "5").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2));
    }

    @Test
    void cantidadNoLeidas_respondeElContador() throws Exception {
        when(notificacionService.cantidadNoLeidas(9L)).thenReturn(new CantidadNoLeidasResponse(3));

        mvc.perform(get(RUTA + "/no-leidas/cantidad").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(3));
    }

    // CA4: marcar como leída

    @Test
    void marcarLeida_conCsrf_responde200() throws Exception {
        NotificacionResponse leida = new NotificacionResponse(1L, TipoNotificacion.INSPECCION_CON_OBSERVACIONES,
                Severidad.ALTA, "Inspección con observaciones", "Rotulado incompleto", 5L, "DESP-2026-0001", true,
                LocalDateTime.of(2026, 11, 6, 9, 0), LocalDateTime.of(2026, 11, 6, 10, 0));
        when(notificacionService.marcarLeida(1L, 9L)).thenReturn(leida);

        mvc.perform(patch(RUTA + "/1/leida").with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true))
                .andExpect(jsonPath("$.leidaEn").value("2026-11-06T10:00:00"));
    }

    @Test
    void marcarLeidaSinCsrf_responde403() throws Exception {
        mvc.perform(patch(RUTA + "/1/leida").cookie(sesion("AGENTE")))
                .andExpect(status().isForbidden());

        verify(notificacionService, never()).marcarLeida(any(), any());
    }

    @Test
    void marcarLeidaDeOtroUsuario_responde404() throws Exception {
        when(notificacionService.marcarLeida(1L, 9L))
                .thenThrow(ApiException.noEncontrado("No existe la notificación solicitada."));

        mvc.perform(patch(RUTA + "/1/leida").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get(RUTA + "/no-leidas/cantidad")).andExpect(status().isUnauthorized());
    }

    @Test
    void preferencias_respondeElCanal() throws Exception {
        when(notificacionService.preferencias(9L)).thenReturn(new PreferenciasResponse(CanalNotificacion.SISTEMA));

        mvc.perform(get(RUTA_PREFERENCIAS).cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalNotificacion").value("SISTEMA"));
    }

    @Test
    void actualizarPreferenciasComoCliente_responde200() throws Exception {
        when(notificacionService.actualizarPreferencias(9L, CanalNotificacion.AMBOS))
                .thenReturn(new PreferenciasResponse(CanalNotificacion.AMBOS));

        mvc.perform(put(RUTA_PREFERENCIAS).with(csrf()).cookie(sesion("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"canalNotificacion\":\"AMBOS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalNotificacion").value("AMBOS"));
    }

    @Test
    void actualizarPreferenciasSinCanal_responde400ConElCampo() throws Exception {
        mvc.perform(put(RUTA_PREFERENCIAS).with(csrf()).cookie(sesion("AGENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.canalNotificacion").exists());

        verify(notificacionService, never()).actualizarPreferencias(any(), any());
    }

    @Test
    void actualizarPreferenciasConCanalDesconocido_responde400() throws Exception {
        mvc.perform(put(RUTA_PREFERENCIAS).with(csrf()).cookie(sesion("AGENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"canalNotificacion\":\"PALOMA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION));

        verify(notificacionService, never()).actualizarPreferencias(eq(9L), any());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
