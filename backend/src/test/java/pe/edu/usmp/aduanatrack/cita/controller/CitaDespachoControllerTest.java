package pe.edu.usmp.aduanatrack.cita.controller;

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.cita.dto.CitaResponse;
import pe.edu.usmp.aduanatrack.cita.dto.DepositoResponse;
import pe.edu.usmp.aduanatrack.cita.entity.EstadoCita;
import pe.edu.usmp.aduanatrack.cita.entity.TipoCita;
import pe.edu.usmp.aduanatrack.cita.entity.TipoDeposito;
import pe.edu.usmp.aduanatrack.cita.service.CitaDespachoService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest({ CitaDespachoController.class, DepositoController.class })
@Import(SeguridadWebTestConfig.class)
class CitaDespachoControllerTest {

    private static final String RUTA = "/api/v1/despachos/1/citas";
    private static final String CUERPO_INSPECCION = """
            {"depositoId":1,"numeroCita":"TK-2026-004512","tipo":"INSPECCION",
             "fechaHoraConfirmada":"2026-11-12T09:30:00"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CitaDespachoService citaService;

    private static CitaResponse citaDeInspeccion() {
        return new CitaResponse(7L, 1L, "Depósito temporal 1 (ejemplo)", "TK-2026-004512", TipoCita.INSPECCION,
                LocalDateTime.of(2026, 11, 12, 9, 30), EstadoCita.PROGRAMADA, null, "Agente Demo",
                LocalDateTime.of(2026, 11, 4, 10, 0), EstadoDespacho.INSPECCION_PROGRAMADA);
    }

    @Test
    void registrarComoAgente_responde201ConLaFormaDelContratoC6() throws Exception {
        when(citaService.registrar(eq(1L), any(), eq(9L))).thenReturn(citaDeInspeccion());

        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_INSPECCION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.estadoDespacho").value("INSPECCION_PROGRAMADA"))
                .andExpect(jsonPath("$.tipo").value("INSPECCION"))
                .andExpect(jsonPath("$.fechaHoraConfirmada").value("2026-11-12T09:30:00"))
                .andExpect(jsonPath("$.transportista").doesNotExist());
    }

    // CA4: sin «Fecha y hora confirmada» responde 400 con el campo y no guarda

    @Test
    void sinFechaHoraConfirmada_responde400ConElCampoYNoGuarda() throws Exception {
        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"depositoId\":1,\"numeroCita\":\"TK-1\",\"tipo\":\"INSPECCION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.fechaHoraConfirmada").exists());

        verify(citaService, never()).registrar(any(), any(), any());
    }

    @Test
    void sinDepositoNiNumero_marcaLosDosCampos() throws Exception {
        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"INSPECCION\",\"fechaHoraConfirmada\":\"2026-11-12T09:30:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.depositoId").exists())
                .andExpect(jsonPath("$.campos.numeroCita").exists());
    }

    @Test
    void retiroSinLevante_responde409() throws Exception {
        when(citaService.registrar(eq(1L), any(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.CITA_RETIRO_SIN_LEVANTE, "Registre el levante antes de programar el retiro."));

        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("AGENTE")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"depositoId":1,"numeroCita":"TK-1","tipo":"RETIRO","fechaHoraConfirmada":"2026-11-12T09:30:00"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CITA_RETIRO_SIN_LEVANTE"));
    }

    @Test
    void comoCliente_responde403() throws Exception {
        mvc.perform(post(RUTA).with(csrf()).cookie(sesion("CLIENTE")).contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_INSPECCION))
                .andExpect(status().isForbidden());

        verify(citaService, never()).registrar(any(), any(), any());
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(post(RUTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CUERPO_INSPECCION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarComoCliente_responde200() throws Exception {
        when(citaService.listar(1L)).thenReturn(List.of(citaDeInspeccion()));

        mvc.perform(get(RUTA).cookie(sesion("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroCita").value("TK-2026-004512"))
                .andExpect(jsonPath("$[0].deposito").value("Depósito temporal 1 (ejemplo)"));
    }

    @Test
    void catalogoDeDepositos_respondeLaFormaDelContratoC6() throws Exception {
        when(citaService.depositos()).thenReturn(List.of(
                new DepositoResponse(1L, "DT-01", "Depósito temporal 1 (ejemplo)", TipoDeposito.DEPOSITO_TEMPORAL)));

        mvc.perform(get("/api/v1/catalogos/depositos").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("DT-01"))
                .andExpect(jsonPath("$[0].tipo").value("DEPOSITO_TEMPORAL"));
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }
}
