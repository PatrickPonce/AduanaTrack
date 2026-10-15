package pe.edu.usmp.aduanatrack.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

class TableroExcelServiceTest {

    @Test
    void generaLasMismasFilasQueLaTablaConEncabezadosEnNegrita() throws Exception {
        List<DespachoTableroResponse> filas = List.of(
                new DespachoTableroResponse(1L, "DESP-2026-0001", 1L, "Importadora Andina SAC", "118",
                        "Marítima del Callao", "HBL-2026-00125", "CSQU3054383", 2, LocalDate.of(2026, 10, 15),
                        EstadoDespacho.APERTURA, null, 36L, false, EstadoCartaPoder.VIGENTE, false),
                new DespachoTableroResponse(2L, "DESP-2026-0002", 2L, "Comercial Lima SA", null, null, null, null, 0,
                        null, EstadoDespacho.APERTURA, "ROJO", null, false, EstadoCartaPoder.VENCIDA, true));

        byte[] archivo = new TableroExcelService().generar(filas);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getLastRowNum()).isEqualTo(2);
            Row cabecera = hoja.getRow(0);
            assertThat(cabecera.getCell(0).getStringCellValue()).isEqualTo("N° despacho");
            assertThat(libro.getFontAt(cabecera.getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();

            Row primera = hoja.getRow(1);
            assertThat(primera.getCell(0).getStringCellValue()).isEqualTo("DESP-2026-0001");
            assertThat(primera.getCell(2).getStringCellValue()).isEqualTo("118 - Marítima del Callao");
            assertThat(primera.getCell(4).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 10, 15));
            assertThat(primera.getCell(6).getNumericCellValue()).isEqualTo(36);
            assertThat(hoja.getRow(2).getCell(7).getStringCellValue()).isEqualTo("Sí");
            assertThat(primera.getCell(8).getStringCellValue()).isEqualTo("CSQU3054383 (+1)");
            assertThat(hoja.getRow(2).getCell(9).getStringCellValue()).isEqualTo("ROJO");
        }
    }
}
