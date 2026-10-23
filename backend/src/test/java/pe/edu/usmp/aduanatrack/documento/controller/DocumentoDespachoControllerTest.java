package pe.edu.usmp.aduanatrack.documento.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoCargadoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoRequeridoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentosDespachoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumentacion;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumento;
import pe.edu.usmp.aduanatrack.documento.dto.VersionDocumentoResponse;
import pe.edu.usmp.aduanatrack.documento.service.DocumentoDespachoService;
import pe.edu.usmp.aduanatrack.documento.service.DocumentoDespachoService.ArchivoDocumento;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(DocumentoDespachoController.class)
@Import(SeguridadWebTestConfig.class)
class DocumentoDespachoControllerTest {

    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DocumentoDespachoService documentoService;

    @TempDir
    Path directorio;

    @Test
    void listar_respondeElContratoC1() throws Exception {
        when(documentoService.listar(1L)).thenReturn(new DocumentosDespachoResponse(EstadoDocumentacion.PENDIENTE,
                List.of(new DocumentoRequeridoResponse("FACTURA_COMERCIAL", "Factura comercial", true,
                        EstadoDocumento.CARGADO, 2, 41L, "factura.pdf", 482113L, "Agente Demo",
                        LocalDateTime.of(2026, 10, 22, 10, 15)),
                        DocumentoRequeridoResponse.pendiente("POLIZA_SEGURO", "Póliza de seguro", true))));

        mvc.perform(get("/api/v1/despachos/1/documentos").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoGeneral").value("PENDIENTE"))
                .andExpect(jsonPath("$.documentos[0].tipo").value("FACTURA_COMERCIAL"))
                .andExpect(jsonPath("$.documentos[0].estado").value("CARGADO"))
                .andExpect(jsonPath("$.documentos[0].version").value(2))
                .andExpect(jsonPath("$.documentos[0].documentoId").value(41))
                .andExpect(jsonPath("$.documentos[0].cargadoEn").value("2026-10-22T10:15:00"))
                .andExpect(jsonPath("$.documentos[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.documentos[1].version").doesNotExist());
    }

    @Test
    void cargarComoAgente_responde201ConLaVersion() throws Exception {
        when(documentoService.cargar(eq(1L), eq("FACTURA_COMERCIAL"), any(), eq(9L)))
                .thenReturn(new DocumentoCargadoResponse(42L, "FACTURA_COMERCIAL", 3, EstadoDocumento.CARGADO));

        mvc.perform(multipart("/api/v1/despachos/1/documentos/FACTURA_COMERCIAL")
                .file(new MockMultipartFile("archivo", "factura.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentoId").value(42))
                .andExpect(jsonPath("$.tipo").value("FACTURA_COMERCIAL"))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.estado").value("CARGADO"));
    }

    @Test
    void cargarComoCliente_responde403() throws Exception {
        mvc.perform(multipart("/api/v1/despachos/1/documentos/FACTURA_COMERCIAL")
                .file(new MockMultipartFile("archivo", "factura.pdf", "application/pdf", PDF))
                .with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.ACCESO_DENEGADO));
        verify(documentoService, never()).cargar(any(), any(), any(), any());
    }

    @Test
    void cargarSinTokenCsrf_responde403() throws Exception {
        mvc.perform(multipart("/api/v1/despachos/1/documentos/FACTURA_COMERCIAL")
                .file(new MockMultipartFile("archivo", "factura.pdf", "application/pdf", PDF))
                .cookie(sesion("AGENTE")))
                .andExpect(status().isForbidden());
        verify(documentoService, never()).cargar(any(), any(), any(), any());
    }

    @Test
    void versiones_respondeLaListaDeLaMasRecienteALaMasAntigua() throws Exception {
        when(documentoService.versiones(1L, "FACTURA_COMERCIAL")).thenReturn(List.of(
                new VersionDocumentoResponse(42L, 2, true, "factura-v2.pdf", 10, "Agente Demo",
                        LocalDateTime.of(2026, 10, 23, 9, 40)),
                new VersionDocumentoResponse(41L, 1, false, "factura.pdf", 10, "Agente Demo",
                        LocalDateTime.of(2026, 10, 22, 10, 15))));

        mvc.perform(get("/api/v1/despachos/1/documentos/FACTURA_COMERCIAL/versiones").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].vigente").value(true))
                .andExpect(jsonPath("$[1].vigente").value(false));
    }

    @Test
    void archivo_descargaElPdfConElNombreOriginal() throws Exception {
        Path ruta = Files.write(directorio.resolve("uuid.pdf"), PDF);
        when(documentoService.archivo(1L, 41L)).thenReturn(new ArchivoDocumento(ruta, "Factura 001.pdf"));

        mvc.perform(get("/api/v1/despachos/1/documentos/41/archivo").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes(PDF));
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
