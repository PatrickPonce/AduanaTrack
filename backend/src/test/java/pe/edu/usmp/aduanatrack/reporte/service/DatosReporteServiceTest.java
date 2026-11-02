package pe.edu.usmp.aduanatrack.reporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;
import pe.edu.usmp.aduanatrack.reporte.repository.ReporteExportRepository;

/** Datos del reporte que se exporta: HU-15 · T-15.2 (SCRUM-95); los comentarios CAn son los criterios de HU-15. */
class DatosReporteServiceTest {

    private ReporteExportRepository repositorio;
    private DatosReporteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ReporteExportRepository.class);
        servicio = new DatosReporteService(repositorio);
    }

    static FilaReporte fila(String numero, String apertura, String levante, String canal) {
        return new FilaReporte(numero, "Comercial Lima SA", "Aduana Marítima del Callao",
                levante == null ? EstadoDespacho.CANAL_ASIGNADO : EstadoDespacho.LEVANTE_OTORGADO, canal,
                LocalDateTime.parse(apertura + "T09:00:00"), levante == null ? null : LocalDate.parse(levante));
    }

    // CA1: el tiempo promedio coincide con un cálculo a mano (solo despachos con levante)

    @Test
    void tiempoPromedio_coincideConElCalculoAManoYSoloContarLosDespachosConLevante() {
        // 10 días, 8 días y 9 días (promedio 9,0); el último no tiene levante y no cuenta.
        List<FilaReporte> filas = List.of(
                fila("DESP-1", "2026-10-01", "2026-10-11", "VERDE"),
                fila("DESP-2", "2026-10-05", "2026-10-13", "NARANJA"),
                fila("DESP-3", "2026-10-10", "2026-10-19", "ROJO"),
                fila("DESP-4", "2026-10-20", null, null));
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(filas);

        DatosReporte datos = servicio.generar("2026-10-01", "2026-10-31");

        assertThat(datos.totalDespachos()).isEqualTo(4);
        assertThat(datos.conLevante()).isEqualTo(3);
        assertThat(datos.tiempoPromedioDias()).isEqualTo(9.0);
    }

    @Test
    void tiempoPromedio_seRedondeaAUnDecimal() {
        List<FilaReporte> filas = List.of(
                fila("DESP-1", "2026-10-01", "2026-10-10", "VERDE"),
                fila("DESP-2", "2026-10-01", "2026-10-11", "VERDE"),
                fila("DESP-3", "2026-10-01", "2026-10-11", "VERDE"));
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(filas);

        assertThat(servicio.generar("2026-10-01", "2026-10-31").tiempoPromedioDias()).isEqualTo(9.7);
    }

    @Test
    void sinDespachosConLevante_elTiempoPromedioEsNulo() {
        when(repositorio.despachosEnRango(any(), any(), any()))
                .thenReturn(List.of(fila("DESP-1", "2026-10-01", null, null)));

        DatosReporte datos = servicio.generar("2026-10-01", "2026-10-31");

        assertThat(datos.conLevante()).isZero();
        assertThat(datos.tiempoPromedioDias()).isNull();
    }

    @Test
    void sinDespachosEnElRango_todoQuedaEnCero() {
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(List.of());

        DatosReporte datos = servicio.generar("2026-10-01", "2026-10-31");

        assertThat(datos.totalDespachos()).isZero();
        assertThat(datos.despachosPorMes()).isEmpty();
        assertThat(datos.porCanal()).containsEntry("VERDE", 0L).containsEntry("NARANJA", 0L).containsEntry("ROJO", 0L);
        assertThat(datos.despachos()).isEmpty();
    }

    // CA2: despachos por mes y por canal

    @Test
    void despachosPorMesYPorCanal_seCuentanPorLaFechaDeApertura() {
        List<FilaReporte> filas = List.of(
                fila("DESP-1", "2026-09-15", "2026-09-25", "VERDE"),
                fila("DESP-2", "2026-10-01", null, "VERDE"),
                fila("DESP-3", "2026-10-31", null, "rojo"),
                fila("DESP-4", "2026-10-31", null, null));
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(filas);

        DatosReporte datos = servicio.generar("2026-09-01", "2026-10-31");

        assertThat(datos.despachosPorMes()).containsExactly(
                org.assertj.core.api.Assertions.entry(YearMonth.of(2026, 9), 1L),
                org.assertj.core.api.Assertions.entry(YearMonth.of(2026, 10), 3L));
        assertThat(datos.porCanal()).containsEntry("VERDE", 2L).containsEntry("NARANJA", 0L).containsEntry("ROJO", 1L);
        assertThat(datos.sinCanal()).isEqualTo(1);
    }

    @Test
    void laConsultaIncluyeElDiaFinalCompleto() {
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(List.of());

        servicio.generar("2026-10-01", "2026-10-31");

        verify(repositorio).despachosEnRango(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0),
                EstadoDespacho.BORRADOR);
    }

    // Reglas del rango

    @Test
    void desdeMayorQueHasta_responde400() {
        assertThatThrownBy(() -> servicio.generar("2026-11-30", "2026-11-01"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCampos()).containsKey("desde");
                });
        verify(repositorio, never()).despachosEnRango(any(), any(), any());
    }

    @Test
    void rangoDeMasDeDoceMeses_responde400() {
        assertThatThrownBy(() -> servicio.generar("2025-10-31", "2026-11-01"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("hasta"));
    }

    @Test
    void rangoDeExactamenteDoceMeses_seAcepta() {
        when(repositorio.despachosEnRango(any(), any(), any())).thenReturn(List.of());

        assertThat(servicio.generar("2025-11-01", "2026-11-01").totalDespachos()).isZero();
    }

    @Test
    void fechasFaltantesOMalEscritas_responde400ConElCampo() {
        assertThatThrownBy(() -> servicio.generar(null, "2026-11-01"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("desde"));
        assertThatThrownBy(() -> servicio.generar("2026-10-01", " "))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("hasta"));
        assertThatThrownBy(() -> servicio.generar("01/10/2026", "2026-11-01"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCampos()).containsKey("desde"));
    }
}
