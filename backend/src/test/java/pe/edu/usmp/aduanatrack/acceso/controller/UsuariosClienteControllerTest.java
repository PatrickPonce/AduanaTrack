package pe.edu.usmp.aduanatrack.acceso.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.EmpresaAsignableResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.UsuarioClienteResponse;
import pe.edu.usmp.aduanatrack.acceso.service.UsuariosClienteService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(UsuariosClienteController.class)
@Import(SeguridadWebTestConfig.class)
class UsuariosClienteControllerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 5, 20, 15);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuariosClienteService servicio;

    @Test
    void listar_adminVeLosUsuariosClienteConSuEmpresa() throws Exception {
        when(servicio.listar()).thenReturn(List.of(
                new UsuarioClienteResponse(5L, "jperez", "jperez@andina.pe", "Jorge Pérez", true, 1L, "20512345671",
                        "Importadora Andina S.A.C.", FECHA, "ASIGNADO"),
                new UsuarioClienteResponse(6L, "cliente", "cliente@aduanatrack.pe", "Cliente Demo", true, null, null,
                        null, null, "SIN_ASIGNAR")));

        mvc.perform(get("/api/v1/administracion/usuarios-cliente").cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].razonSocial").value("Importadora Andina S.A.C."))
                .andExpect(jsonPath("$[0].estado").value("ASIGNADO"))
                .andExpect(jsonPath("$[1].estado").value("SIN_ASIGNAR"))
                .andExpect(jsonPath("$[1].ruc").doesNotExist());
    }

    @Test
    void empresasAsignables_adminLasLista() throws Exception {
        when(servicio.empresasAsignables())
                .thenReturn(List.of(new EmpresaAsignableResponse(1L, "20512345671", "Importadora Andina S.A.C.")));

        mvc.perform(get("/api/v1/administracion/empresas-asignables").cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruc").value("20512345671"));
    }

    @Test
    void asignar_adminAsignaLaEmpresaYResponde200() throws Exception {
        when(servicio.asignar(6L, 1L, 1L)).thenReturn(new UsuarioClienteResponse(6L, "cliente",
                "cliente@aduanatrack.pe", "Cliente Demo", true, 1L, "20512345671", "Importadora Andina S.A.C.", FECHA,
                "ASIGNADO"));

        mvc.perform(put("/api/v1/administracion/usuarios-cliente/6/empresa").with(csrf()).cookie(sesion("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"));
        verify(servicio).asignar(6L, 1L, 1L);
    }

    @Test
    void asignar_sinClienteId_responde400ConElCampo() throws Exception {
        mvc.perform(put("/api/v1/administracion/usuarios-cliente/6/empresa").with(csrf()).cookie(sesion("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campos.clienteId").value("Campo obligatorio"));
        verify(servicio, never()).asignar(any(), any(), any());
    }

    @Test
    void asignar_rucInvalido_propagaElError400DelServicio() throws Exception {
        when(servicio.asignar(6L, 2L, 1L))
                .thenThrow(ApiException.validacion(Map.of("clienteId", "Solo se pueden asignar empresas con RUC válido")));

        mvc.perform(put("/api/v1/administracion/usuarios-cliente/6/empresa").with(csrf()).cookie(sesion("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.clienteId").value("Solo se pueden asignar empresas con RUC válido"));
    }

    @Test
    void accesosDenegados_adminVeLosIntentosConSuPaginacion() throws Exception {
        when(servicio.accesosDenegados(0, 20)).thenReturn(new PaginaResponse<>(List.of(new AccesoDenegadoResponse(1L,
                FECHA, "jperez", 20L, "DESP-2026-0342", 2L, "Pacífico Trade E.I.R.L.",
                MotivoAccesoDenegado.OTRA_EMPRESA)), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/administracion/accesos-denegados").cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].usuario").value("jperez"))
                .andExpect(jsonPath("$.contenido[0].numeroDespacho").value("DESP-2026-0342"))
                .andExpect(jsonPath("$.contenido[0].empresaDespacho").value("Pacífico Trade E.I.R.L."))
                .andExpect(jsonPath("$.contenido[0].resultado").value("OTRA_EMPRESA"))
                .andExpect(jsonPath("$.contenido[0].mensaje").value("Despacho no encontrado"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void agenteYCliente_recibenAcceso403EnTodasLasRutas() throws Exception {
        for (String rol : List.of("AGENTE", "CLIENTE")) {
            mvc.perform(get("/api/v1/administracion/usuarios-cliente").cookie(sesion(rol)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
            mvc.perform(get("/api/v1/administracion/empresas-asignables").cookie(sesion(rol)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/administracion/accesos-denegados").cookie(sesion(rol)))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/administracion/usuarios-cliente/6/empresa").with(csrf()).cookie(sesion(rol))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":1}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/administracion/usuarios-cliente")).andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        long id = "ADMIN".equals(rol) ? 1L : 9L;
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(
                new UsuarioAutenticado(id, rol.toLowerCase(), rol, rol), Duration.ofHours(2)));
    }
}
