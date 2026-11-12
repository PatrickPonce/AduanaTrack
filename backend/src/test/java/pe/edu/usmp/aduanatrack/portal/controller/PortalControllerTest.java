package pe.edu.usmp.aduanatrack.portal.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.portal.dto.MiEmpresaResponse;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;
import pe.edu.usmp.aduanatrack.portal.service.PortalService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(PortalController.class)
@Import(SeguridadWebTestConfig.class)
class PortalControllerTest {

    private static final UsuarioAutenticado CLIENTE = new UsuarioAutenticado(50L, "jperez", "Jorge Pérez", "CLIENTE");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PortalService servicio;

    @Test
    void miEmpresa_clienteVeSuRazonSocialYRuc() throws Exception {
        when(servicio.miEmpresa(CLIENTE))
                .thenReturn(new MiEmpresaResponse(1L, "20512345671", "Importadora Andina S.A.C."));

        mvc.perform(get("/api/v1/portal/mi-empresa").cookie(sesion(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Importadora Andina S.A.C."))
                .andExpect(jsonPath("$.ruc").value("20512345671"));
    }

    @Test
    void despachos_pasaLosFiltrosYDevuelveLasColumnasDeLaPantalla() throws Exception {
        when(servicio.despachos(CLIENTE, "0318", "INSPECCION_PROGRAMADA", "2026-10-01", "2026-11-30"))
                .thenReturn(List.of(new PortalDespachoResponse(318L, "DESP-2026-0318", "MSCU-BL-55120",
                        LocalDate.of(2026, 10, 21), EstadoDespacho.INSPECCION_PROGRAMADA, "NARANJA",
                        LocalDateTime.of(2026, 11, 5, 18, 40))));

        mvc.perform(get("/api/v1/portal/despachos").param("numero", "0318").param("estado", "INSPECCION_PROGRAMADA")
                .param("etaDesde", "2026-10-01").param("etaHasta", "2026-11-30").cookie(sesion(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("DESP-2026-0318"))
                .andExpect(jsonPath("$[0].numeroBl").value("MSCU-BL-55120"))
                .andExpect(jsonPath("$[0].fechaEta").value("2026-10-21"))
                .andExpect(jsonPath("$[0].estado").value("INSPECCION_PROGRAMADA"))
                .andExpect(jsonPath("$[0].canalControl").value("NARANJA"))
                .andExpect(jsonPath("$[0].ultimaActualizacion").value("2026-11-05T18:40:00"));
    }

    @Test
    void despachos_filtroInvalido_responde400() throws Exception {
        when(servicio.despachos(CLIENTE, null, "XX", null, null))
                .thenThrow(ApiException.validacion(Map.of("estado", "Estado no válido")));

        mvc.perform(get("/api/v1/portal/despachos").param("estado", "XX").cookie(sesion(CLIENTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").value("Estado no válido"));
    }

    @Test
    void adminYAgente_recibenAcceso403() throws Exception {
        for (String rol : List.of("ADMIN", "AGENTE")) {
            UsuarioAutenticado u = new UsuarioAutenticado(1L, rol.toLowerCase(), rol, rol);
            mvc.perform(get("/api/v1/portal/despachos").cookie(sesion(u))).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/portal/mi-empresa").cookie(sesion(u))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/portal/despachos")).andExpect(status().isUnauthorized());
    }

    private Cookie sesion(UsuarioAutenticado usuario) {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(usuario, Duration.ofHours(2)));
    }
}
