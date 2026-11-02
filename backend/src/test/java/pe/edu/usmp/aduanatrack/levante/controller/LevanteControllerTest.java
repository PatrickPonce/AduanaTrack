package pe.edu.usmp.aduanatrack.levante.controller;

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
import java.util.List;
import java.util.Map;

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
import pe.edu.usmp.aduanatrack.levante.dto.LevanteResponse;
import pe.edu.usmp.aduanatrack.levante.service.LevanteService;
import pe.edu.usmp.aduanatrack.levante.service.LevanteService.ArchivoConstancia;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(LevanteController.class)
@Import(SeguridadWebTestConfig.class)
class LevanteControllerTest {

    private static final String RUTA = "/api/v1/despachos/1/levante";
    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LevanteService levanteService;

    @TempDir
    Path directorio;

    private static MockMultipartFile datos(String json) {
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static LevanteResponse registrado(boolean conAdvertencia) {
        return new LevanteResponse("LEV-2026-000318", LocalDate.of(2026, 11, 13), "constancia.pdf", conAdvertencia,
                true, EstadoDespacho.LEVANTE_OTORGADO);
    }

    @Test
    void registrarComoAgente_responde201ConLaFormaDelContratoC8() throws Exception {
        when(levanteService.registrar(eq(1L), any(), any())).thenReturn(registrado(false));

        mvc.perform(multipart(RUTA)
                .file(datos("{\"numeroLevante\":\"LEV-2026-000318\",\"fechaLevante\":\"2026-11-13\","
                        + "\"confirmarSinRequisitos\":false}"))
                .file(new MockMultipartFile("constancia", "constancia.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoDespacho").value("LEVANTE_OTORGADO"))
                .andExpect(jsonPath("$.conAdvertencia").value(false))
                .andExpect(jsonPath("$.retiroHabilitado").value(true))
                .andExpect(jsonPath("$.numeroLevante").value("LEV-2026-000318"))
                .andExpect(jsonPath("$.constanciaNombre").value("constancia.pdf"));
    }

    // CA2: los requisitos pendientes viajan en la raíz del 409 para que la pantalla pregunte

    @Test
    void requisitosPendientes_responde409ConLaListaDePendientes() throws Exception {
        when(levanteService.registrar(eq(1L), any(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.LEVANTE_REQUISITOS_PENDIENTES, "Faltan requisitos previos al levante.",
                Map.of("pendientes", List.of("Pago de tributos", "Inspección conforme"))));

        mvc.perform(multipart(RUTA)
                .file(datos("{\"numeroLevante\":\"LEV-1\",\"fechaLevante\":\"2026-11-13\"}"))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("LEVANTE_REQUISITOS_PENDIENTES"))
                .andExpect(jsonPath("$.pendientes[0]").value("Pago de tributos"))
                .andExpect(jsonPath("$.pendientes[1]").value("Inspección conforme"));
    }

    @Test
    void confirmarSinRequisitos_llegaAlServicio() throws Exception {
        when(levanteService.registrar(eq(1L), any(), any())).thenReturn(registrado(true));

        mvc.perform(multipart(RUTA)
                .file(datos("{\"numeroLevante\":\"LEV-1\",\"fechaLevante\":\"2026-11-13\","
                        + "\"confirmarSinRequisitos\":true}"))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conAdvertencia").value(true));

        verify(levanteService).registrar(eq(1L), org.mockito.ArgumentMatchers.argThat(
                datos -> datos.confirmaSinRequisitos() && datos.numeroLevante().equals("LEV-1")), any());
    }

    @Test
    void sinNumeroNiFecha_responde400ConLosCampos() throws Exception {
        mvc.perform(multipart(RUTA).file(datos("{}")).with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.numeroLevante").exists())
                .andExpect(jsonPath("$.campos.fechaLevante").exists());

        verify(levanteService, never()).registrar(any(), any(), any());
    }

    @Test
    void sinLaParteDatos_responde400EnVezDeUnErrorGenerico() throws Exception {
        mvc.perform(multipart(RUTA).file(new MockMultipartFile("constancia", "c.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroLevante").exists());
    }

    @Test
    void comoCliente_responde403() throws Exception {
        mvc.perform(multipart(RUTA).file(datos("{\"numeroLevante\":\"LEV-1\",\"fechaLevante\":\"2026-11-13\"}"))
                .with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden());

        verify(levanteService, never()).registrar(any(), any(), any());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(multipart(RUTA).file(datos("{}")).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void consultarComoCliente_responde200() throws Exception {
        when(levanteService.obtener(1L)).thenReturn(registrado(false));

        mvc.perform(get(RUTA).cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLevante").value("LEV-2026-000318"))
                .andExpect(jsonPath("$.fechaLevante").value("2026-11-13"));
    }

    @Test
    void descargarConstancia_devuelveElPdf() throws Exception {
        Path archivo = Files.write(directorio.resolve("constancia.pdf"), PDF);
        when(levanteService.constancia(1L)).thenReturn(new ArchivoConstancia(archivo, "constancia 318.pdf"));

        mvc.perform(get(RUTA + "/constancia").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void descargarSinConstancia_responde404() throws Exception {
        when(levanteService.constancia(1L)).thenThrow(ApiException.noEncontrado("El levante no tiene constancia."));

        mvc.perform(get(RUTA + "/constancia").cookie(sesion("AGENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.NO_ENCONTRADO));
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
