package pe.edu.usmp.aduanatrack.cliente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResumen;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.config.CartaPoderProperties;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;

class CartaPoderConsultaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 8);

    @TempDir
    Path directorio;

    private CartaPoderRepository repositorio;
    private ClienteRepository clienteRepository;
    private CartaPoderConsultaService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CartaPoderRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        ParametroService parametros = mock(ParametroService.class);
        when(parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER)).thenReturn(15);
        when(clienteRepository.existsById(7L)).thenReturn(true);
        Clock reloj = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CartaPoderConsultaService(repositorio, clienteRepository, parametros, reloj,
                new CartaPoderProperties(directorio.toString()));
    }

    @Test
    void listar_calculaElEstadoDeCadaCarta() {
        when(repositorio.findByClienteIdOrderByFechaVencimientoDescIdDesc(7L)).thenReturn(List.of(
                carta(2L, HOY.plusDays(10), "nueva.pdf"),
                carta(1L, HOY.minusDays(1), "anterior.pdf")));

        assertThat(servicio.listar(7L)).extracting(CartaPoderResumen::estado)
                .containsExactly(EstadoCartaPoder.POR_VENCER, EstadoCartaPoder.VENCIDA);
    }

    @Test
    void listar_clienteInexistente_responde404() {
        assertThatThrownBy(() -> servicio.listar(99L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void archivo_leeElPdfDelDirectorioConfigurado() throws Exception {
        Path pdf = Files.writeString(directorio.resolve("abc.pdf"), "%PDF-1.7", StandardCharsets.US_ASCII);
        CartaPoder c = carta(3L, HOY.plusYears(1), "carta original.pdf");
        ReflectionTestUtils.setField(c, "rutaArchivo", "/otra/carpeta/abc.pdf");
        when(repositorio.findByIdAndClienteId(3L, 7L)).thenReturn(Optional.of(c));

        CartaPoderConsultaService.ArchivoCartaPoder archivo = servicio.archivo(7L, 3L);

        assertThat(archivo.ruta()).isEqualTo(pdf.toAbsolutePath().normalize());
        assertThat(archivo.nombre()).isEqualTo("carta original.pdf");
    }

    @Test
    void archivo_deOtroCliente_responde404() {
        when(repositorio.findByIdAndClienteId(3L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.archivo(8L, 3L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private CartaPoder carta(Long id, LocalDate vencimiento, String nombre) {
        CartaPoder c = new CartaPoder(7L, vencimiento.minusYears(1), vencimiento,
                directorio.resolve(id + ".pdf").toString(), nombre, 8, "a".repeat(64));
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
