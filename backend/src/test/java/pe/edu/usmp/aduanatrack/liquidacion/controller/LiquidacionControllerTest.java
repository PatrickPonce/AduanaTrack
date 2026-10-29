package pe.edu.usmp.aduanatrack.liquidacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionConsultaResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionConsultaResponse.PagoConsulta;
import pe.edu.usmp.aduanatrack.liquidacion.dto.LiquidacionResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.PagoResponse;
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo.Tasas;
import pe.edu.usmp.aduanatrack.liquidacion.service.LiquidacionService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(LiquidacionController.class)
@Import(SeguridadWebTestConfig.class)
class LiquidacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LiquidacionService liquidacionService;

    @Test
    void generar_respondeElContratoC3() throws Exception {
        when(liquidacionService.generar(eq(1L), any(), eq(9L))).thenReturn(new LiquidacionResponse(
                new BigDecimal("10000.00"), new BigDecimal("800.00"), new BigDecimal("150.00"),
                new BigDecimal("10950.00"), new BigDecimal("657.00"), new BigDecimal("1857.12"),
                new BigDecimal("232.14"), new BigDecimal("479.37"), new BigDecimal("3225.63"), "USD",
                new Tasas(new BigDecimal("16"), new BigDecimal("2"), new BigDecimal("3.5")), "LIQUIDADO"));

        mvc.perform(post("/api/v1/despachos/1/liquidacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"flete\": 800.00, \"seguro\": 150.00}").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cif").value(10950.00))
                .andExpect(jsonPath("$.total").value(3225.63))
                .andExpect(jsonPath("$.tasas.percepcion").value(3.5))
                .andExpect(jsonPath("$.estado").value("LIQUIDADO"));
    }

    @Test
    void consultar_elClienteTambienVeLaLiquidacionGuardada() throws Exception {
        when(liquidacionService.consultar(1L)).thenReturn(new LiquidacionConsultaResponse(
                new LiquidacionResponse(new BigDecimal("10000.00"), new BigDecimal("800.00"),
                        new BigDecimal("150.00"), new BigDecimal("10950.00"), new BigDecimal("657.00"),
                        new BigDecimal("1857.12"), new BigDecimal("232.14"), new BigDecimal("479.37"),
                        new BigDecimal("3225.63"), "USD",
                        new Tasas(new BigDecimal("16"), new BigDecimal("2"), new BigDecimal("3.5")), "TRIBUTOS_PAGADOS"),
                "Agente Demo", LocalDateTime.of(2026, 10, 28, 10, 0),
                new PagoConsulta(LocalDate.of(2026, 10, 28), new BigDecimal("3225.63"), "VENTANILLA", "00123456",
                        "Agente Demo", LocalDateTime.of(2026, 10, 28, 11, 0))));

        mvc.perform(get("/api/v1/despachos/1/liquidacion").cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liquidacion.total").value(3225.63))
                .andExpect(jsonPath("$.liquidacion.estado").value("TRIBUTOS_PAGADOS"))
                .andExpect(jsonPath("$.generadoPor").value("Agente Demo"))
                .andExpect(jsonPath("$.pago.medioPago").value("VENTANILLA"))
                .andExpect(jsonPath("$.pago.fecha").value("2026-10-28"));
    }

    @Test
    void generarSinSeguroOConFleteNegativo_marcaLosCampos() throws Exception {
        mvc.perform(post("/api/v1/despachos/1/liquidacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"flete\": -1.00}").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.flete").exists())
                .andExpect(jsonPath("$.campos.seguro").exists());
        verify(liquidacionService, never()).generar(any(), any(), any());
    }

    @Test
    void generarSinClasificar_responde409() throws Exception {
        when(liquidacionService.generar(eq(1L), any(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.DESPACHO_NO_CLASIFICADO, "Clasifique todos los ítems del despacho."));

        mvc.perform(post("/api/v1/despachos/1/liquidacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"flete\": 0, \"seguro\": 0}").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.DESPACHO_NO_CLASIFICADO));
    }

    @Test
    void generarComoCliente_responde403() throws Exception {
        mvc.perform(post("/api/v1/despachos/1/liquidacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"flete\": 0, \"seguro\": 0}").with(csrf()).cookie(sesion("CLIENTE")))
                .andExpect(status().isForbidden());
        verify(liquidacionService, never()).generar(any(), any(), any());
    }

    @Test
    void pagar_responde201ConElEstado() throws Exception {
        when(liquidacionService.registrarPago(eq(1L), any(), eq(9L))).thenReturn(new PagoResponse("TRIBUTOS_PAGADOS"));

        mvc.perform(post("/api/v1/despachos/1/pago").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fecha": "2026-10-28", "monto": 3225.63, "medioPago": "TRANSFERENCIA",
                         "numeroOperacion": "00123456"}""")
                .with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("TRIBUTOS_PAGADOS"));
    }

    @Test
    void pagarSinDatos_marcaLosCuatroCampos() throws Exception {
        mvc.perform(post("/api/v1/despachos/1/pago").contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\": 0}").with(csrf()).cookie(sesion("AGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fecha").exists())
                .andExpect(jsonPath("$.campos.monto").exists())
                .andExpect(jsonPath("$.campos.medioPago").exists())
                .andExpect(jsonPath("$.campos.numeroOperacion").exists());
        verify(liquidacionService, never()).registrarPago(any(), any(), any());
    }

    @Test
    void pagarSinLiquidacion_responde409() throws Exception {
        when(liquidacionService.registrarPago(eq(1L), any(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.LIQUIDACION_PENDIENTE, "Genere la liquidación antes de registrar el pago."));

        mvc.perform(post("/api/v1/despachos/1/pago").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fecha": "2026-10-28", "monto": 10.00, "medioPago": "EFECTIVO", "numeroOperacion": "1"}""")
                .with(csrf()).cookie(sesion("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.LIQUIDACION_PENDIENTE));
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
