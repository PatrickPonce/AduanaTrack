package pe.edu.usmp.aduanatrack.dashboard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroResponse;

/** Exporta la tabla del Dashboard a .xlsx con las mismas filas y orden que la pantalla (HU-02). */
@Service
public class TableroExcelService {

    static final String[] ENCABEZADOS = { "N° despacho", "Cliente", "Aduana", "N° BL", "ETA", "Estado",
            "Días de almacenaje", "Alerta crítica" };
    private static final int[] ANCHOS = { 18, 40, 30, 20, 12, 12, 18, 14 };

    public byte[] generar(List<DespachoTableroResponse> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Despachos");
            CreationHelper ayuda = libro.getCreationHelper();

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle encabezado = libro.createCellStyle();
            encabezado.setFont(negrita);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy"));

            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                cabecera.createCell(i).setCellValue(ENCABEZADOS[i]);
                cabecera.getCell(i).setCellStyle(encabezado);
            }

            int numeroFila = 1;
            for (DespachoTableroResponse d : filas) {
                Row fila = hoja.createRow(numeroFila++);
                fila.createCell(0).setCellValue(d.numero());
                fila.createCell(1).setCellValue(d.cliente());
                fila.createCell(2).setCellValue(d.aduana() == null ? "" : d.aduanaCodigo() + " - " + d.aduana());
                fila.createCell(3).setCellValue(d.numeroBl() == null ? "" : d.numeroBl());
                if (d.fechaEta() != null) {
                    fila.createCell(4).setCellValue(d.fechaEta());
                    fila.getCell(4).setCellStyle(fecha);
                }
                fila.createCell(5).setCellValue(d.estado().name());
                if (d.diasAlmacenaje() != null) {
                    fila.createCell(6).setCellValue(d.diasAlmacenaje());
                }
                fila.createCell(7).setCellValue(d.alertaCritica() ? "Sí" : "No");
            }
            // Anchos fijos (en caracteres): autoSizeColumn necesita fuentes que la imagen JRE no trae.
            for (int i = 0; i < ANCHOS.length; i++) {
                hoja.setColumnWidth(i, ANCHOS[i] * 256);
            }
            hoja.createFreezePane(0, 1);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del Dashboard", e);
        }
    }
}
