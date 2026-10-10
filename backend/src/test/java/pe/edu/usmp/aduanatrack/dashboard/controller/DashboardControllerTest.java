package pe.edu.usmp.aduanatrack.dashboard.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroExcelService;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(DashboardController.class)
@Import(SeguridadWebTestConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TableroService tableroService;

    @MockitoBean
    private TableroExcelService excelService;

    @Test
    void despachos_pasaLosFiltros() throws Exception {
        when(tableroService.despachos(118L, null)).thenReturn(List.of());

        mvc.perform(get("/api/v1/dashboard/despachos").param("aduanaId", "118").cookie(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void excel_descargaXlsxConNombreConFecha() throws Exception {
        when(tableroService.despachos(null, 1L)).thenReturn(List.of());
        when(excelService.generar(anyList())).thenReturn(new byte[] { 1, 2, 3 });

        mvc.perform(get("/api/v1/dashboard/despachos/excel").param("clienteId", "1").cookie(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DashboardController.XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=\"despachos-\\d{8}\\.xlsx\"")))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    private Cookie sesion() {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(
                new UsuarioAutenticado(9L, "agente", "Agente", "AGENTE"), Duration.ofHours(2)));
    }
}
