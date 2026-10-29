package pe.edu.usmp.aduanatrack.dam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.dam.dto.DamResponse;
import pe.edu.usmp.aduanatrack.dam.repository.DamHistorialRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;

/** Un grupo de pruebas por criterio de aceptación de HU-09 (T-09.3 · SCRUM-69) más los casos de borde. */
class NumeracionDamServiceTest {

    private static final Long DESPACHO = 5L;
    private static final byte[] PDF = "%PDF-1.7\ncargo de numeración de prueba\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path directorio;

    private DespachoRepository despachoRepository;
    private HitoDespachoService hitoService;
    private Despacho despacho;
    private NumeracionDamService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        hitoService = mock(HitoDespachoService.class);

        despacho = new Despacho(new Cliente("20787789324", java.time.LocalDateTime.of(2026, 10, 1, 9, 0)), null,
                java.time.LocalDateTime.of(2026, 10, 1, 9, 0));
        despacho.abrir("DESP-2026-0001");
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);
        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(despachoRepository.saveAndFlush(any(Despacho.class))).thenAnswer(i -> i.getArgument(0));

        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new NumeracionDamService(despachoRepository, mock(DamHistorialRepository.class),
                mock(UsuarioRepository.class), hitoService, reloj, new DocumentoProperties(directorio.toString()));
    }

    // CA1: guarda el número junto con la fecha de numeración

    @Test
    void numeroValido_quedaGuardadoConLaFechaIndicada() {
        DamResponse respuesta = servicio.registrar(DESPACHO, "118-2026-10-123456", "2026-10-27", null);

        assertThat(respuesta.numeroDam()).isEqualTo("118-2026-10-123456");
        assertThat(respuesta.fechaNumeracion()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(despacho.getNumeroDam()).isEqualTo("118-2026-10-123456");
        assertThat(despacho.getFechaNumeracion()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    void sinFecha_usaLaFechaDeHoy() {
        DamResponse respuesta = servicio.registrar(DESPACHO, "  118-2026-10-123456 ", null, null);

        assertThat(respuesta.numeroDam()).isEqualTo("118-2026-10-123456");
        assertThat(respuesta.fechaNumeracion()).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    void fechaFutura_responde400SinGuardar() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-123456", "2026-10-29", null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKey("fechaNumeracion");
                });
        assertThat(despacho.getNumeroDam()).isNull();
    }

    @Test
    void fechaConFormatoInvalido_responde400() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-123456", "28/10/2026", null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("fechaNumeracion"));
    }

    // CA2: el campo N° de DAM vacío no permite guardar

    @Test
    void numeroVacioONulo_responde400ConElCampoMarcadoYNoGuarda() {
        Stream.of("", "   ", null).forEach(vacio -> assertThatThrownBy(
                () -> servicio.registrar(DESPACHO, vacio, null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.VALIDACION);
                    assertThat(e.getCampos()).containsEntry("numeroDam", "Ingrese el N° de DAM");
                }));

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.TRIBUTOS_PAGADOS);
        verify(despachoRepository, never()).saveAndFlush(any());
        verify(hitoService, never()).registrar(any(), any(), any());
    }

    @Test
    void numeroConCaracteresNoPermitidosOMuyLargo_responde400() {
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118 2026 <script>", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("numeroDam"));
        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "1".repeat(31), null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("numeroDam"));
    }

    // CA3: el estado del despacho cambia a «DAM numerada»

    @Test
    void registroExitoso_cambiaElEstadoARegistraElHito() {
        DamResponse respuesta = servicio.registrar(DESPACHO, "118-2026-10-123456", null, null);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DAM_NUMERADA);
        assertThat(respuesta.estado()).isEqualTo("DAM_NUMERADA");
        verify(hitoService).registrar(eq(DESPACHO), eq(TipoHito.DAM_NUMERADA), eq("DAM numerada: 118-2026-10-123456"));
    }

    @Test
    void sinTributosPagados_responde409YNoCambiaElEstado() {
        despacho.cambiarEstado(EstadoDespacho.LIQUIDADO);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-123456", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.TRIBUTOS_PENDIENTES);
                });
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.LIQUIDADO);
        assertThat(despacho.getNumeroDam()).isNull();
    }

    @Test
    void despachoYaNumerado_noSePuedeRegistrarOtraVez() {
        servicio.registrar(DESPACHO, "118-2026-10-123456", null, null);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-999999", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.TRIBUTOS_PENDIENTES);
                    assertThat(e.getMessage()).contains("ya está registrada");
                });
        assertThat(despacho.getNumeroDam()).isEqualTo("118-2026-10-123456");
    }

    @Test
    void numeroYaUsadoEnOtroDespacho_responde409() {
        when(despachoRepository.existsByNumeroDam("118-2026-10-123456")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-123456", null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DAM_DUPLICADA);
                });
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.TRIBUTOS_PAGADOS);
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.registrar(99L, "118-2026-10-123456", null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // CA4: el cargo de numeración en PDF se guarda junto al registro

    @Test
    void cargoPdf_quedaEnElVolumenYSuNombreEnElDespacho() throws IOException {
        DamResponse respuesta = servicio.registrar(DESPACHO, "118-2026-10-123456", null,
                new MockMultipartFile("cargo", "Cargo DAM.pdf", "application/pdf", PDF));

        assertThat(respuesta.nombreCargo()).isEqualTo("Cargo DAM.pdf");
        assertThat(despacho.getCargoRuta()).matches("5/cargo-dam/[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(directorio.resolve(despacho.getCargoRuta()))).isEqualTo(PDF);
    }

    @Test
    void cargoOpcional_sinArchivoElRegistroSeGuardaIgual() {
        DamResponse respuesta = servicio.registrar(DESPACHO, "118-2026-10-123456", null,
                new MockMultipartFile("cargo", "", "application/pdf", new byte[0]));

        assertThat(respuesta.nombreCargo()).isNull();
        assertThat(despacho.getCargoRuta()).isNull();
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DAM_NUMERADA);
    }

    @Test
    void cargoQueNoEsPdf_responde415YNoRegistraLaDam() {
        byte[] docx = { 'P', 'K', 3, 4, 20, 0, 6, 0 };

        assertThatThrownBy(() -> servicio.registrar(DESPACHO, "118-2026-10-123456", null,
                new MockMultipartFile("cargo", "cargo.pdf", "application/pdf", docx)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
                });
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.TRIBUTOS_PAGADOS);
        assertThat(despacho.getNumeroDam()).isNull();
    }

    @Test
    void cargoDescargable_soloSiSeAdjunto() throws IOException {
        servicio.registrar(DESPACHO, "118-2026-10-123456", null,
                new MockMultipartFile("cargo", "Cargo DAM.pdf", "application/pdf", PDF));

        NumeracionDamService.ArchivoCargo archivo = servicio.cargo(DESPACHO);

        assertThat(archivo.nombre()).isEqualTo("Cargo DAM.pdf");
        assertThat(Files.readAllBytes(archivo.ruta())).isEqualTo(PDF);
    }

    @Test
    void cargoSinAdjuntar_responde404() {
        servicio.registrar(DESPACHO, "118-2026-10-123456", null, null);

        assertThatThrownBy(() -> servicio.cargo(DESPACHO)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void obtenerSinRegistrar_devuelveLosCamposNulosYElEstado() {
        DamResponse respuesta = servicio.obtener(DESPACHO);

        assertThat(respuesta.numeroDam()).isNull();
        assertThat(respuesta.fechaNumeracion()).isNull();
        assertThat(respuesta.estado()).isEqualTo("TRIBUTOS_PAGADOS");
    }
}
