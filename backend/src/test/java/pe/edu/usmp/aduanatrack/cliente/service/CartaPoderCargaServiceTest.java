package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.CartaPoderProperties;

class CartaPoderCargaServiceTest {

    private static final Long CLIENTE = 7L;
    private static final byte[] PDF = "%PDF-1.7\ncarta poder de prueba\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path directorio;

    private CartaPoderRepository repositorio;
    private ClienteRepository clienteRepository;
    private CartaPoderCargaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CartaPoderRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        CartaPoderEstadoService estadoService = mock(CartaPoderEstadoService.class);
        when(clienteRepository.existsById(CLIENTE)).thenReturn(true);
        when(repositorio.saveAndFlush(any(CartaPoder.class))).thenAnswer(i -> i.getArgument(0));
        when(estadoService.estado(CLIENTE)).thenReturn(EstadoCartaPoder.VIGENTE);
        servicio = new CartaPoderCargaService(repositorio, clienteRepository, estadoService,
                new CartaPoderProperties(directorio.toString()));
    }

    @Test
    void pdfValido_seGuardaConUuidYRegistraNombreTamanoYSha256() throws Exception {
        CartaPoderResponse respuesta = servicio.cargar(CLIENTE, pdf("Carta Poder 2026.pdf", PDF), "2026-01-15",
                "2027-01-15");

        CartaPoder carta = guardada();
        Path archivo = Path.of(carta.getRutaArchivo());
        assertThat(archivo.getParent()).isEqualTo(directorio);
        assertThat(archivo.getFileName().toString())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");
        assertThat(Files.readAllBytes(archivo)).isEqualTo(PDF);
        assertThat(carta.getClienteId()).isEqualTo(CLIENTE);
        assertThat(carta.getNombreArchivo()).isEqualTo("Carta Poder 2026.pdf");
        assertThat(carta.getTamanoBytes()).isEqualTo(PDF.length);
        assertThat(carta.getSha256())
                .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PDF)));
        assertThat(carta.getFechaEmision()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(carta.getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 1, 15));
        assertThat(respuesta.estadoCartaPoder()).isEqualTo(EstadoCartaPoder.VIGENTE);
        assertThat(respuesta.nombreArchivo()).isEqualTo("Carta Poder 2026.pdf");
    }

    @Test
    void archivoQueNoEmpiezaConPdf_responde415YNoGuardaNada() throws IOException {
        MultipartFile falso = pdf("carta.pdf", "<html>no soy un pdf</html>".getBytes(StandardCharsets.US_ASCII));

        ApiException error = catchThrowableOfType(ApiException.class,
                () -> servicio.cargar(CLIENTE, falso, "2026-01-15", "2027-01-15"));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FORMATO_NO_PERMITIDO);
        verify(repositorio, never()).saveAndFlush(any());
        assertThat(archivosGuardados()).isEmpty();
    }

    @Test
    void archivoDeMasDe10Mb_responde413YNoGuardaNada() throws IOException {
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(CartaPoderCargaService.TAMANO_MAXIMO_BYTES + 1);

        ApiException error = catchThrowableOfType(ApiException.class,
                () -> servicio.cargar(CLIENTE, grande, "2026-01-15", "2027-01-15"));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(error.getCodigo()).isEqualTo(CodigoError.ARCHIVO_MUY_GRANDE);
        verify(repositorio, never()).saveAndFlush(any());
        assertThat(archivosGuardados()).isEmpty();
    }

    @Test
    void archivoDeExactamente10Mb_seAcepta() {
        byte[] contenido = new byte[(int) CartaPoderCargaService.TAMANO_MAXIMO_BYTES];
        System.arraycopy(PDF, 0, contenido, 0, PDF.length);

        servicio.cargar(CLIENTE, pdf("limite.pdf", contenido), "2026-01-15", "2027-01-15");

        assertThat(guardada().getTamanoBytes()).isEqualTo(CartaPoderCargaService.TAMANO_MAXIMO_BYTES);
    }

    @Test
    void sinArchivoNiFechas_responde400ConLosTresCampos() {
        ApiException error = catchThrowableOfType(ApiException.class,
                () -> servicio.cargar(CLIENTE, null, null, " "));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getCodigo()).isEqualTo(CodigoError.VALIDACION);
        assertThat(error.getCampos()).containsOnlyKeys("archivo", "fechaEmision", "fechaVencimiento");
    }

    @Test
    void vencimientoNoPosteriorALaEmision_oFechaMalEscrita_responde400EnElCampo() {
        ApiException mismaFecha = catchThrowableOfType(ApiException.class,
                () -> servicio.cargar(CLIENTE, pdf("carta.pdf", PDF), "2026-01-15", "2026-01-15"));
        ApiException malEscrita = catchThrowableOfType(ApiException.class,
                () -> servicio.cargar(CLIENTE, pdf("carta.pdf", PDF), "15/01/2026", "2027-01-15"));

        assertThat(mismaFecha.getCampos())
                .containsEntry("fechaVencimiento", "Debe ser posterior a la fecha de emisión");
        assertThat(malEscrita.getCampos()).containsOnlyKeys("fechaEmision");
    }

    @Test
    void clienteInexistente_responde404() {
        assertThatThrownBy(() -> servicio.cargar(99L, pdf("carta.pdf", PDF), "2026-01-15", "2027-01-15"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    /** El nombre que envía el navegador nunca decide dónde se guarda el archivo. */
    @Test
    void nombreConRuta_seGuardaSoloElNombreYElArchivoQuedaEnLaCarpeta() {
        servicio.cargar(CLIENTE, pdf("..\\..\\etc/../secreto.pdf", PDF), "2026-01-15", "2027-01-15");

        CartaPoder carta = guardada();
        assertThat(carta.getNombreArchivo()).isEqualTo("secreto.pdf");
        assertThat(Path.of(carta.getRutaArchivo()).getParent()).isEqualTo(directorio);
    }

    @Test
    void siFallaElGuardadoEnLaBase_seEliminaElArchivo() throws IOException {
        when(repositorio.saveAndFlush(any(CartaPoder.class))).thenThrow(new DataIntegrityViolationException("x"));

        assertThatThrownBy(() -> servicio.cargar(CLIENTE, pdf("carta.pdf", PDF), "2026-01-15", "2027-01-15"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(archivosGuardados()).isEmpty();
    }

    private static MultipartFile pdf(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }

    private CartaPoder guardada() {
        ArgumentCaptor<CartaPoder> captor = ArgumentCaptor.forClass(CartaPoder.class);
        verify(repositorio).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private List<Path> archivosGuardados() throws IOException {
        try (Stream<Path> archivos = Files.list(directorio)) {
            return archivos.toList();
        }
    }
}
