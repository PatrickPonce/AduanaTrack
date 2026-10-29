package pe.edu.usmp.aduanatrack.dam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.dam.dto.DamHistorialResponse;
import pe.edu.usmp.aduanatrack.dam.dto.DamResponse;
import pe.edu.usmp.aduanatrack.dam.entity.DamHistorial;
import pe.edu.usmp.aduanatrack.dam.repository.DamHistorialRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;

/** Rectificación del N° de DAM con historial (HU-09, regla de negocio 3 · SCRUM-67). */
class RectificacionDamServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDate FECHA_ORIGINAL = LocalDate.of(2026, 10, 26);
    private static final byte[] PDF = "%PDF-1.7\ncargo corregido\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path directorio;

    private DespachoRepository despachoRepository;
    private DamHistorialRepository historialRepository;
    private HitoDespachoService hitoService;
    private Usuario agente;
    private Despacho despacho;
    private NumeracionDamService servicio;

    @BeforeEach
    void preparar() {
        despachoRepository = mock(DespachoRepository.class);
        historialRepository = mock(DamHistorialRepository.class);
        hitoService = mock(HitoDespachoService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        despacho = new Despacho(new Cliente("20787789324", LocalDateTime.of(2026, 10, 1, 9, 0)), null,
                LocalDateTime.of(2026, 10, 1, 9, 0));
        despacho.abrir("DESP-2026-0001");
        despacho.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);
        despacho.registrarDam("118-2026-10-123456", FECHA_ORIGINAL, "cargo.pdf", "5/cargo-dam/original.pdf");

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(despachoRepository.saveAndFlush(any(Despacho.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);

        Clock reloj = Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new NumeracionDamService(despachoRepository, historialRepository, usuarioRepository, hitoService,
                reloj, new DocumentoProperties(directorio.toString()));
    }

    @Test
    void corregirElNumero_guardaElNuevoYDejaElAnteriorEnElHistorial() {
        DamResponse respuesta = servicio.rectificar(DESPACHO, " 118-2026-10-654321 ", null, "Error de digitación",
                null, AGENTE);

        assertThat(respuesta.numeroDam()).isEqualTo("118-2026-10-654321");
        assertThat(respuesta.fechaNumeracion()).isEqualTo(FECHA_ORIGINAL);
        assertThat(respuesta.estado()).isEqualTo("DAM_NUMERADA");

        ArgumentCaptor<DamHistorial> captura = ArgumentCaptor.forClass(DamHistorial.class);
        verify(historialRepository).save(captura.capture());
        DamHistorial registro = captura.getValue();
        assertThat(registro.getNumeroAnterior()).isEqualTo("118-2026-10-123456");
        assertThat(registro.getNumeroNuevo()).isEqualTo("118-2026-10-654321");
        assertThat(registro.getFechaAnterior()).isEqualTo(FECHA_ORIGINAL);
        assertThat(registro.getMotivo()).isEqualTo("Error de digitación");
        assertThat(registro.getRectificadoPor()).isSameAs(agente);
        assertThat(registro.getRectificadoEn()).isEqualTo(LocalDateTime.of(2026, 10, 28, 10, 0));
    }

    @Test
    void corregirElNumero_registraElHitoEnLaLineaDeTiempo() {
        servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "Error de digitación", null, AGENTE);

        verify(hitoService).registrar(DESPACHO, TipoHito.DAM_RECTIFICADA,
                "DAM rectificada: 118-2026-10-123456 → 118-2026-10-654321");
    }

    @Test
    void corregirSoloLaFecha_conservaElNumero() {
        DamResponse respuesta = servicio.rectificar(DESPACHO, "118-2026-10-123456", "2026-10-27", "Fecha mal digitada",
                null, AGENTE);

        assertThat(respuesta.numeroDam()).isEqualTo("118-2026-10-123456");
        assertThat(respuesta.fechaNumeracion()).isEqualTo(LocalDate.of(2026, 10, 27));
        verify(hitoService).registrar(DESPACHO, TipoHito.DAM_RECTIFICADA,
                "Datos de la DAM rectificados: 118-2026-10-123456");
    }

    @Test
    void conCargoNuevo_reemplazaElCargoYGuardaElArchivo() {
        servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "Cargo corregido",
                new MockMultipartFile("cargo", "Cargo nuevo.pdf", "application/pdf", PDF), AGENTE);

        assertThat(despacho.getCargoNombre()).isEqualTo("Cargo nuevo.pdf");
        assertThat(despacho.getCargoRuta()).startsWith("5/cargo-dam/").endsWith(".pdf")
                .isNotEqualTo("5/cargo-dam/original.pdf");
    }

    @Test
    void sinCargoNuevo_conservaElCargoAnterior() {
        servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "Error de digitación", null, AGENTE);

        assertThat(despacho.getCargoNombre()).isEqualTo("cargo.pdf");
        assertThat(despacho.getCargoRuta()).isEqualTo("5/cargo-dam/original.pdf");
    }

    @Test
    void sinMotivo_responde400ConElCampoMarcadoYNoGuarda() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "  ", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKey("motivo");
                });
        verify(historialRepository, never()).save(any());
        assertThat(despacho.getNumeroDam()).isEqualTo("118-2026-10-123456");
    }

    @Test
    void motivoMuyLargo_responde400() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "x".repeat(201), null,
                AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("motivo"));
    }

    @Test
    void numeroVacioOInvalido_responde400() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "", null, "Error", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("numeroDam"));
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118 2026 #1", null, "Error", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("numeroDam"));
    }

    @Test
    void fechaFuturaOInvalida_responde400() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-654321", "2026-10-29", "Error", null,
                AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("fechaNumeracion"));
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-654321", "27/10/2026", "Error", null,
                AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsKey("fechaNumeracion"));
    }

    @Test
    void sinNingunCambio_responde400() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-123456", "2026-10-26", "Error", null,
                AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKey("numeroDam");
                });
        verify(historialRepository, never()).save(any());
    }

    @Test
    void despachoSinDamRegistrada_responde409() {
        Despacho sinDam = new Despacho(new Cliente("20787789324", LocalDateTime.of(2026, 10, 1, 9, 0)), null,
                LocalDateTime.of(2026, 10, 1, 9, 0));
        sinDam.abrir("DESP-2026-0002");
        sinDam.cambiarEstado(EstadoDespacho.TRIBUTOS_PAGADOS);
        when(despachoRepository.findById(6L)).thenReturn(Optional.of(sinDam));

        assertThatThrownBy(() -> servicio.rectificar(6L, "118-2026-10-654321", null, "Error", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DAM_NO_REGISTRADA);
                });
    }

    @Test
    void numeroQueYaTieneOtroDespacho_responde409SinCambiar() {
        when(despachoRepository.existsByNumeroDam("118-2026-10-999999")).thenReturn(true);

        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-999999", null, "Error", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DAM_DUPLICADA));
        assertThat(despacho.getNumeroDam()).isEqualTo("118-2026-10-123456");
        verify(historialRepository, never()).save(any());
    }

    @Test
    void conservarElMismoNumero_noSeConsideraDuplicado() {
        servicio.rectificar(DESPACHO, "118-2026-10-123456", "2026-10-27", "Fecha corregida", null, AGENTE);

        verify(despachoRepository, never()).existsByNumeroDam(any());
    }

    @Test
    void cargoQueNoEsPdf_responde415SinCambiarLaDam() {
        assertThatThrownBy(() -> servicio.rectificar(DESPACHO, "118-2026-10-654321", null, "Error",
                new MockMultipartFile("cargo", "foto.pdf", "application/pdf", "no soy un pdf".getBytes()), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        assertThat(despacho.getNumeroDam()).isEqualTo("118-2026-10-123456");
    }

    @Test
    void despachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.rectificar(99L, "118-2026-10-654321", null, "Error", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void historial_devuelveLasRectificacionesConElNombreDelUsuario() {
        DamHistorial registro = new DamHistorial(DESPACHO, "118-2026-10-123456", FECHA_ORIGINAL, "118-2026-10-654321",
                FECHA_ORIGINAL, "Error de digitación", agente, LocalDateTime.of(2026, 10, 28, 10, 0));
        when(historialRepository.findByDespachoIdOrderByRectificadoEnDescIdDesc(DESPACHO))
                .thenReturn(List.of(registro));

        List<DamHistorialResponse> historial = servicio.historial(DESPACHO);

        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).numeroAnterior()).isEqualTo("118-2026-10-123456");
        assertThat(historial.get(0).numeroNuevo()).isEqualTo("118-2026-10-654321");
        assertThat(historial.get(0).motivo()).isEqualTo("Error de digitación");
        assertThat(historial.get(0).usuario()).isEqualTo("Agente Demo");
    }

    @Test
    void historialDeDespachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.historial(99L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(historialRepository, never()).findByDespachoIdOrderByRectificadoEnDescIdDesc(eq(99L));
    }
}
