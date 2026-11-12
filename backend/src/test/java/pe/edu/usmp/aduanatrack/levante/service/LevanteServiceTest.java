package pe.edu.usmp.aduanatrack.levante.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

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
import pe.edu.usmp.aduanatrack.levante.dto.LevanteRequest;
import pe.edu.usmp.aduanatrack.levante.dto.LevanteResponse;

/** Levante de HU-13 · T-13.1 y T-13.2 (SCRUM-86 y SCRUM-87); los comentarios CAn son los criterios de HU-13. */
class LevanteServiceTest {

    private static final Long DESPACHO = 5L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 13, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path carpeta;

    private DespachoRepository despachoRepository;
    private HitoDespachoService hitos;
    private Despacho despacho;
    private AvisoClienteService avisosCliente;
    private LevanteService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        hitos = mock(HitoDespachoService.class);
        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        despacho.asignarCanal(CanalControl.ROJO);
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_CONFORME);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        avisosCliente = mock(AvisoClienteService.class);
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        servicio = new LevanteService(despachoRepository, hitos, avisosCliente, reloj,
                new DocumentoProperties(carpeta.toString()));
    }

    private static LevanteRequest solicitud(boolean confirmar) {
        return new LevanteRequest("LEV-2026-000318", HOY, confirmar);
    }

    /** Despacho con el pago hecho y la inspección conforme: no falta ningún requisito. */
    private void conTodosLosRequisitos() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_CONFORME);
    }

    // CA1: con número y fecha, el despacho pasa a «Levante otorgado»

    @Test
    void conNumeroYFecha_elDespachoPasaALevanteOtorgado() {
        conTodosLosRequisitos();

        LevanteResponse respuesta = servicio.registrar(DESPACHO, solicitud(false), null);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LEVANTE_OTORGADO);
        assertThat(despacho.getNumeroLevante()).isEqualTo("LEV-2026-000318");
        assertThat(despacho.getFechaLevante()).isEqualTo(HOY);
        assertThat(despacho.isLevanteConAdvertencia()).isFalse();
        assertThat(respuesta.estadoDespacho()).isEqualTo(EstadoDespacho.LEVANTE_OTORGADO);
        assertThat(respuesta.conAdvertencia()).isFalse();
        assertThat(respuesta.retiroHabilitado()).isTrue();
        verify(hitos).registrar(DESPACHO, TipoHito.LEVANTE_OTORGADO, "Levante otorgado: LEV-2026-000318 (13/11/2026)");
        verify(hitos, never()).registrar(any(), eq(TipoHito.LEVANTE_CON_ADVERTENCIA), anyString());
        verify(despachoRepository).saveAndFlush(despacho);
        verify(avisosCliente).levanteOtorgado(despacho, "LEV-2026-000318", HOY);
    }

    @Test
    void canalVerdeConTributosPagados_noTieneRequisitosPendientes() {
        despacho.asignarCanal(CanalControl.VERDE);

        LevanteResponse respuesta = servicio.registrar(DESPACHO, solicitud(false), null);

        assertThat(respuesta.conAdvertencia()).isFalse();
        assertThat(respuesta.estadoDespacho()).isEqualTo(EstadoDespacho.LEVANTE_OTORGADO);
    }

    // CA2: sin inspección conforme responde 409 con el pendiente; con la confirmación guarda y deja el hito

    @Test
    void sinInspeccionConforme_responde409ConElPendiente() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_PROGRAMADA);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, solicitud(false), null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.LEVANTE_REQUISITOS_PENDIENTES);
                    assertThat(e.getDatos()).containsEntry("pendientes", List.of("Inspección conforme"));
                });
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_PROGRAMADA);
        assertThat(despacho.getNumeroLevante()).isNull();
        verify(despachoRepository, never()).saveAndFlush(any());
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void sinPagoNiInspeccion_laListaTraLosDosPendientes() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, solicitud(false), null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getDatos())
                        .containsEntry("pendientes", List.of("Pago de tributos", "Inspección conforme")));
    }

    @Test
    void conLaConfirmacion_guardaConAdvertenciaYDejaElHitoDeAdvertencia() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_PROGRAMADA);

        LevanteResponse respuesta = servicio.registrar(DESPACHO, solicitud(true), null);

        assertThat(respuesta.conAdvertencia()).isTrue();
        assertThat(despacho.isLevanteConAdvertencia()).isTrue();
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LEVANTE_OTORGADO);
        verify(hitos).registrar(DESPACHO, TipoHito.LEVANTE_OTORGADO, "Levante otorgado: LEV-2026-000318 (13/11/2026)");
        verify(hitos).registrar(DESPACHO, TipoHito.LEVANTE_CON_ADVERTENCIA,
                "Levante registrado con requisitos pendientes: Inspección conforme");
    }

    @Test
    void confirmarSinNada_equivaleAFalse() {
        despacho.cambiarEstado(EstadoDespacho.INSPECCION_PROGRAMADA);
        LevanteRequest sinConfirmacion = new LevanteRequest("LEV-1", HOY, null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, sinConfirmacion, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.LEVANTE_REQUISITOS_PENDIENTES));
    }

    // CA3: la constancia PDF queda guardada y se puede descargar

    @Test
    void constanciaPdf_quedaGuardadaYSePuedeDescargar() throws Exception {
        conTodosLosRequisitos();
        MockMultipartFile constancia = new MockMultipartFile("constancia", "constancia-318.pdf", "application/pdf", PDF);

        LevanteResponse respuesta = servicio.registrar(DESPACHO, solicitud(false), constancia);

        assertThat(respuesta.constanciaNombre()).isEqualTo("constancia-318.pdf");
        assertThat(despacho.getConstanciaLevanteRuta()).startsWith("5/constancia-levante/").endsWith(".pdf");
        var descargable = servicio.constancia(DESPACHO);
        assertThat(descargable.nombre()).isEqualTo("constancia-318.pdf");
        assertThat(Files.readAllBytes(descargable.ruta())).isEqualTo(PDF);
    }

    @Test
    void constanciaQueNoEsPdf_seRechazaSinGuardarNada() throws Exception {
        conTodosLosRequisitos();
        MockMultipartFile falso = new MockMultipartFile("constancia", "x.pdf", "application/pdf", "hola".getBytes());

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, solicitud(false), falso))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
                });
        assertThat(despacho.getNumeroLevante()).isNull();
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            assertThat(archivos.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    void sinConstancia_laDescargaResponde404() {
        conTodosLosRequisitos();
        servicio.registrar(DESPACHO, solicitud(false), null);

        assertThatThrownBy(() -> servicio.constancia(DESPACHO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void siFallaElGuardado_laConstanciaNoQuedaHuerfana() throws Exception {
        conTodosLosRequisitos();
        when(despachoRepository.saveAndFlush(despacho)).thenThrow(new DataIntegrityViolationException("duplicado"));
        MockMultipartFile constancia = new MockMultipartFile("constancia", "c.pdf", "application/pdf", PDF);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, solicitud(false), constancia))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.LEVANTE_DUPLICADO));
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            assertThat(archivos.filter(Files::isRegularFile)).isEmpty();
        }
    }

    // CA4: después del levante el servicio de citas acepta el retiro; aquí se verifica que quede habilitado

    @Test
    void obtener_informaQueElRetiroEstaHabilitadoSoloDespuesDelLevante() {
        assertThat(servicio.obtener(DESPACHO).retiroHabilitado()).isFalse();
        assertThat(servicio.obtener(DESPACHO).numeroLevante()).isNull();

        conTodosLosRequisitos();
        servicio.registrar(DESPACHO, solicitud(false), null);

        LevanteResponse registrado = servicio.obtener(DESPACHO);
        assertThat(registrado.retiroHabilitado()).isTrue();
        assertThat(registrado.numeroLevante()).isEqualTo("LEV-2026-000318");
        assertThat(registrado.fechaLevante()).isEqualTo(HOY);
    }

    // Validaciones y reglas de estado

    @Test
    void sinNumero_responde400ConElCampo() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, new LevanteRequest("  ", HOY, false), null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKey("numeroLevante");
                });
    }

    @Test
    void numeroConCaracteresNoPermitidos_responde400() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, new LevanteRequest("LEV 1;--", HOY, false), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("numeroLevante"));
    }

    @Test
    void fechaFutura_responde400() {
        LevanteRequest futura = new LevanteRequest("LEV-1", HOY.plusDays(1), false);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, futura, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("fechaLevante"));
    }

    @Test
    void levanteYaRegistrado_responde409() {
        conTodosLosRequisitos();
        servicio.registrar(DESPACHO, solicitud(false), null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, new LevanteRequest("LEV-OTRO", HOY, false), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.LEVANTE_YA_REGISTRADO));
    }

    @Test
    void numeroRepetidoEnOtroDespacho_responde409() {
        conTodosLosRequisitos();
        when(despachoRepository.existsByNumeroLevante("LEV-2026-000318")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, solicitud(false), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.LEVANTE_DUPLICADO));
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.INSPECCION_CONFORME);
    }

    @Test
    void despachoEnBorrador_responde409() {
        Despacho borrador = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        when(despachoRepository.findById(7L)).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> servicio.registrar(7L, solicitud(true), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR));
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.registrar(99L, solicitud(false), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(ApiException.class);
    }
}
