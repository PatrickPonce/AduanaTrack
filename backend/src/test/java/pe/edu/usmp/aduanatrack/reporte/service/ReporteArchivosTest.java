package pe.edu.usmp.aduanatrack.reporte.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;

/** Archivos del reporte de gestión (HU-15 · T-15.2, CA3): las dos exportaciones se abren sin errores. */
class ReporteArchivosTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    private static DatosReporte datos(List<FilaReporte> filas) {
        return DatosReporteService.calcular(DESDE, HASTA, filas);
    }

    private static final List<FilaReporte> CON_DESPACHOS = List.of(
            DatosReporteServiceTest.fila("DESP-2026-0001", "2026-10-01", "2026-10-11", "VERDE"),
            DatosReporteServiceTest.fila("DESP-2026-0002", "2026-10-05", "2026-10-13", "ROJO"),
            DatosReporteServiceTest.fila("DESP-2026-0003", "2026-10-20", null, null));

    private static Workbook abrirExcel(byte[] archivo) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(archivo));
    }

    private static String textoPdf(byte[] archivo) throws IOException {
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

    @Test
    void excel_traeLosIndicadoresYLaTablaDeDespachos() throws Exception {
        byte[] archivo = new ReporteExcelService().generar(datos(CON_DESPACHOS));

        try (Workbook libro = abrirExcel(archivo)) {
            assertThat(libro.getSheetName(0)).isEqualTo("Indicadores");
            assertThat(libro.getSheetName(1)).isEqualTo("Despachos");

            Sheet indicadores = libro.getSheet("Indicadores");
            assertThat(indicadores.getRow(1).getCell(1).getStringCellValue()).isEqualTo("01/10/2026");
            assertThat(indicadores.getRow(2).getCell(1).getStringCellValue()).isEqualTo("31/10/2026");
            assertThat(indicadores.getRow(5).getCell(1).getNumericCellValue()).isEqualTo(3);
            assertThat(indicadores.getRow(6).getCell(1).getNumericCellValue()).isEqualTo(2);
            // 10 y 8 días: promedio 9,0.
            assertThat(indicadores.getRow(7).getCell(1).getNumericCellValue()).isEqualTo(9.0);

            Sheet despachos = libro.getSheet("Despachos");
            assertThat(despachos.getLastRowNum()).isEqualTo(3);
            assertThat(despachos.getRow(0).getCell(0).getStringCellValue()).isEqualTo("N° despacho");
            assertThat(despachos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("DESP-2026-0001");
            assertThat(despachos.getRow(1).getCell(4).getStringCellValue()).isEqualTo("VERDE");
            assertThat(despachos.getRow(1).getCell(6).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 10, 11));
            assertThat(despachos.getRow(3).getCell(6)).isNull();
        }
    }

    @Test
    void excel_sinDespachosSeAbreConLasHojasYSinPromedio() throws Exception {
        byte[] archivo = new ReporteExcelService().generar(datos(List.of()));

        try (Workbook libro = abrirExcel(archivo)) {
            assertThat(libro.getSheet("Despachos").getLastRowNum()).isZero();
            assertThat(libro.getSheet("Indicadores").getRow(7).getCell(1).getStringCellValue())
                    .isEqualTo("Sin despachos con levante");
        }
    }

    @Test
    void pdf_esUnPdfValidoConLosIndicadoresYLosDespachos() throws Exception {
        byte[] archivo = new ReportePdfService().generar(datos(CON_DESPACHOS));

        assertThat(new String(archivo, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = textoPdf(archivo);
        assertThat(texto).contains("Reporte de gestión aduanera");
        assertThat(texto).contains("Del 01/10/2026 al 31/10/2026");
        assertThat(texto).contains("Tiempo promedio de despacho (días)");
        assertThat(texto).contains("octubre 2026");
        assertThat(texto).contains("DESP-2026-0001").contains("DESP-2026-0003");
    }

    @Test
    void pdf_sinDespachosTambienSeAbre() throws Exception {
        byte[] archivo = new ReportePdfService().generar(datos(List.of()));

        assertThat(textoPdf(archivo)).contains("Sin despachos en el rango");
    }
}
