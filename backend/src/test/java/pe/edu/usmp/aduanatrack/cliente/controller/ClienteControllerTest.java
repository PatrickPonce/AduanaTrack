package pe.edu.usmp.aduanatrack.cliente.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteRequest;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.service.ClienteService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

@WebMvcTest(ClienteController.class)
@Import(SeguridadWebTestConfig.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void crear_rucValido_responde201() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class))).thenReturn(new ClienteResponse(1L, "20787789324",
                "Importadora Andina SAC", null, null, null, null, true, EstadoCartaPoder.SIN_CARTA, null, null));

        mvc.perform(autenticado(post("/api/v1/clientes"))
                .content("{\"ruc\":\"20787789324\",\"razonSocial\":\"Importadora Andina SAC\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clientes/1"))
                .andExpect(jsonPath("$.estadoCartaPoder").value("SIN_CARTA"));
    }

    @Test
    void crear_rucDe10Digitos_responde400EnElCampoRuc() throws Exception {
        mvc.perform(autenticado(post("/api/v1/clientes"))
                .content("{\"ruc\":\"2078778932\",\"razonSocial\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(CodigoError.VALIDACION))
                .andExpect(jsonPath("$.campos.ruc").value("Debe tener 11 dígitos"));
    }

    @Test
    void crear_rucConDigitoVerificadorErrado_responde400() throws Exception {
        mvc.perform(autenticado(post("/api/v1/clientes"))
                .content("{\"ruc\":\"20787789325\",\"razonSocial\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ruc").exists());
    }

    @Test
    void crear_duplicado_responde409ConClienteIdYRazonSocial() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class))).thenThrow(new ApiException(HttpStatus.CONFLICT,
                CodigoError.CLIENTE_DUPLICADO, "Este RUC ya está registrado: Importadora Andina SAC",
                Map.of("clienteId", 1L, "razonSocial", "Importadora Andina SAC")));

        mvc.perform(autenticado(post("/api/v1/clientes"))
                .content("{\"ruc\":\"20787789324\",\"razonSocial\":\"Otra\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(CodigoError.CLIENTE_DUPLICADO))
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.razonSocial").value("Importadora Andina SAC"));
    }

    @Test
    void sinSesion_responde401() throws Exception {
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void perfilCliente_noPuedeRegistrarClientes_responde403() throws Exception {
        mvc.perform(autenticado(post("/api/v1/clientes"), "CLIENTE")
                .content("{\"ruc\":\"20787789324\",\"razonSocial\":\"Importadora Andina SAC\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.ACCESO_DENEGADO));
    }

    @Test
    void perfilCliente_puedeConsultar() throws Exception {
        mvc.perform(autenticado(get("/api/v1/clientes"), "CLIENTE")).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder solicitud) {
        return autenticado(solicitud, "AGENTE");
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder solicitud, String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(9L, "usuario", "Usuario", rol),
                Duration.ofHours(2));
        return solicitud.with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie(SesionCookieService.NOMBRE_COOKIE, token));
    }
}
