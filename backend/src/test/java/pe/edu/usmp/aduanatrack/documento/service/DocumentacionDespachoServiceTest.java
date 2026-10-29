package pe.edu.usmp.aduanatrack.documento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.auth.entity.Rol;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.documento.entity.DocumentoDespacho;
import pe.edu.usmp.aduanatrack.documento.entity.TipoDocumento;
import pe.edu.usmp.aduanatrack.documento.repository.DocumentoDespachoRepository;
import pe.edu.usmp.aduanatrack.documento.repository.TipoDocumentoRepository;

/** Estado general y bloqueo de HU-06 · T-06.3 (SCRUM-55). */
class DocumentacionDespachoServiceTest {

    private static final Long DESPACHO = 5L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 24, 9, 0);

    private DocumentoDespachoRepository documentoRepository;
    private DespachoRepository despachoRepository;
    private HitoDespachoService hitos;
    private Despacho despacho;
    private Usuario agente;
    private TipoDocumento factura;
    private TipoDocumento bl;
    private TipoDocumento poliza;
    private TipoDocumento certificado;
    private DocumentacionDespachoService servicio;

    @BeforeEach
    void preparar() {
        documentoRepository = mock(DocumentoDespachoRepository.class);
        TipoDocumentoRepository tipoRepository = mock(TipoDocumentoRepository.class);
        despachoRepository = mock(DespachoRepository.class);
        hitos = mock(HitoDespachoService.class);

        despacho = new Despacho(new Cliente("20787789324", AHORA), null, AHORA);
        despacho.abrir("DESP-2026-0001");
        agente = new Usuario("agente", "agente@aduanatrack.pe", "Agente Demo", "hash", new Rol("AGENTE", "Agente"));
        factura = tipo(1L, "FACTURA_COMERCIAL", "Factura comercial", true, 1);
        bl = tipo(2L, "CONOCIMIENTO_EMBARQUE", "Conocimiento de embarque (BL)", true, 2);
        poliza = tipo(4L, "POLIZA_SEGURO", "Póliza de seguro", true, 4);
        certificado = tipo(5L, "CERTIFICADO_ORIGEN", "Certificado de origen", false, 5);

        when(despachoRepository.findById(DESPACHO)).thenReturn(Optional.of(despacho));
        when(tipoRepository.findByActivoTrueOrderByOrden()).thenReturn(List.of(factura, bl, poliza, certificado));
        cargados();

        servicio = new DocumentacionDespachoService(documentoRepository, tipoRepository, despachoRepository, hitos);
    }

    // CA4: con todos los obligatorios el despacho pasa a DOCUMENTACION_COMPLETA

    @Test
    void conTodosLosObligatorios_pasaADocumentacionCompletaYRegistraElHito() {
        cargados(factura, bl, poliza);

        servicio.actualizarEstado(DESPACHO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.DOCUMENTACION_COMPLETA);
        verify(hitos).registrar(any(), any(), anyString());
        verify(hitos).registrar(DESPACHO, TipoHito.DOCUMENTACION_COMPLETA,
                "Documentación completa: se cargaron todos los documentos obligatorios");
    }

    @Test
    void conUnObligatorioPendiente_siguiendoEnAperturaYSinHito() {
        cargados(factura, bl);

        servicio.actualizarEstado(DESPACHO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.APERTURA);
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void unDocumentoOpcionalNoCuentaParaCompletar() {
        cargados(factura, bl, certificado);

        servicio.actualizarEstado(DESPACHO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.APERTURA);
    }

    @Test
    void soloAvanzaDesdeApertura_noRetrocedeNiRepiteElHito() {
        cargados(factura, bl, poliza);
        despacho.cambiarEstado(EstadoDespacho.CLASIFICADO);

        servicio.actualizarEstado(DESPACHO);

        assertThat(despacho.getEstado()).isEqualTo(EstadoDespacho.CLASIFICADO);
        verify(hitos, never()).registrar(any(), any(), anyString());
    }

    @Test
    void siYaEstabaCompleta_unReemplazoNoRepiteElHito() {
        cargados(factura, bl, poliza);
        servicio.actualizarEstado(DESPACHO);

        servicio.actualizarEstado(DESPACHO);

        verify(hitos, org.mockito.Mockito.times(1)).registrar(any(), any(), anyString());
    }

    @Test
    void actualizarEstadoDeUnDespachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.actualizarEstado(99L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // CA2: sin los obligatorios no se puede clasificar

    @Test
    void exigir_conUnObligatorioPendiente_responde409ConLaListaDePendientes() {
        cargados(factura, bl);

        assertThatThrownBy(() -> servicio.exigirDocumentacionCompleta(DESPACHO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo(CodigoError.DOCUMENTACION_INCOMPLETA);
                    assertThat(e.getMessage()).contains("Póliza de seguro");
                    assertThat(e.getDatos()).containsEntry("pendientes", List.of("POLIZA_SEGURO"));
                });
    }

    @Test
    void exigir_conVariosPendientes_losListaEnElOrdenDelCatalogo() {
        cargados(bl);

        assertThatThrownBy(() -> servicio.exigirDocumentacionCompleta(DESPACHO))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getDatos())
                        .containsEntry("pendientes", List.of("FACTURA_COMERCIAL", "POLIZA_SEGURO")));
    }

    @Test
    void exigir_sinNingunDocumento_nombraTodosLosObligatorios() {
        assertThatThrownBy(() -> servicio.exigirDocumentacionCompleta(DESPACHO))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getDatos())
                        .containsEntry("pendientes", List.of("FACTURA_COMERCIAL", "CONOCIMIENTO_EMBARQUE",
                                "POLIZA_SEGURO")));
    }

    @Test
    void exigir_conTodosLosObligatorios_noBloquea() {
        cargados(factura, bl, poliza);

        assertThatCode(() -> servicio.exigirDocumentacionCompleta(DESPACHO)).doesNotThrowAnyException();
    }

    @Test
    void exigir_deUnDespachoInexistente_responde404() {
        assertThatThrownBy(() -> servicio.exigirDocumentacionCompleta(99L)).isInstanceOfSatisfying(
                ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    /** Documentos con versión vigente en el despacho. */
    private void cargados(TipoDocumento... tipos) {
        List<DocumentoDespacho> vigentes = java.util.Arrays.stream(tipos).map(this::documento).toList();
        when(documentoRepository.vigentes(DESPACHO)).thenReturn(vigentes);
    }

    private DocumentoDespacho documento(TipoDocumento tipo) {
        return new DocumentoDespacho(DESPACHO, tipo, 1, tipo.getCodigo().toLowerCase() + ".pdf",
                DESPACHO + "/" + tipo.getCodigo().toLowerCase() + ".pdf", 1024, "a".repeat(64), agente, AHORA);
    }

    private static TipoDocumento tipo(Long id, String codigo, String nombre, boolean obligatorio, int orden) {
        TipoDocumento t = new TipoDocumento(codigo, nombre, obligatorio, orden);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }
}
