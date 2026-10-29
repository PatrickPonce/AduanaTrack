package pe.edu.usmp.aduanatrack.documento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoRequeridoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentosDespachoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumentacion;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumento;
import pe.edu.usmp.aduanatrack.documento.dto.VersionDocumentoResponse;
import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;
import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;
import pe.edu.usmp.aduanatrack.documento.repository.DocumentoDespachoRepository;
import pe.edu.usmp.aduanatrack.documento.repository.TipoDocumentoRepository;

import org.mockito.Mockito;

/**
 * Pruebas de aceptación de HU-06 · T-06.5 (SCRUM-57): una por criterio, con los servicios reales
 * de documentos y de estado general, y repositorios en memoria que conservan lo guardado.
 */
class Hu06CriteriosAceptacionTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 24, 11, 0);
    private static final byte[] PDF = "%PDF-1.7\ndocumento de prueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    /** Un .docx es un ZIP: empieza con «PK». */
    private static final byte[] DOCX = { 'P', 'K', 3, 4, 20, 0, 6, 0 };

    @TempDir
    Path directorio;

    private final List<DocumentoDespacho> guardados = new ArrayList<>();
    private HitoDespachoService hitos;
    private Despacho despacho;
    private DocumentoDespachoService documentos;
    private DocumentacionDespachoService documentacion;

    @BeforeEach
    void preparar() {
        DocumentoDespachoRepository documentoRepository = mock(DocumentoDespachoRepository.class);
        TipoDocumentoRepository tipoRepository = mock(TipoDocumentoRepository.class);
        DespachoRepository despachoRepository = mock(DespachoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        hitos = mock(HitoDespachoService.class);

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        Usuario agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash",
                new Rol("AGENTE", "Agente"));
        List<TipoDocumento> tipos = List.of(
                tipo(1L, "FACTURA_COMERCIAL", "Factura comercial", true, 1),
                tipo(2L, "CONOCIMIENTO_EMBARQUE", "Conocimiento de embarque (BL)", true, 2),
                tipo(3L, "LISTA_EMPAQUE", "Lista de empaque", true, 3),
                tipo(4L, "POLIZA_SEGURO", "Póliza de seguro", true, 4),
                tipo(5L, "CERTIFICADO_ORIGEN", "Certificado de origen", false, 5));

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);
        when(tipoRepository.findByActivoTrueOrderByOrden()).thenReturn(tipos);
        tipos.forEach(t -> when(tipoRepository.findByCodigoAndActivoTrue(t.getCodigo())).thenReturn(Optional.of(t)));

        // Repositorio en memoria: lo que se guarda es lo que después se consulta.
        when(documentoRepository.saveAndFlush(any(DocumentoDespacho.class))).thenAnswer(i -> {
            DocumentoDespacho d = i.getArgument(0);
            if (!guardados.contains(d)) {
                ReflectionTestUtils.setField(d, "id", 100L + guardados.size());
                guardados.add(d);
            }
            return d;
        });
        when(documentoRepository.vigentes(DESPACHO)).thenAnswer(i -> guardados.stream()
                .filter(DocumentoDespacho::isVigente).toList());
        when(documentoRepository.findByDespachoIdAndTipoIdAndVigenteTrue(any(), any())).thenAnswer(i -> guardados
                .stream().filter(d -> d.isVigente() && d.getTipo().getId().equals(i.getArgument(1))).findFirst());
        when(documentoRepository.ultimaVersion(any(), any())).thenAnswer(i -> guardados.stream()
                .filter(d -> d.getTipo().getId().equals(i.getArgument(1)))
                .mapToInt(DocumentoDespacho::getVersion).max().orElse(0));
        when(documentoRepository.versiones(any(), any())).thenAnswer(i -> guardados.stream()
                .filter(d -> d.getTipo().getId().equals(i.getArgument(1)))
                .sorted(Comparator.comparingInt(DocumentoDespacho::getVersion).reversed()).toList());

        documentacion = new DocumentacionDespachoService(documentoRepository, tipoRepository, despachoRepository,
                hitos);
        Clock reloj = Clock.fixed(Instant.parse("2026-10-24T16:00:00Z"), ZoneId.of("America/Lima"));
        documentos = new DocumentoDespachoService(documentoRepository, tipoRepository, despachoRepository,
                usuarioRepository, documentacion, reloj, new DocumentoProperties(directorio.toString()));
    }

    @Test
    void criterio1_unPdfDeMenosDe10MbQuedaCargado_unoDe11MbOUnDocxSeRechaza() {
        documentos.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("factura.pdf", PDF), AGENTE);

        assertThat(fila("FACTURA_COMERCIAL").estado()).isEqualTo(EstadoDocumento.CARGADO);

        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(11L * 1024 * 1024);
        assertThatThrownBy(() -> documentos.cargar(DESPACHO, "LISTA_EMPAQUE", grande, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.ARCHIVO_MUY_GRANDE);
                });
        assertThatThrownBy(() -> documentos.cargar(DESPACHO, "LISTA_EMPAQUE", pdf("lista.pdf", DOCX), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
                });
        assertThat(fila("LISTA_EMPAQUE").estado()).isEqualTo(EstadoDocumento.PENDIENTE);
        assertThat(guardados).hasSize(1);
    }

    @Test
    void criterio2_conUnObligatorioPendiente_clasificarResponde409YLoNombra() {
        cargarObligatorios("FACTURA_COMERCIAL", "CONOCIMIENTO_EMBARQUE", "LISTA_EMPAQUE");

        assertThatThrownBy(() -> documentacion.exigirDocumentacionCompleta(DESPACHO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DOCUMENTACION_INCOMPLETA);
                    assertThat(e.getDatos()).containsEntry("pendientes", List.of("POLIZA_SEGURO"));
                    assertThat(e.getMessage()).contains("Póliza de seguro");
                });
    }

    @Test
    void criterio3_alReemplazar_existenDosVersionesYSoloLaNuevaEstaVigente() {
        documentos.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("factura-v1.pdf", PDF), AGENTE);
        documentos.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("factura-v2.pdf", PDF), AGENTE);

        List<VersionDocumentoResponse> versiones = documentos.versiones(DESPACHO, "FACTURA_COMERCIAL");

        assertThat(versiones).hasSize(2);
        assertThat(versiones.get(0).version()).isEqualTo(2);
        assertThat(versiones.get(0).vigente()).isTrue();
        assertThat(versiones.get(0).archivo()).isEqualTo("factura-v2.pdf");
        assertThat(versiones.get(1).version()).isEqualTo(1);
        assertThat(versiones.get(1).vigente()).isFalse();
        assertThat(versiones.get(1).archivo()).isEqualTo("factura-v1.pdf");
    }

    @Test
    void criterio4_conTodosLosObligatorios_elEstadoGeneralEsCompletoYElDespachoPasaADocumentacionCompleta() {
        cargarObligatorios("FACTURA_COMERCIAL", "CONOCIMIENTO_EMBARQUE", "LISTA_EMPAQUE");
        assertThat(documentos.listar(DESPACHO).estadoGeneral()).isEqualTo(EstadoDocumentacion.PENDIENTE);
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.APERTURA);

        documentos.cargar(DESPACHO, "POLIZA_SEGURO", pdf("poliza.pdf", PDF), AGENTE);

        DocumentosDespachoResponse respuesta = documentos.listar(DESPACHO);
        assertThat(respuesta.estadoGeneral()).isEqualTo(EstadoDocumentacion.COMPLETO);
        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DOCUMENTACION_COMPLETA);
        Mockito.verify(hitos).registrar(Mockito.eq(DESPACHO), Mockito.eq(TipoHito.DOCUMENTACION_COMPLETA),
                Mockito.anyString());
        // Y ya no bloquea la clasificación.
        documentacion.exigirDocumentacionCompleta(DESPACHO);
    }

    private void cargarObligatorios(String... tipos) {
        for (String tipo : tipos) {
            documentos.cargar(DESPACHO, tipo, pdf(tipo.toLowerCase() + ".pdf", PDF), AGENTE);
        }
    }

    private DocumentoRequeridoResponse fila(String tipo) {
        return documentos.listar(DESPACHO).documentos().stream()
                .filter(d -> d.tipo().equals(tipo)).findFirst().orElseThrow();
    }

    private static TipoDocumento tipo(Long id, String codigo, String nombre, boolean obligatorio, int orden) {
        TipoDocumento t = new TipoDocumento(codigo, nombre, obligatorio, orden);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static MockMultipartFile pdf(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }
}
