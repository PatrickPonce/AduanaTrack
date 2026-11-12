package pe.edu.usmp.aduanatrack.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.reporte.controller.ReporteExportController;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;
import pe.edu.usmp.aduanatrack.reporte.repository.ReporteExportRepository;
import pe.edu.usmp.aduanatrack.reporte.service.DatosReporteService;
import pe.edu.usmp.aduanatrack.reporte.service.ReporteExcelService;
import pe.edu.usmp.aduanatrack.reporte.service.ReportePdfService;
import pe.edu.usmp.aduanatrack.seguridad.JwtService;
import pe.edu.usmp.aduanatrack.seguridad.SesionCookieService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;
import pe.edu.usmp.aduanatrack.support.SeguridadWebTestConfig;

/**
 * Pruebas de aceptación de HU-15 · T-15.5 (SCRUM-98): una por criterio, de la ruta HTTP al archivo descargado,
 * con los servicios reales del reporte (datos, Excel con POI y PDF con OpenPDF). Solo la base de datos se
 * reemplaza por un repositorio en memoria que aplica el mismo filtro que la consulta: apertura dentro del rango
 * y sin borradores.
 *
 * <p>El CA4 (envío programado por correo) depende de T-15.3 (SCRUM-96, V015 reporte_programado), que todavía no
 * está en develop; su prueba de pantalla ya está en {@code ReportesPage.test.tsx} y la de backend se agrega
 * cuando se publique ese servicio.</p>
 */
@WebMvcTest(ReporteExportController.class)
@Import({ SeguridadWebTestConfig.class, DatosReporteService.class, ReporteExcelService.class,
        ReportePdfService.class, Hu15CriteriosAceptacionTest.RelojDeLaPrueba.class })
class Hu15CriteriosAceptacionTest {

    private static final String RUTA = "/api/v1/reportes/exportar";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    /**
     * Despachos de ejemplo. Días de apertura a levante, contados a mano en el calendario:
     * <ul>
     * <li>0101: 03/09 → 15/09 = 12 días</li>
     * <li>0102: 20/09 → 02/10 = 12 días (10 de septiembre + 2 de octubre)</li>
     * <li>0103: 01/10 → 08/10 = 7 días</li>
     * <li>0104: 14/10 → 24/10 = 10 días</li>
     * <li>0105 y 0106: sin levante, no cuentan para el promedio</li>
     * </ul>
     * Del 01/09 al 31/10: (12 + 12 + 7 + 10) / 4 = 41 / 4 = 10,25 → 10,3 días.
     * Del 01/10 al 30/11: (7 + 10) / 2 = 8,5 días.
     */
    private static final List<FilaReporte> DESPACHOS = List.of(
            fila("DESP-2026-0101", "2026-09-03", "2026-09-15", "VERDE", EstadoDespacho.LEVANTE_OTORGADO),
            fila("DESP-2026-0102", "2026-09-20", "2026-10-02", "NARANJA", EstadoDespacho.RETIRO_PROGRAMADO),
            fila("DESP-2026-0103", "2026-10-01", "2026-10-08", "VERDE", EstadoDespacho.LEVANTE_OTORGADO),
            fila("DESP-2026-0104", "2026-10-14", "2026-10-24", "ROJO", EstadoDespacho.LEVANTE_OTORGADO),
            fila("DESP-2026-0105", "2026-10-28", null, "NARANJA", EstadoDespacho.INSPECCION_PROGRAMADA),
            fila("DESP-2026-0106", "2026-11-04", null, null, EstadoDespacho.TRIBUTOS_PAGADOS),
            // Un borrador nunca entra al reporte.
            fila(null, "2026-10-10", null, null, EstadoDespacho.BORRADOR));

    @TestConfiguration
    static class RelojDeLaPrueba {
        @Bean
        @Primary
        Clock relojDePrueba() {
            // Martes 17/11/2026, día de las pruebas de HU-15.
            return Clock.fixed(LocalDateTime.of(2026, 11, 17, 10, 0).atZone(LIMA).toInstant(), LIMA);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DatosReporteService datosService;

    @MockitoBean
    private ReporteExportRepository repositorio;

    @BeforeEach
    void preparar() {
        when(repositorio.despachosEnRango(any(), any(), any())).thenAnswer(i -> {
            LocalDateTime desde = i.getArgument(0);
            LocalDateTime hastaExclusivo = i.getArgument(1);
            EstadoDespacho borrador = i.getArgument(2);
            return DESPACHOS.stream()
                    .filter(f -> f.estado() != borrador)
                    .filter(f -> !f.creadoEn().isBefore(desde) && f.creadoEn().isBefore(hastaExclusivo))
                    .toList();
        });
    }

    @Test
    void criterio1_elTiempoPromedioCoincideConElCalculoAManoYEsElMismoEnLasDosExportaciones() throws Exception {
        double aMano = (12 + 12 + 7 + 10) / 4.0;
        assertThat(aMano).isEqualTo(10.25);
        assertThat(diasEntre("2026-09-20", "2026-10-02")).isEqualTo(12);

        DatosReporte datos = datosService.generar("2026-09-01", "2026-10-31");

        assertThat(datos.totalDespachos()).isEqualTo(5);
        assertThat(datos.conLevante()).isEqualTo(4);
        assertThat(datos.tiempoPromedioDias()).isEqualTo(10.3);

        try (Workbook libro = excel(exportar("xlsx", "2026-09-01", "2026-10-31"))) {
            Sheet indicadores = libro.getSheet("Indicadores");
            assertThat(valor(indicadores, "Tiempo promedio de despacho (días)").getNumericCellValue()).isEqualTo(10.3);
        }
        assertThat(textoPdf(exportar("pdf", "2026-09-01", "2026-10-31")))
                .contains("Tiempo promedio de despacho (días)").contains("10,3");
    }

    @Test
    void criterio1_sinDespachosConLevanteEnElRangoNoHayPromedio() {
        DatosReporte datos = datosService.generar("2026-11-01", "2026-11-30");

        assertThat(datos.totalDespachos()).isEqualTo(1);
        assertThat(datos.conLevante()).isZero();
        assertThat(datos.tiempoPromedioDias()).isNull();
    }

    @Test
    void criterio2_alCambiarElRangoLosIndicadoresYLosDespachosPorMesYPorCanalSeActualizan() throws Exception {
        DatosReporte septiembreOctubre = datosService.generar("2026-09-01", "2026-10-31");
        DatosReporte octubreNoviembre = datosService.generar("2026-10-01", "2026-11-30");

        assertThat(septiembreOctubre.despachosPorMes())
                .containsExactly(entry(YearMonth.of(2026, 9), 2L), entry(YearMonth.of(2026, 10), 3L));
        assertThat(septiembreOctubre.porCanal())
                .containsEntry("VERDE", 2L).containsEntry("NARANJA", 2L).containsEntry("ROJO", 1L);

        assertThat(octubreNoviembre.totalDespachos()).isEqualTo(4);
        assertThat(octubreNoviembre.conLevante()).isEqualTo(2);
        assertThat(octubreNoviembre.tiempoPromedioDias()).isEqualTo(8.5);
        assertThat(octubreNoviembre.despachosPorMes())
                .containsExactly(entry(YearMonth.of(2026, 10), 3L), entry(YearMonth.of(2026, 11), 1L));
        assertThat(octubreNoviembre.porCanal())
                .containsEntry("VERDE", 1L).containsEntry("NARANJA", 1L).containsEntry("ROJO", 1L);
        assertThat(octubreNoviembre.sinCanal()).isEqualTo(1);

        // El archivo exportado también sigue al rango.
        try (Workbook libro = excel(exportar("xlsx", "2026-10-01", "2026-11-30"))) {
            assertThat(valor(libro.getSheet("Indicadores"), "Tiempo promedio de despacho (días)")
                    .getNumericCellValue()).isEqualTo(8.5);
            Sheet despachos = libro.getSheet("Despachos");
            assertThat(despachos.getLastRowNum()).isEqualTo(4);
            assertThat(despachos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("DESP-2026-0103");
        }
    }

    @Test
    void criterio3_laExportacionAExcelDescargaUnXlsxQueSeAbreSinErrores() throws Exception {
        byte[] archivo = mvc.perform(get(RUTA).param("formato", "xlsx").param("desde", "2026-09-01")
                .param("hasta", "2026-11-30").cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-gestion_2026-11-17.xlsx\""))
                .andReturn().getResponse().getContentAsByteArray();

        // Un .xlsx es un ZIP: empieza con «PK».
        assertThat(new String(archivo, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
        try (Workbook libro = excel(archivo)) {
            assertThat(libro.getNumberOfSheets()).isEqualTo(2);
            assertThat(valor(libro.getSheet("Indicadores"), "Total de despachos").getNumericCellValue())
                    .isEqualTo(6);
            assertThat(libro.getSheet("Despachos").getLastRowNum()).isEqualTo(6);
        }
    }

    @Test
    void criterio3_laExportacionAPdfDescargaUnPdfQueSeAbreSinErrores() throws Exception {
        byte[] archivo = mvc.perform(get(RUTA).param("formato", "pdf").param("desde", "2026-09-01")
                .param("hasta", "2026-11-30").cookie(sesion("AGENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-gestion_2026-11-17.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(archivo, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = textoPdf(archivo);
        assertThat(texto).contains("Reporte de gestión aduanera").contains("Del 01/09/2026 al 30/11/2026");
        assertThat(texto).contains("DESP-2026-0101").contains("DESP-2026-0106");
    }

    private byte[] exportar(String formato, String desde, String hasta) throws Exception {
        return mvc.perform(get(RUTA).param("formato", formato).param("desde", desde).param("hasta", hasta)
                .cookie(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private Cookie sesion(String rol) {
        String token = jwtService.generar(new UsuarioAutenticado(1L, "usuario", "Usuario", rol), Duration.ofHours(2));
        return new Cookie(SesionCookieService.NOMBRE_COOKIE, token);
    }

    private static Workbook excel(byte[] archivo) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(archivo));
    }

    /** Celda de valor (columna B) de la fila cuya etiqueta (columna A) es {@code etiqueta}. */
    private static org.apache.poi.ss.usermodel.Cell valor(Sheet hoja, String etiqueta) {
        for (Row fila : hoja) {
            if (fila.getCell(0) != null && etiqueta.equals(fila.getCell(0).getStringCellValue())) {
                return fila.getCell(1);
            }
        }
        throw new AssertionError("No está la fila «" + etiqueta + "» en la hoja " + hoja.getSheetName());
    }

    private static String textoPdf(byte[] archivo) throws Exception {
        PdfReader lector = new PdfReader(archivo);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return texto.toString();
        } finally {
            lector.close();
        }
    }

    private static long diasEntre(String desde, String hasta) {
        return ChronoUnit.DAYS.between(LocalDate.parse(desde), LocalDate.parse(hasta));
    }

    private static FilaReporte fila(String numero, String apertura, String levante, String canal,
            EstadoDespacho estado) {
        return new FilaReporte(numero, "Comercial Lima SA", "Aduana Marítima del Callao", estado, canal,
                LocalDateTime.parse(apertura + "T09:00:00"), levante == null ? null : LocalDate.parse(levante));
    }
}
