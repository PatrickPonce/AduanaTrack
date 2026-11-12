package pe.edu.usmp.aduanatrack.reporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.service.DatosReporteService;
import pe.edu.usmp.aduanatrack.reporte.service.ReporteExcelService;
import pe.edu.usmp.aduanatrack.reporte.service.ReportePdfService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(ReporteExportController.class)
@Import({ SeguridadWebTestConfig.class, ReporteExportControllerTest.RelojFijo.class })
class ReporteExportControllerTest {

    private static final String RUTA = "/api/v1/reportes/exportar";
    private static final DatosReporte DATOS = new DatosReporte(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            0, 0, null, new TreeMap<>(), Map.of(), 0, List.of());

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(LocalDate.of(2026, 11, 16).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DatosReporteService datosService;

    @MockitoBean
    private ReporteExcelService excelService;

    @MockitoBean
    private ReportePdfService pdfService;

    // CA3: las dos exportaciones descargan un archivo

    @Test
    void excel_descargaElXlsxConElNombreDelReporte() throws Exception {
        when(datosService.generar("2026-10-01", "2026-10-31")).thenReturn(DATOS);
        when(excelService.generar(DATOS)).thenReturn(new byte[] { 1, 2, 3 });

        mvc.perform(get(RUTA).param("formato", "xlsx").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                .cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ReporteExportController.XLSX))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte-gestion_2026-11-16.xlsx\""))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    void pdf_descargaElPdfConElNombreDelReporte() throws Exception {
        when(datosService.generar("2026-10-01", "2026-10-31")).thenReturn(DATOS);
        when(pdfService.generar(DATOS)).thenReturn(new byte[] { 9, 8 });

        mvc.perform(get(RUTA).param("formato", "PDF").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                .cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte-gestion_2026-11-16.pdf\""))
                .andExpect(content().bytes(new byte[] { 9, 8 }));
    }

    @Test
    void formatoInvalido_responde400ConElCampo() throws Exception {
        mvc.perform(get(RUTA).param("formato", "docx").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                .cookie(sesion("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.formato").exists());
        mvc.perform(get(RUTA).param("desde", "2026-10-01").param("hasta", "2026-10-31").cookie(sesion("ADMIN")))
                .andExpect(status().isBadRequest());

        verify(datosService, never()).generar(any(), any());
    }

    @Test
    void rangoInvalido_respondeElErrorDelServicio() throws Exception {
        when(datosService.generar("2026-11-30", "2026-11-01"))
                .thenThrow(ApiException.validacion(Map.of("desde", "«Desde» no puede ser posterior a «Hasta»")));

        mvc.perform(get(RUTA).param("formato", "xlsx").param("desde", "2026-11-30").param("hasta", "2026-11-01")
                .cookie(sesion("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.desde").exists());
    }

    @Test
    void comoCliente_responde403SinGenerarNada() throws Exception {
        mvc.perform(get(RUTA).param("formato", "xlsx").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                .cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verify(datosService, never()).generar(any(), any());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get(RUTA).param("formato", "xlsx")).andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
