package pe.edu.usmp.aduanatrack.inspeccion.service;

import pe.edu.usmp.aduanatrack.notificacion.service.AvisoClienteService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionRequest;
import pe.edu.usmp.aduanatrack.inspeccion.dto.InspeccionResponse;
import pe.edu.usmp.aduanatrack.inspeccion.entity.InspeccionDespacho;
import pe.edu.usmp.aduanatrack.inspeccion.entity.ResultadoInspeccion;
import pe.edu.usmp.aduanatrack.inspeccion.repository.InspeccionDespachoRepository;
import pe.edu.usmp.aduanatrack.notificacion.entity.Severidad;
import pe.edu.usmp.aduanatrack.notificacion.entity.TipoNotificacion;
import pe.edu.usmp.aduanatrack.notificacion.service.NotificacionService;

/** Inspección de HU-12 · T-12.1 y T-12.2; los comentarios CAn son los criterios de aceptación de HU-12. */
class InspeccionServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long USUARIO = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 10, 30);
    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path carpeta;

    private DespachoRepository despachoRepository;
    private InspeccionDespachoRepository inspeccionRepository;
    private UsuarioRepository usuarioRepository;
    private HitoDespachoService hitos;
    private NotificacionService notificaciones;
    private Despacho despacho;
    private AvisoClienteService avisosCliente;
    private InspeccionService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        inspeccionRepository = mock(InspeccionDespachoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        hitos = mock(HitoDespachoService.class);
        notificaciones = mock(NotificacionService.class);
        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.asignarCanal(CanalControl.ROJO);
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_PROGRAMADA);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        Usuario agente = mock(Usuario.class);
        when(agente.getNombre()).thenReturn("Agente Demo");
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(agente);
        avisosCliente = mock(AvisoClienteService.class);
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new InspeccionService(despachoRepository, inspeccionRepository, usuarioRepository, hitos,
                notificaciones, avisosCliente, reloj, new DocumentoProperties(carpeta.toString()));
    }

    private static List<InspeccionRequest.Item> todosConformes() {
        return new ArrayList<>(InspeccionService.ITEMS.stream()
                .map(d -> new InspeccionRequest.Item(d, true, null)).toList());
    }

    private static InspeccionRequest solicitud(List<InspeccionRequest.Item> items) {
        return new InspeccionRequest(items, null);
    }

    private static List<InspeccionRequest.Item> conUnNoConforme(String observacion) {
        List<InspeccionRequest.Item> items = todosConformes();
        items.set(4, new InspeccionRequest.Item("Rotulado y marcas", false, observacion));
        return items;
    }

    private static void esperaValidacion(Runnable accion, String campo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getCampos()).containsKey(campo);
        });
    }

    // CA1: el resultado lo calcula el sistema a partir de los ítems

    @Test
    void todosLosItemsConformes_elResultadoEsConforme() {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);

        assertThat(respuesta.resultado()).isEqualTo(ResultadoInspeccion.CONFORME);
        assertThat(respuesta.items()).hasSize(InspeccionService.ITEMS.size());
        assertThat(respuesta.items()).extracting(InspeccionResponse.Item::descripcion)
                .containsExactlyElementsOf(InspeccionService.ITEMS);
        assertThat(respuesta.registradaPor()).isEqualTo("Agente Demo");
        verify(inspeccionRepository).saveAndFlush(any(InspeccionDespacho.class));
    }

    @Test
    void unItemNoConforme_elResultadoEsConObservaciones() {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO,
                solicitud(conUnNoConforme("Rotulado incompleto")), null);

        assertThat(respuesta.resultado()).isEqualTo(ResultadoInspeccion.CON_OBSERVACIONES);
        assertThat(respuesta.items().get(4).observacion()).isEqualTo("Rotulado incompleto");
        assertThat(respuesta.items().get(0).observacion()).isNull();
    }

    @Test
    void laObservacionDeUnItemConformeSeDescarta() {
        List<InspeccionRequest.Item> items = todosConformes();
        items.set(0, new InspeccionRequest.Item("Cantidad de bultos", true, "no debería guardarse"));

        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO, solicitud(items), null);

        assertThat(respuesta.items().get(0).observacion()).isNull();
    }

    // CA2: con observaciones el estado no cambia, queda el hito y se avisa al agente

    @Test
    void conObservaciones_mantieneElEstadoRegistraElHitoYAvisaAlAgente() {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO,
                solicitud(conUnNoConforme("Rotulado incompleto")), null);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        assertThat(respuesta.estadoDespacho()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        assertThat(respuesta.levanteHabilitado()).isFalse();
        verify(hitos).registrar(DESPACHO, TipoHito.INSPECCION_CON_OBSERVACIONES,
                "Inspección con observaciones: Rotulado y marcas (Rotulado incompleto)");
        verify(notificaciones).notificarAgente(despacho, TipoNotificacion.INSPECCION_CON_OBSERVACIONES,
                Severidad.ALTA, "Inspección con observaciones",
                "DESP-2026-0001: Rotulado y marcas (Rotulado incompleto)");
        verify(hitos, never()).registrar(any(), eq(TipoHito.INSPECCION_CONFORME), anyString());
        verify(avisosCliente).inspeccionConObservaciones(despacho, "Rotulado y marcas (Rotulado incompleto)");
        verify(avisosCliente, never()).inspeccionConforme(any());
    }

    @Test
    void conObservaciones_sePuedeRepetirLaInspeccion() {
        servicio.registrar(DESPACHO, USUARIO, solicitud(conUnNoConforme("Rotulado incompleto")), null);

        InspeccionResponse segunda = servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);

        assertThat(segunda.resultado()).isEqualTo(ResultadoInspeccion.CONFORME);
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_CONFORME);
    }

    @Test
    void itemNoConformeSinObservacion_responde400() {
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(conUnNoConforme("   ")), null),
                "item-4");
        verify(inspeccionRepository, never()).saveAndFlush(any());
    }

    // CA3: conforme pasa a INSPECCION_CONFORME y habilita el levante

    @Test
    void conforme_pasaAInspeccionConformeYHabilitaElLevante() {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_CONFORME);
        assertThat(respuesta.estadoDespacho()).isEqualTo(EstadoDespacho.INSPECCION_CONFORME);
        assertThat(respuesta.levanteHabilitado()).isTrue();
        verify(despachoRepository).saveAndFlush(despacho);
        verify(hitos).registrar(DESPACHO, TipoHito.INSPECCION_CONFORME,
                "Inspección conforme: el levante queda habilitado.");
        verify(notificaciones).notificarAgente(despacho, TipoNotificacion.INSPECCION_CONFORME, Severidad.INFO,
                "Inspección conforme", "DESP-2026-0001: el levante quedó habilitado.");
        verify(hitos, never()).registrar(any(), eq(TipoHito.INSPECCION_CON_OBSERVACIONES), anyString());
        verify(avisosCliente).inspeccionConforme(despacho);
        verify(avisosCliente, never()).inspeccionConObservaciones(any(), anyString());
    }

    // CA4: el acta en PDF es opcional

    @Test
    void sinActa_seRegistraIgual() {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);

        assertThat(respuesta.actaNombre()).isNull();
    }

    @Test
    void conActaPdf_seGuardaEnElDirectorioDeDocumentos() throws Exception {
        InspeccionResponse respuesta = servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()),
                new MockMultipartFile("acta", "acta 123.pdf", "application/pdf", PDF));

        assertThat(respuesta.actaNombre()).isEqualTo("acta 123.pdf");
        ArgumentCaptor<InspeccionDespacho> guardada = ArgumentCaptor.forClass(InspeccionDespacho.class);
        verify(inspeccionRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getActaRuta()).startsWith("5/acta-inspeccion/").endsWith(".pdf");
        try (Stream<Path> archivos = Files.walk(carpeta.resolve("5").resolve("acta-inspeccion"))) {
            assertThat(archivos.filter(Files::isRegularFile)).hasSize(1);
        }
    }

    @Test
    void actaQueNoEsPdf_responde415YNoRegistraNada() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()),
                new MockMultipartFile("acta", "acta.pdf", "application/pdf", "no es un pdf".getBytes())))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
                });

        verify(inspeccionRepository, never()).saveAndFlush(any());
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
    }

    @Test
    void siFallaElGuardado_elActaNoQuedaHuerfana() throws Exception {
        when(inspeccionRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()),
                new MockMultipartFile("acta", "acta.pdf", "application/pdf", PDF)))
                .isInstanceOf(IllegalStateException.class);

        try (Stream<Path> archivos = Files.walk(carpeta)) {
            assertThat(archivos.filter(Files::isRegularFile)).isEmpty();
        }
    }

    // Estado del despacho

    @Test
    void sinCitaDeInspeccion_responde409() {
        despacho.cambiarEstado(EstadoDespacho.CANAL_ASIGNADO);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.INSPECCION_NO_PROGRAMADA);
                });
        verify(inspeccionRepository, never()).saveAndFlush(any());
    }

    @Test
    void yaConforme_responde409() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_CONFORME);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.INSPECCION_NO_PROGRAMADA));
    }

    @Test
    void despachoInexistente_responde404() {
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(99L, USUARIO, solicitud(todosConformes()), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // Validación del checklist

    @Test
    void sinItems_responde400() {
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(List.of()), null), "items");
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, null, null), "items");
    }

    @Test
    void faltanItemsDelCatalogo_responde400() {
        List<InspeccionRequest.Item> items = todosConformes();
        items.remove(0);

        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(items), null), "items");
    }

    @Test
    void itemRepetidoOFueraDelCatalogo_responde400() {
        List<InspeccionRequest.Item> repetido = todosConformes();
        repetido.set(1, new InspeccionRequest.Item("Cantidad de bultos", true, null));
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(repetido), null), "item-1");

        List<InspeccionRequest.Item> ajeno = todosConformes();
        ajeno.set(2, new InspeccionRequest.Item("Color del contenedor", true, null));
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(ajeno), null), "item-2");
    }

    @Test
    void itemSinIndicarSiEsConforme_responde400() {
        List<InspeccionRequest.Item> items = todosConformes();
        items.set(3, new InspeccionRequest.Item("Estado y embalaje de la mercancía", null, null));

        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO, solicitud(items), null), "item-3");
    }

    @Test
    void observacionesQueSePasanDeLosMaximos_responden400() {
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO,
                solicitud(conUnNoConforme("x".repeat(256))), null), "item-4");
        esperaValidacion(() -> servicio.registrar(DESPACHO, USUARIO,
                new InspeccionRequest(todosConformes(), "x".repeat(501)), null), "observaciones");
    }

    // Consulta

    @Test
    void sinInspecciones_laConsultaResponde404() {
        when(inspeccionRepository.ultima(DESPACHO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.ultima(DESPACHO)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void laConsultaDevuelveLaUltimaConElEstadoActualDelDespacho() {
        servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);
        ArgumentCaptor<InspeccionDespacho> guardada = ArgumentCaptor.forClass(InspeccionDespacho.class);
        verify(inspeccionRepository).saveAndFlush(guardada.capture());
        when(inspeccionRepository.ultima(DESPACHO)).thenReturn(Optional.of(guardada.getValue()));

        InspeccionResponse respuesta = servicio.ultima(DESPACHO);

        assertThat(respuesta.resultado()).isEqualTo(ResultadoInspeccion.CONFORME);
        assertThat(respuesta.levanteHabilitado()).isTrue();
    }

    @Test
    void elActaSoloSeDescargaSiExiste() throws Exception {
        servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()),
                new MockMultipartFile("acta", "acta.pdf", "application/pdf", PDF));
        ArgumentCaptor<InspeccionDespacho> guardada = ArgumentCaptor.forClass(InspeccionDespacho.class);
        verify(inspeccionRepository).saveAndFlush(guardada.capture());
        when(inspeccionRepository.ultima(DESPACHO)).thenReturn(Optional.of(guardada.getValue()));

        InspeccionService.ArchivoActa acta = servicio.acta(DESPACHO);

        assertThat(acta.nombre()).isEqualTo("acta.pdf");
        assertThat(Files.readAllBytes(acta.ruta())).isEqualTo(PDF);
    }

    @Test
    void sinActa_ladescargaResponde404() {
        servicio.registrar(DESPACHO, USUARIO, solicitud(todosConformes()), null);
        ArgumentCaptor<InspeccionDespacho> guardada = ArgumentCaptor.forClass(InspeccionDespacho.class);
        verify(inspeccionRepository).saveAndFlush(guardada.capture());
        when(inspeccionRepository.ultima(DESPACHO)).thenReturn(Optional.of(guardada.getValue()));

        assertThatThrownBy(() -> servicio.acta(DESPACHO)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
