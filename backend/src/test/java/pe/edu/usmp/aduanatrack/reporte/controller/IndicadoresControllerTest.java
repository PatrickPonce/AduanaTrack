package pe.edu.usmp.aduanatrack.reporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.service.DatosReporteService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(IndicadoresController.class)
@Import(SeguridadWebTestConfig.class)
class IndicadoresControllerTest {

    private static final String RUTA = "/api/v1/reportes/indicadores";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DatosReporteService datosService;

    private static DatosReporte datos(Double promedio) {
        Map<YearMonth, Long> porMes = new TreeMap<>();
        porMes.put(YearMonth.of(2026, 9), 2L);
        porMes.put(YearMonth.of(2026, 10), 3L);
        Map<String, Long> porCanal = new LinkedHashMap<>();
        porCanal.put("VERDE", 2L);
        porCanal.put("NARANJA", 1L);
        porCanal.put("ROJO", 1L);
        return new DatosReporte(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), 5, 3, promedio, porMes,
                porCanal, 1, List.of());
    }

    @Test
    void devuelveLosIndicadoresConLaFormaDelContratoC10() throws Exception {
        when(datosService.generar("2026-09-01", "2026-10-31")).thenReturn(datos(4.5));

        mvc.perform(get(RUTA).param("desde", "2026-09-01").param("hasta", "2026-10-31").cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDespachos").value(5))
                .andExpect(jsonPath("$.conLevante").value(3))
                .andExpect(jsonPath("$.tiempoPromedioDias").value(4.5))
                .andExpect(jsonPath("$.despachosPorMes[0].mes").value("2026-09"))
                .andExpect(jsonPath("$.despachosPorMes[0].cantidad").value(2))
                .andExpect(jsonPath("$.despachosPorMes[1].mes").value("2026-10"))
                .andExpect(jsonPath("$.porCanal.VERDE").value(2))
                .andExpect(jsonPath("$.porCanal.NARANJA").value(1))
                .andExpect(jsonPath("$.porCanal.ROJO").value(1));
    }

    @Test
    void sinDespachosConLevante_elTiempoPromedioVieneNulo() throws Exception {
        when(datosService.generar("2026-09-01", "2026-10-31")).thenReturn(datos(null));

        mvc.perform(get(RUTA).param("desde", "2026-09-01").param("hasta", "2026-10-31").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiempoPromedioDias").doesNotExist());
    }

    @Test
    void rangoInvalido_respondeElErrorDelServicioConElCampo() throws Exception {
        when(datosService.generar("2026-11-30", "2026-11-01"))
                .thenThrow(ApiException.validacion(Map.of("desde", "«Desde» no puede ser posterior a «Hasta»")));

        mvc.perform(get(RUTA).param("desde", "2026-11-30").param("hasta", "2026-11-01").cookie(sesion("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.desde").exists());
    }

    @Test
    void sinFechas_llegaAlServicioQueResponde400() throws Exception {
        when(datosService.generar(null, null))
                .thenThrow(ApiException.validacion(Map.of("desde", "Indique la fecha «Desde»")));

        mvc.perform(get(RUTA).cookie(sesion("ADMIN"))).andExpect(status().isBadRequest());
    }

    @Test
    void comoCliente_responde403SinCalcularNada() throws Exception {
        mvc.perform(get(RUTA).param("desde", "2026-09-01").param("hasta", "2026-10-31").cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verify(datosService, never()).generar(any(), any());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get(RUTA).param("desde", "2026-09-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
