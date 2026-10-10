package pe.edu.usmp.aduanatrack.cliente.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderCargaService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(CartaPoderController.class)
@Import(SeguridadWebTestConfig.class)
class CartaPoderControllerTest {

    private static final String RUTA = "/api/v1/clientes/7/carta-poder";
    private static final MockMultipartFile PDF = new MockMultipartFile("archivo", "carta.pdf", "application/pdf",
            "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CartaPoderCargaService cargaService;

    /** Los nombres de los campos son los que envía el frontend (api/clientes.ts). */
    @Test
    void cargar_pdfValido_responde201() throws Exception {
        when(cargaService.cargar(eq(7L), any(MultipartFile.class), eq("2026-01-15"), eq("2027-01-15")))
                .thenReturn(new CartaPoderResponse(3L, 7L, LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15),
                        "carta.pdf", 8, "a".repeat(64), EstadoCartaPoder.VIGENTE));

        mvc.perform(multipart(RUTA).file(PDF).param("fechaEmision", "2026-01-15")
                .param("fechaVencimiento", "2027-01-15").with(csrf()).cookie(sesion()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.fechaVencimiento").value("2027-01-15"))
                .andExpect(jsonPath("$.nombreArchivo").value("carta.pdf"))
                .andExpect(jsonPath("$.estadoCartaPoder").value("VIGENTE"));
    }

    @Test
    void cargar_archivoQueNoEsPdf_responde415() throws Exception {
        when(cargaService.cargar(eq(7L), any(MultipartFile.class), any(), any()))
                .thenThrow(new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CodigoError.FORMATO_NO_PERMITIDO,
                        "El archivo no es un PDF válido."));

        mvc.perform(multipart(RUTA).file(PDF).param("fechaEmision", "2026-01-15")
                .param("fechaVencimiento", "2027-01-15").with(csrf()).cookie(sesion()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value(CodigoError.FORMATO_NO_PERMITIDO));
    }

    @Test
    void cargar_archivoDeMasDe10Mb_responde413() throws Exception {
        when(cargaService.cargar(eq(7L), any(MultipartFile.class), any(), any()))
                .thenThrow(new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, CodigoError.ARCHIVO_MUY_GRANDE,
                        "El archivo supera el tamaño máximo de 10 MB."));

        mvc.perform(multipart(RUTA).file(PDF).param("fechaEmision", "2026-01-15")
                .param("fechaVencimiento", "2027-01-15").with(csrf()).cookie(sesion()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value(CodigoError.ARCHIVO_MUY_GRANDE));
    }

    @Test
    void cargar_sinArchivoNiFechas_llegaAlServicioYResponde400ConLosCampos() throws Exception {
        when(cargaService.cargar(eq(7L), isNull(), isNull(), isNull())).thenThrow(ApiException.validacion(
                Map.of("archivo", "Seleccione el PDF de la carta poder")));

        mvc.perform(multipart(RUTA).with(csrf()).cookie(sesion()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.archivo").value("Seleccione el PDF de la carta poder"));
    }

    @Test
    void cargar_sinTokenCsrf_responde403() throws Exception {
        mvc.perform(multipart(RUTA).file(PDF).param("fechaEmision", "2026-01-15")
                .param("fechaVencimiento", "2027-01-15").cookie(sesion()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cargar_sinSesion_responde401() throws Exception {
        mvc.perform(multipart(RUTA).file(PDF).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private Cookie sesion() {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(
                new UsuarioAutenticado(9L, "agente", "Agente", "AGENTE"), Duration.ofHours(2)));
    }
}
