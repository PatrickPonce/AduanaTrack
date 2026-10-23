package pe.edu.usmp.aduanatrack.documento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoCargadoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentoRequeridoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.DocumentosDespachoResponse;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumentacion;
import pe.edu.usmp.aduanatrack.documento.dto.EstadoDocumento;
import pe.edu.usmp.aduanatrack.documento.dto.VersionDocumentoResponse;
import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;
import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;
import pe.edu.usmp.aduanatrack.documento.repository.DocumentoDespachoRepository;
import pe.edu.usmp.aduanatrack.documento.repository.TipoDocumentoRepository;

class DocumentoDespachoServiceTest {

    private static final Long DESPACHO = 5L;
    private static final Long AGENTE = 9L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 22, 10, 15);
    private static final byte[] PDF = "%PDF-1.7\nfactura comercial de prueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    /** Un .docx es un ZIP: empieza con «PK». */
    private static final byte[] DOCX = { 'P', 'K', 3, 4, 20, 0, 6, 0 };

    @TempDir
    Path directorio;

    private DocumentoDespachoRepository documentoRepository;
    private TipoDocumentoRepository tipoRepository;
    private DespachoRepository despachoRepository;
    private Despacho despacho;
    private Usuario agente;
    private TipoDocumento factura;
    private TipoDocumento poliza;
    private TipoDocumento certificado;
    private DocumentoDespachoService servicio;

    @BeforeEach
    void preparar() {
        documentoRepository = mock(DocumentoDespachoRepository.class);
        tipoRepository = mock(TipoDocumentoRepository.class);
        despachoRepository = mock(DespachoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        factura = tipo(1L, "FACTURA_COMERCIAL", "Factura comercial", true, 1);
        poliza = tipo(4L, "POLIZA_SEGURO", "Póliza de seguro", true, 4);
        certificado = tipo(5L, "CERTIFICADO_ORIGEN", "Certificado de origen", false, 5);

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(usuarioRepository.getReferenceById(AGENTE)).thenReturn(agente);
        when(tipoRepository.findByActivoTrueOrderByOrden()).thenReturn(List.of(factura, poliza, certificado));
        Stream.of(factura, poliza, certificado).forEach(
                t -> when(tipoRepository.findByCodigoAndActivoTrue(t.getCodigo())).thenReturn(Optional.of(t)));
        when(documentoRepository.findByDespachoIdAndTipoIdAndVigenteTrue(any(), any())).thenReturn(Optional.empty());
        when(documentoRepository.saveAndFlush(any(DocumentoDespacho.class))).thenAnswer(i -> {
            DocumentoDespacho d = i.getArgument(0);
            if (d.getId() == null) {
                ReflectionTestUtils.setField(d, "id", 40L + d.getVersion());
            }
            return d;
        });

        Clock reloj = Clock.fixed(Instant.parse("2026-10-22T15:15:00Z"), ZoneId.of("America/Lima"));
        servicio = new DocumentoDespachoService(documentoRepository, tipoRepository, despachoRepository,
                usuarioRepository, reloj, new DocumentoProperties(directorio.toString()));
    }

    // CA1: carga de PDF de hasta 10 MB

    @Test
    void pdfValido_quedaCargadoComoVersion1EnLaCarpetaDelDespacho() throws Exception {
        DocumentoCargadoResponse respuesta = servicio.cargar(DESPACHO, "factura_comercial",
                pdf("Factura 001-2026.pdf", PDF), AGENTE);

        assertThat(respuesta.tipo()).isEqualTo("FACTURA_COMERCIAL");
        assertThat(respuesta.version()).isEqualTo(1);
        assertThat(respuesta.estado()).isEqualTo(EstadoDocumento.CARGADO);

        DocumentoDespacho guardado = guardados().get(0);
        assertThat(guardado.isVigente()).isTrue();
        assertThat(guardado.getNombreArchivo()).isEqualTo("Factura 001-2026.pdf");
        assertThat(guardado.getRutaArchivo()).matches("5/[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(directorio.resolve(guardado.getRutaArchivo()))).isEqualTo(PDF);
        assertThat(guardado.getTamanoBytes()).isEqualTo(PDF.length);
        assertThat(guardado.getSha256()).hasSize(64);
        assertThat(guardado.getUsuario()).isSameAs(agente);
        assertThat(guardado.getCargadoEn()).isEqualTo(AHORA);
    }

    @Test
    void archivoDeMasDe10Mb_responde413SinGuardarNada() {
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(AlmacenPdf.TAMANO_MAXIMO_BYTES + 1);

        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "FACTURA_COMERCIAL", grande, AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.ARCHIVO_MUY_GRANDE);
                });
        verify(documentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void docxRenombradoComoPdf_responde415SinDejarArchivo() throws Exception {
        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("factura.pdf", DOCX), AGENTE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
                });
        verify(documentoRepository, never()).saveAndFlush(any());
        assertThat(archivosEnDisco()).isEmpty();
    }

    @Test
    void sinArchivo_responde400EnElCampoArchivo() {
        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "FACTURA_COMERCIAL", null, AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCampos()).containsOnlyKeys("archivo"));
    }

    @Test
    void despachoEnBorrador_responde409() {
        Despacho borrador = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        when(despachoRepository.findById(6L)).thenReturn(Optional.of(borrador));

        assertThatThrownBy(() -> servicio.cargar(6L, "FACTURA_COMERCIAL", pdf("f.pdf", PDF), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.DESPACHO_EN_BORRADOR));
    }

    @Test
    void tipoDeDocumentoInexistente_responde404() {
        when(tipoRepository.findByCodigoAndActivoTrue("CONTRATO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "contrato", pdf("c.pdf", PDF), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void siFallaLaBaseDeDatos_noQuedaElPdfHuerfano() throws Exception {
        when(documentoRepository.saveAndFlush(any(DocumentoDespacho.class)))
                .thenThrow(new IllegalStateException("caída de la base de datos"));

        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("f.pdf", PDF), AGENTE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(archivosEnDisco()).isEmpty();
    }

    @Test
    void cargaSimultaneaDelMismoDocumento_responde409YBorraElPdf() throws Exception {
        when(documentoRepository.saveAndFlush(any(DocumentoDespacho.class)))
                .thenThrow(new DataIntegrityViolationException("uq_documento_despacho_version"));

        assertThatThrownBy(() -> servicio.cargar(DESPACHO, "FACTURA_COMERCIAL", pdf("f.pdf", PDF), AGENTE))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.CARGA_SIMULTANEA));
        assertThat(archivosEnDisco()).isEmpty();
    }

    // CA3: al reemplazar se conserva la versión anterior

    @Test
    void reemplazar_creaLaVersion2VigenteYConservaLaAnterior() throws Exception {
        DocumentoDespacho anterior = documento(41L, factura, 1, "factura.pdf");
        Path archivoAnterior = directorio.resolve(anterior.getRutaArchivo());
        Files.createDirectories(archivoAnterior.getParent());
        Files.write(archivoAnterior, PDF);
        when(documentoRepository.findByDespachoIdAndTipoIdAndVigenteTrue(DESPACHO, 1L)).thenReturn(Optional.of(anterior));
        when(documentoRepository.ultimaVersion(DESPACHO, 1L)).thenReturn(1);

        DocumentoCargadoResponse respuesta = servicio.cargar(DESPACHO, "FACTURA_COMERCIAL",
                pdf("factura-corregida.pdf", PDF), AGENTE);

        assertThat(respuesta.version()).isEqualTo(2);
        List<DocumentoDespacho> guardados = guardados();
        assertThat(guardados).hasSize(2);
        // Primero se retira la vigente y después se inserta la nueva (índice único de vigentes).
        assertThat(guardados.get(0)).isSameAs(anterior);
        assertThat(anterior.isVigente()).isFalse();
        assertThat(guardados.get(1).isVigente()).isTrue();
        assertThat(guardados.get(1).getVersion()).isEqualTo(2);
        assertThat(archivoAnterior).exists();
    }

    @Test
    void versiones_listaTodasConSoloLaUltimaVigente() {
        DocumentoDespacho v1 = documento(41L, factura, 1, "factura.pdf");
        v1.reemplazar();
        DocumentoDespacho v2 = documento(42L, factura, 2, "factura-corregida.pdf");
        when(documentoRepository.versiones(DESPACHO, 1L)).thenReturn(List.of(v2, v1));

        List<VersionDocumentoResponse> versiones = servicio.versiones(DESPACHO, "FACTURA_COMERCIAL");

        assertThat(versiones).extracting(VersionDocumentoResponse::version).containsExactly(2, 1);
        assertThat(versiones).extracting(VersionDocumentoResponse::vigente).containsExactly(true, false);
        assertThat(versiones.get(0).cargadoPor()).isEqualTo("Agente Demo");
    }

    // CA4: estado general de la documentación

    @Test
    void conUnObligatorioPendiente_elEstadoGeneralEsPendiente() {
        when(documentoRepository.vigentes(DESPACHO)).thenReturn(List.of(documento(41L, factura, 2, "factura.pdf")));

        DocumentosDespachoResponse respuesta = servicio.listar(DESPACHO);

        assertThat(respuesta.estadoGeneral()).isEqualTo(EstadoDocumentacion.PENDIENTE);
        assertThat(respuesta.documentos()).extracting(DocumentoRequeridoResponse::tipo)
                .containsExactly("FACTURA_COMERCIAL", "POLIZA_SEGURO", "CERTIFICADO_ORIGEN");
        DocumentoRequeridoResponse cargada = respuesta.documentos().get(0);
        assertThat(cargada.estado()).isEqualTo(EstadoDocumento.CARGADO);
        assertThat(cargada.version()).isEqualTo(2);
        assertThat(cargada.documentoId()).isEqualTo(41L);
        assertThat(cargada.archivo()).isEqualTo("factura.pdf");
        assertThat(cargada.cargadoPor()).isEqualTo("Agente Demo");
        DocumentoRequeridoResponse pendiente = respuesta.documentos().get(1);
        assertThat(pendiente.estado()).isEqualTo(EstadoDocumento.PENDIENTE);
        assertThat(pendiente.obligatorio()).isTrue();
        assertThat(pendiente.version()).isNull();
    }

    @Test
    void conTodosLosObligatorios_elEstadoGeneralEsCompletoAunqueFalteUnOpcional() {
        when(documentoRepository.vigentes(DESPACHO)).thenReturn(List.of(
                documento(41L, factura, 1, "factura.pdf"), documento(43L, poliza, 1, "poliza.pdf")));

        DocumentosDespachoResponse respuesta = servicio.listar(DESPACHO);

        assertThat(respuesta.estadoGeneral()).isEqualTo(EstadoDocumentacion.COMPLETO);
        assertThat(respuesta.documentos().get(2).estado()).isEqualTo(EstadoDocumento.PENDIENTE);
    }

    @Test
    void listarDespachoInexistente_responde404() {
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listar(99L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // Descarga

    @Test
    void archivo_devuelveLaRutaDentroDelDirectorioYElNombreOriginal() throws Exception {
        DocumentoDespacho doc = documento(41L, factura, 1, "factura.pdf");
        Path ruta = directorio.resolve(doc.getRutaArchivo());
        Files.createDirectories(ruta.getParent());
        Files.write(ruta, PDF);
        when(documentoRepository.findByIdAndDespachoId(41L, DESPACHO)).thenReturn(Optional.of(doc));

        DocumentoDespachoService.ArchivoDocumento archivo = servicio.archivo(DESPACHO, 41L);

        assertThat(archivo.ruta()).isEqualTo(ruta.toAbsolutePath().normalize());
        assertThat(archivo.nombre()).isEqualTo("factura.pdf");
    }

    @Test
    void archivoDeOtroDespacho_responde404() {
        when(documentoRepository.findByIdAndDespachoId(41L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.archivo(7L, 41L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rutaQueSaleDelDirectorio_responde404() throws Exception {
        Files.write(directorio.getParent().resolve("fuera.pdf"), PDF);
        DocumentoDespacho doc = new DocumentoDespacho(DESPACHO, factura, 1, "fuera.pdf", "../fuera.pdf", 10, "x",
                agente, AHORA);
        when(documentoRepository.findByIdAndDespachoId(41L, DESPACHO)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> servicio.archivo(DESPACHO, 41L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private List<DocumentoDespacho> guardados() {
        ArgumentCaptor<DocumentoDespacho> captor = ArgumentCaptor.forClass(DocumentoDespacho.class);
        verify(documentoRepository, atLeastOnce()).saveAndFlush(captor.capture());
        return new ArrayList<>(captor.getAllValues());
    }

    private List<Path> archivosEnDisco() throws Exception {
        try (Stream<Path> archivos = Files.walk(directorio)) {
            return archivos.filter(Files::isRegularFile).toList();
        }
    }

    private DocumentoDespacho documento(Long id, TipoDocumento tipo, int version, String nombre) {
        DocumentoDespacho d = new DocumentoDespacho(DESPACHO, tipo, version, nombre,
                DESPACHO + "/" + tipo.getCodigo().toLowerCase() + "-v" + version + ".pdf", PDF.length, "a".repeat(64),
                agente, AHORA);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
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
