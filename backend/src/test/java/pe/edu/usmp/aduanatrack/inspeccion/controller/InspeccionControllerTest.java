package pe.edu.usmp.aduanatrack.inspeccion.controller;

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
import java.time.LocalDateTime;
import java.util.List;

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
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionResponse;
import pe.edu.usmp.aduanatrack.inspeccion.entity.ResultadoInspeccion;
import pe.edu.usmp.aduanatrack.inspeccion.service.InspeccionService;
import pe.edu.usmp.aduanatrack.inspeccion.service.InspeccionService.ArchivoActa;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(InspeccionController.class)
@Import(SeguridadWebTestConfig.class)
class InspeccionControllerTest {

    private static final String RUTA = "/api/v1/despachos/1/inspeccion";
    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final String DATOS = "{\"items\":[{\"descripcion\":\"Cantidad de bultos\",\"conforme\":true,"
            + "\"observacion\":null}],\"observaciones\":null}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InspeccionService inspeccionService;

    @TempDir
    Path directorio;

    private static MockMultipartFile datos(String json) {
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static InspeccionResponse registrada(ResultadoInspeccion resultado, EstadoDespacho estado) {
        return new InspeccionResponse(resultado, estado, estado == EstadoDespacho.INSPECCION_CONFORME, "acta.pdf",
                null, List.of(new InspeccionResponse.Item("Cantidad de bultos", true, null)), "Agente Demo",
                LocalDateTime.of(2026, 11, 12, 10, 30));
    }

    @Test
    void registrarComoAgente_responde201ConLaFormaDelContratoC7() throws Exception {
        when(inspeccionService.registrar(eq(1L), eq(9L), any(), any()))
                .thenReturn(registrada(ResultadoInspeccion.CONFORME, EstadoDespacho.INSPECCION_CONFORME));

        mvc.perform(multipart(RUTA).file(datos(DATOS))
                .file(new MockMultipartFile("acta", "acta.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("CONFORME"))
                .andExpect(jsonPath("$.estadoDespacho").value("INSPECCION_CONFORME"))
                .andExpect(jsonPath("$.levanteHabilitado").value(true))
                .andExpect(jsonPath("$.actaNombre").value("acta.pdf"))
                .andExpect(jsonPath("$.items[0].descripcion").value("Cantidad de bultos"));
    }

    @Test
    void conObservaciones_noHabilitaElLevante() throws Exception {
        when(inspeccionService.registrar(eq(1L), eq(9L), any(), any()))
                .thenReturn(registrada(ResultadoInspeccion.CON_OBSERVACIONES, EstadoDespacho.INSPECCION_PROGRAMADA));

        mvc.perform(multipart(RUTA).file(datos(DATOS)).with(csrf()).cookie(sesion("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("CON_OBSERVACIONES"))
                .andExpect(jsonPath("$.levanteHabilitado").value(false));
    }

    @Test
    void despachoSinInspeccionProgramada_responde409() throws Exception {
        when(inspeccionService.registrar(eq(1L), eq(9L), any(), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT, CodigoError.INSPECCION_NO_PROGRAMADA, "Programe la cita de inspección."));

        mvc.perform(multipart(RUTA).file(datos(DATOS)).with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSPECCION_NO_PROGRAMADA"));
    }

    @Test
    void sinLaParteDatos_llegaAlServicioQueResponde400() throws Exception {
        when(inspeccionService.registrar(eq(1L), eq(9L), eq(null), any()))
                .thenThrow(ApiException.validacion(java.util.Map.of("items", "Complete el checklist")));

        mvc.perform(multipart(RUTA).file(new MockMultipartFile("acta", "a.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.items").exists());
    }

    @Test
    void comoCliente_responde403() throws Exception {
        mvc.perform(multipart(RUTA).file(datos(DATOS)).with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden());

        verify(inspeccionService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(multipart(RUTA).file(datos(DATOS)).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void consultarComoCliente_responde200() throws Exception {
        when(inspeccionService.ultima(1L))
                .thenReturn(registrada(ResultadoInspeccion.CONFORME, EstadoDespacho.INSPECCION_CONFORME));

        mvc.perform(get(RUTA).cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("CONFORME"))
                .andExpect(jsonPath("$.registradaPor").value("Agente Demo"));
    }

    @Test
    void consultarSinInspeccion_responde404() throws Exception {
        when(inspeccionService.ultima(1L)).thenThrow(ApiException.noEncontrado("Sin inspección."));

        mvc.perform(get(RUTA).cookie(sesion("AGENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void catalogoDeItems_loVenLosTresPerfiles() throws Exception {
        mvc.perform(get("/api/v1/catalogos/items-inspeccion").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Cantidad de bultos"))
                .andExpect(jsonPath("$.length()").value(InspeccionService.ITEMS.size()));
    }

    @Test
    void descargarActa_devuelveElPdf() throws Exception {
        Path archivo = Files.write(directorio.resolve("acta.pdf"), PDF);
        when(inspeccionService.acta(1L)).thenReturn(new ArchivoActa(archivo, "acta 123.pdf"));

        mvc.perform(get(RUTA + "/acta").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void descargarSinActa_responde404() throws Exception {
        when(inspeccionService.acta(1L)).thenThrow(ApiException.noEncontrado("La inspección no tiene acta."));

        mvc.perform(get(RUTA + "/acta").cookie(sesion("AGENTE")))
                .andExpect(status().isNotFound());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
