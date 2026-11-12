package pe.edu.usmp.aduanatrack.acceso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.acceso.service.AccesoClienteService;
import pe.edu.usmp.aduanatrack.acceso.service.AccesoDenegadoService;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cita.controller.CitaDespachoController;
import pe.edu.usmp.aduanatrack.cita.service.CitaDespachoService;
import pe.edu.usmp.aduanatrack.clasificacion.controller.ClasificacionController;
import pe.edu.usmp.aduanatrack.clasificacion.controller.ItemFacturaController;
import pe.edu.usmp.aduanatrack.clasificacion.service.ClasificacionService;
import pe.edu.usmp.aduanatrack.clasificacion.service.ItemFacturaService;
import pe.edu.usmp.aduanatrack.cliente.controller.CartaPoderConsultaController;
import pe.edu.usmp.aduanatrack.cliente.controller.CartaPoderController;
import pe.edu.usmp.aduanatrack.cliente.controller.ClienteController;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResumen;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderCargaService;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderConsultaService;
import pe.edu.usmp.aduanatrack.cliente.service.ClienteService;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;
import pe.edu.usmp.aduanatrack.dam.controller.NumeracionDamController;
import pe.edu.usmp.aduanatrack.dam.service.NumeracionDamService;
import pe.edu.usmp.aduanatrack.dashboard.controller.DashboardController;
import pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse;
import pe.edu.usmp.aduanatrack.dashboard.service.IndicadoresService;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroExcelService;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroService;
import pe.edu.usmp.aduanatrack.despacho.controller.CanalControlController;
import pe.edu.usmp.aduanatrack.despacho.controller.DespachoController;
import pe.edu.usmp.aduanatrack.despacho.controller.HitoDespachoController;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository.DespachoAcceso;
import pe.edu.usmp.aduanatrack.despacho.service.CanalControlService;
import pe.edu.usmp.aduanatrack.despacho.service.DespachoService;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.controller.DocumentoDespachoController;
import pe.edu.usmp.aduanatrack.documento.service.DocumentoDespachoService;
import pe.edu.usmp.aduanatrack.embarque.controller.EmbarqueController;
import pe.edu.usmp.aduanatrack.embarque.service.EmbarqueService;
import pe.edu.usmp.aduanatrack.inspeccion.controller.InspeccionController;
import pe.edu.usmp.aduanatrack.inspeccion.service.InspeccionService;
import pe.edu.usmp.aduanatrack.levante.controller.LevanteController;
import pe.edu.usmp.aduanatrack.levante.service.LevanteService;
import pe.edu.usmp.aduanatrack.liquidacion.controller.LiquidacionController;
import pe.edu.usmp.aduanatrack.liquidacion.service.LiquidacionService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

/**
 * HU-16 · El cliente A no puede leer nada del cliente B por ningún endpoint. Carga todos los controladores
 * con ruta de despacho o de cliente (con la seguridad y el interceptor reales), recorre TODAS sus rutas
 * registradas con el despacho de la otra empresa y comprueba que ninguna llega al servicio.
 */
@WebMvcTest(controllers = { CitaDespachoController.class, ClasificacionController.class,
        ItemFacturaController.class, CartaPoderConsultaController.class, CartaPoderController.class,
        ClienteController.class, NumeracionDamController.class, DashboardController.class,
        CanalControlController.class, DespachoController.class, HitoDespachoController.class,
        DocumentoDespachoController.class, EmbarqueController.class, LevanteController.class,
        LiquidacionController.class, InspeccionController.class })
@Import({ SeguridadWebTestConfig.class, AccesoWebConfig.class, AccesoClienteService.class })
class AislamientoPorEmpresaTest {

    private static final long EMPRESA_A = 1L;
    private static final long EMPRESA_B = 2L;
    private static final long DESPACHO_A = 10L;
    private static final long DESPACHO_B = 20L;
    private static final long BORRADOR_A = 30L;
    private static final long INEXISTENTE = 999L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RequestMappingHandlerMapping mapeo;

    @MockitoBean
    private UsuarioRepository usuarios;
    @MockitoBean
    private DespachoRepository despachos;
    @MockitoBean
    private AccesoDenegadoService denegados;

    // Servicios que los controladores no deben tocar cuando el acceso se deniega.
    @MockitoBean
    private CitaDespachoService citas;
    @MockitoBean
    private ClasificacionService clasificacion;
    @MockitoBean
    private ItemFacturaService items;
    @MockitoBean
    private CartaPoderConsultaService cartaConsulta;
    @MockitoBean
    private CartaPoderCargaService cartaCarga;
    @MockitoBean
    private ClienteService clientes;
    @MockitoBean
    private NumeracionDamService dam;
    @MockitoBean
    private IndicadoresService indicadores;
    @MockitoBean
    private TableroService tablero;
    @MockitoBean
    private TableroExcelService tableroExcel;
    @MockitoBean
    private CanalControlService canal;
    @MockitoBean
    private DespachoService despachoService;
    @MockitoBean
    private HitoDespachoService hitos;
    @MockitoBean
    private DocumentoDespachoService documentos;
    @MockitoBean
    private EmbarqueService embarques;
    @MockitoBean
    private LevanteService levante;
    @MockitoBean
    private LiquidacionService liquidacion;
    @MockitoBean
    private InspeccionService inspeccion;

    @BeforeEach
    void datos() {
        when(despachos.accesoDe(DESPACHO_A)).thenReturn(acceso(EMPRESA_A, EstadoDespacho.CANAL_ASIGNADO));
        when(despachos.accesoDe(DESPACHO_B)).thenReturn(acceso(EMPRESA_B, EstadoDespacho.CANAL_ASIGNADO));
        when(despachos.accesoDe(BORRADOR_A)).thenReturn(acceso(EMPRESA_A, EstadoDespacho.BORRADOR));
        when(despachos.accesoDe(INEXISTENTE)).thenReturn(Optional.empty());
        when(usuarios.clienteIdDe(50L)).thenReturn(Optional.of(EMPRESA_A));
        when(usuarios.clienteIdDe(51L)).thenReturn(Optional.empty());
    }

    @Test
    void clienteA_recorreTodasLasRutasConElDespachoDeB_ningunaLlegaAlServicio() throws Exception {
        List<String> recorridas = new ArrayList<>();
        for (Ruta ruta : rutasConDespacho()) {
            var resultado = mvc.perform(ruta.peticion(DESPACHO_B, usuarioA(), true)).andReturn().getResponse();
            String etiqueta = ruta.metodo() + " " + ruta.patron();
            assertThat(resultado.getStatus()).as(etiqueta)
                    .isEqualTo(ruta.metodo() == HttpMethod.GET ? 404 : 403);
            if (ruta.metodo() == HttpMethod.GET) {
                assertThat(resultado.getContentAsString()).as(etiqueta).contains("\"NO_ENCONTRADO\"")
                        .contains("Despacho no encontrado");
            }
            recorridas.add(etiqueta);
        }
        // Todas las rutas con despacho deben haberse probado (si se agrega una, entra sola en la prueba).
        assertThat(recorridas).hasSizeGreaterThanOrEqualTo(28);
        verifyNoInteractions(citas, clasificacion, items, dam, canal, despachoService, hitos, documentos, embarques,
                levante, liquidacion, inspeccion);
    }

    @Test
    void clienteA_recorreTodasLasRutasDeLaEmpresaB_ningunaLlegaAlServicio() throws Exception {
        int probadas = 0;
        for (Ruta ruta : rutasConCliente()) {
            var resultado = mvc.perform(ruta.peticion(EMPRESA_B, usuarioA(), true)).andReturn().getResponse();
            String etiqueta = ruta.metodo() + " " + ruta.patron();
            assertThat(resultado.getStatus()).as(etiqueta).isEqualTo(ruta.metodo() == HttpMethod.GET ? 404 : 403);
            probadas++;
        }
        assertThat(probadas).isGreaterThanOrEqualTo(6);
        verifyNoInteractions(cartaConsulta, cartaCarga);
        verify(clientes, never()).obtener(any());
        verify(clientes, never()).historial(any());
    }

    @Test
    void clienteA_tambienPierdeElDespachoInexistenteSinPistas_yElIntentoAjenoQuedaRegistrado() throws Exception {
        mvc.perform(get("/api/v1/despachos/" + INEXISTENTE + "/hitos").cookie(usuarioA()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Despacho no encontrado"));
        verifyNoInteractions(denegados);

        mvc.perform(get("/api/v1/despachos/" + DESPACHO_B + "/hitos").cookie(usuarioA()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Despacho no encontrado"));
        verify(denegados).registrar(50L, DESPACHO_B, EMPRESA_B, MotivoAccesoDenegado.OTRA_EMPRESA);
    }

    @Test
    void clienteA_leeLasRutasDeSuPropioDespacho() throws Exception {
        when(hitos.listar(DESPACHO_A)).thenReturn(List.of());

        mvc.perform(get("/api/v1/despachos/" + DESPACHO_A + "/hitos").cookie(usuarioA()))
                .andExpect(status().isOk());
        verifyNoInteractions(denegados);
    }

    @Test
    void clienteA_noVeSusBorradores() throws Exception {
        mvc.perform(get("/api/v1/despachos/" + BORRADOR_A).cookie(usuarioA()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Despacho no encontrado"));
        verify(denegados).registrar(50L, BORRADOR_A, EMPRESA_A, MotivoAccesoDenegado.BORRADOR);
        verifyNoInteractions(despachoService);
    }

    @Test
    void clienteSinEmpresa_noVeNingunDespachoYRecibeListasVacias() throws Exception {
        mvc.perform(get("/api/v1/despachos/" + DESPACHO_A).cookie(usuarioSinEmpresa()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Despacho no encontrado"));
        verify(denegados).registrar(51L, DESPACHO_A, EMPRESA_A, MotivoAccesoDenegado.SIN_EMPRESA);

        when(tablero.despachos(null, AccesoClienteService.SIN_RESULTADOS)).thenReturn(List.of());
        when(indicadores.indicadores(null, AccesoClienteService.SIN_RESULTADOS))
                .thenReturn(new IndicadoresResponse(0, 0, 0, 0));
        when(clientes.soloEmpresa(null)).thenReturn(new PaginaResponse<>(List.of(), 0, 50, 0, 0));

        mvc.perform(get("/api/v1/dashboard/despachos").cookie(usuarioSinEmpresa()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/dashboard/indicadores").cookie(usuarioSinEmpresa()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.despachosActivos").value(0));
        mvc.perform(get("/api/v1/clientes").cookie(usuarioSinEmpresa()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(0));
    }

    @Test
    void dashboard_elClienteSoloRecibeSuEmpresaAunqueFiltrePorOtra() throws Exception {
        when(tablero.despachos(null, EMPRESA_A)).thenReturn(List.of());
        when(indicadores.indicadores(118L, EMPRESA_A)).thenReturn(new IndicadoresResponse(1, 0, 0, 0));
        when(tableroExcel.generar(any())).thenReturn(new byte[] { 1 });

        mvc.perform(get("/api/v1/dashboard/despachos").param("clienteId", "2").cookie(usuarioA()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/dashboard/indicadores").param("aduanaId", "118").param("clienteId", "2")
                .cookie(usuarioA())).andExpect(status().isOk()).andExpect(jsonPath("$.despachosActivos").value(1));
        mvc.perform(get("/api/v1/dashboard/despachos/excel").param("clienteId", "2").cookie(usuarioA()))
                .andExpect(status().isOk());

        verify(tablero, never()).despachos(any(), eq(EMPRESA_B));
        verify(indicadores, never()).indicadores(any(), eq(EMPRESA_B));
    }

    @Test
    void clientes_elClienteSoloVeSuEmpresa_yNoUsaElPadronDeRuc() throws Exception {
        when(clientes.soloEmpresa(EMPRESA_A)).thenReturn(new PaginaResponse<>(
                List.of(new ClienteResumen(EMPRESA_A, "20787789324", "Importadora Andina SAC", null)), 0, 50, 1, 1));

        mvc.perform(get("/api/v1/clientes").param("q", "Pacifico").cookie(usuarioA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(EMPRESA_A));
        verify(clientes, never()).buscar(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());

        mvc.perform(get("/api/v1/clientes/existe").param("ruc", "20605544330").cookie(usuarioA()))
                .andExpect(status().isForbidden());
        verify(clientes, never()).existe(any());
    }

    @Test
    void agenteYAdmin_siguenViendoTodo() throws Exception {
        when(hitos.listar(DESPACHO_B)).thenReturn(List.of());
        when(tablero.despachos(null, null)).thenReturn(List.of());
        when(tablero.despachos(null, EMPRESA_B)).thenReturn(List.of());

        for (String rol : List.of("AGENTE", "ADMIN")) {
            Cookie sesion = sesion(new UsuarioAutenticado(2L, rol.toLowerCase(), rol, rol));
            mvc.perform(get("/api/v1/despachos/" + DESPACHO_B + "/hitos").cookie(sesion)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/dashboard/despachos").cookie(sesion)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/dashboard/despachos").param("clienteId", "2").cookie(sesion))
                    .andExpect(status().isOk());
        }
        verifyNoInteractions(denegados);
        verify(despachos, never()).accesoDe(any());
        verify(usuarios, never()).clienteIdDe(any());
    }

    // --- recorrido de rutas ---

    private List<Ruta> rutasConDespacho() {
        return rutas("/api/v1/despachos/{");
    }

    private List<Ruta> rutasConCliente() {
        return rutas("/api/v1/clientes/{");
    }

    private List<Ruta> rutas(String prefijo) {
        List<Ruta> rutas = new ArrayList<>();
        for (var e : mapeo.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = e.getKey();
            HandlerMethod ignorado = e.getValue();
            var patrones = new TreeSet<>(info.getPathPatternsCondition().getPatternValues());
            for (String patron : patrones) {
                if (!patron.startsWith(prefijo)) {
                    continue;
                }
                for (var metodo : info.getMethodsCondition().getMethods()) {
                    rutas.add(new Ruta(HttpMethod.valueOf(metodo.name()), patron));
                }
            }
        }
        return rutas;
    }

    private record Ruta(HttpMethod metodo, String patron) {

        /** El primer segmento variable lleva el id ajeno; los demás, un valor cualquiera. */
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion(long idAjeno,
                Cookie sesion, boolean conCsrf) {
            String url = patron;
            boolean primero = true;
            while (url.contains("{")) {
                int abre = url.indexOf('{');
                int cierra = url.indexOf('}', abre);
                url = url.substring(0, abre) + (primero ? idAjeno : "1") + url.substring(cierra + 1);
                primero = false;
            }
            var peticion = request(metodo, url).cookie(sesion).contentType("application/json").content("{}");
            return conCsrf ? peticion.with(csrf()) : peticion;
        }
    }

    // --- utilidades ---

    private static java.util.Optional<DespachoAcceso> acceso(long clienteId, EstadoDespacho estado) {
        return Optional.of(new DespachoAcceso() {
            @Override
            public Long getClienteId() {
                return clienteId;
            }

            @Override
            public EstadoDespacho getEstado() {
                return estado;
            }
        });
    }

    private Cookie usuarioA() {
        return sesion(new UsuarioAutenticado(50L, "jperez", "Jorge Pérez", "CLIENTE"));
    }

    private Cookie usuarioSinEmpresa() {
        return sesion(new UsuarioAutenticado(51L, "cliente", "Cliente Demo", "CLIENTE"));
    }

    private Cookie sesion(UsuarioAutenticado usuario) {
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, jwtService.generar(usuario, Duration.ofHours(2)));
    }
}
