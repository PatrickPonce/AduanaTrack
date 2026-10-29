package pe.edu.usmp.aduanatrack.dam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.dam.dto.DamHistorialResponse;
import pe.edu.usmp.aduanatrack.dam.dto.DamResponse;
import pe.edu.usmp.aduanatrack.dam.service.NumeracionDamService;
import pe.edu.usmp.aduanatrack.dam.service.NumeracionDamService.ArchivoCargo;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(NumeracionDamController.class)
@Import(SeguridadWebTestConfig.class)
class NumeracionDamControllerTest {

    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NumeracionDamService damService;

    @TempDir
    Path directorio;

    @Test
    void registrarComoAgente_responde201ConElContrato() throws Exception {
        when(damService.registrar(eq(1L), eq("118-2026-10-123456"), eq("2026-10-27"), any()))
                .thenReturn(new DamResponse("118-2026-10-123456", LocalDate.of(2026, 10, 27), "cargo.pdf",
                        "DAM_NUMERADA"));

        mvc.perform(multipart("/api/v1/despachos/1/dam")
                .file(new MockMultipartFile("cargo", "cargo.pdf", "application/pdf", PDF))
                .param("numeroDam", "118-2026-10-123456").param("fechaNumeracion", "2026-10-27")
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDam").value("118-2026-10-123456"))
                .andExpect(jsonPath("$.fechaNumeracion").value("2026-10-27"))
                .andExpect(jsonPath("$.nombreCargo").value("cargo.pdf"))
                .andExpect(jsonPath("$.estado").value("DAM_NUMERADA"));
    }

    @Test
    void registrarSinNumero_respondeElErrorConElCampoMarcado() throws Exception {
        when(damService.registrar(eq(1L), any(), any(), any())).thenThrow(
                ApiException.validacion(java.util.Map.of("numeroDam", "Ingrese el N° de DAM")));

        mvc.perform(multipart("/api/v1/despachos/1/dam").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.numeroDam").value("Ingrese el N° de DAM"));
    }

    @Test
    void registrarSinTributosPagados_responde409() throws Exception {
        when(damService.registrar(eq(1L), any(), any(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.TRIBUTOS_PENDIENTES, "Registre el pago de tributos antes de numerar la DAM."));

        mvc.perform(multipart("/api/v1/despachos/1/dam").param("numeroDam", "118-2026-10-123456")
                .with(csrf()).cookie(sesion("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.TRIBUTOS_PENDIENTES));
    }

    @Test
    void registrarComoCliente_responde403() throws Exception {
        mvc.perform(multipart("/api/v1/despachos/1/dam").param("numeroDam", "118-2026-10-123456")
                .with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.ACCESO_DENEGADO));
        verify(damService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void registrarSinTokenCsrf_responde403() throws Exception {
        mvc.perform(multipart("/api/v1/despachos/1/dam").param("numeroDam", "118-2026-10-123456")
                .cookie(sesion("AGENTE")))
                .andExpect(status().isForbidden());
        verify(damService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void obtenerComoCliente_respondeLaNumeracion() throws Exception {
        when(damService.obtener(1L)).thenReturn(new DamResponse(null, null, null, "TRIBUTOS_PAGADOS"));

        mvc.perform(get("/api/v1/despachos/1/dam").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDam").doesNotExist())
                .andExpect(jsonPath("$.estado").value("TRIBUTOS_PAGADOS"));
    }

    @Test
    void cargo_descargaElPdf() throws Exception {
        Path ruta = Files.write(directorio.resolve("cargo.pdf"), PDF);
        when(damService.cargo(1L)).thenReturn(new ArchivoCargo(ruta, "Cargo DAM.pdf"));

        mvc.perform(get("/api/v1/despachos/1/dam/cargo").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void rectificarComoAgente_responde200ConLaDamCorregida() throws Exception {
        when(damService.rectificar(eq(1L), eq("118-2026-10-654321"), any(), eq("Error de digitación"), any(), eq(9L)))
                .thenReturn(new DamResponse("118-2026-10-654321", LocalDate.of(2026, 10, 27), "cargo.pdf",
                        "DAM_NUMERADA"));

        mvc.perform(multipart("/api/v1/despachos/1/dam/rectificacion")
                .param("numeroDam", "118-2026-10-654321").param("motivo", "Error de digitación")
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDam").value("118-2026-10-654321"))
                .andExpect(jsonPath("$.estado").value("DAM_NUMERADA"));
    }

    @Test
    void rectificarSinMotivo_respondeElErrorConElCampoMarcado() throws Exception {
        when(damService.rectificar(eq(1L), any(), any(), any(), any(), any())).thenThrow(
                ApiException.validacion(java.util.Map.of("motivo", "Indique el motivo de la rectificación")));

        mvc.perform(multipart("/api/v1/despachos/1/dam/rectificacion").param("numeroDam", "118-2026-10-654321")
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.motivo").value("Indique el motivo de la rectificación"));
    }

    @Test
    void rectificarSinDamRegistrada_responde409() throws Exception {
        when(damService.rectificar(eq(1L), any(), any(), any(), any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT, CodigoError.DAM_NO_REGISTRADA, "La DAM aún no está registrada."));

        mvc.perform(multipart("/api/v1/despachos/1/dam/rectificacion").param("numeroDam", "118-2026-10-654321")
                .param("motivo", "Error").with(csrf()).cookie(sesion("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.DAM_NO_REGISTRADA));
    }

    @Test
    void rectificarComoCliente_responde403() throws Exception {
        mvc.perform(multipart("/api/v1/despachos/1/dam/rectificacion").param("numeroDam", "118-2026-10-654321")
                .param("motivo", "Error").with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden());
        verify(damService, never()).rectificar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void historial_loPuedeConsultarCualquierPerfil() throws Exception {
        when(damService.historial(1L)).thenReturn(java.util.List.of(new DamHistorialResponse("118-2026-10-123456",
                LocalDate.of(2026, 10, 26), "118-2026-10-654321", LocalDate.of(2026, 10, 26), "Error de digitación",
                "Agente Demo", java.time.LocalDateTime.of(2026, 10, 28, 10, 0))));

        mvc.perform(get("/api/v1/despachos/1/dam/historial").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroAnterior").value("118-2026-10-123456"))
                .andExpect(jsonPath("$[0].numeroNuevo").value("118-2026-10-654321"))
                .andExpect(jsonPath("$[0].motivo").value("Error de digitación"))
                .andExpect(jsonPath("$[0].usuario").value("Agente Demo"));
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
