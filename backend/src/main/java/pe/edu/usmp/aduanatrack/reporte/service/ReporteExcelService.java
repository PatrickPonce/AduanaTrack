package pe.edu.usmp.aduanatrack.reporte.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.YearMonth;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;

/**
 * Excel del reporte de gestión (HU-15 · T-15.2, CA3): la hoja «Indicadores» con el resumen del rango y la
 * hoja «Despachos» con la tabla. Mismo estilo que {@code TableroExcelService}.
 */
@Service
public class ReporteExcelService {

    static final String HOJA_INDICADORES = "Indicadores";
    static final String HOJA_DESPACHOS = "Despachos";
    static final String[] ENCABEZADOS = { "N° despacho", "Cliente", "Aduana", "Estado", "Canal", "Fecha de apertura",
            "Fecha de levante" };
    private static final int[] ANCHOS = { 18, 40, 30, 24, 10, 18, 18 };

    public byte[] generar(DatosReporte datos) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CreationHelper ayuda = libro.getCreationHelper();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle encabezado = libro.createCellStyle();
            encabezado.setFont(negrita);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy"));

            hojaIndicadores(libro.createSheet(HOJA_INDICADORES), datos, encabezado);
            hojaDespachos(libro.createSheet(HOJA_DESPACHOS), datos, encabezado, fecha);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte de gestión", e);
        }
    }

    private static void hojaIndicadores(Sheet hoja, DatosReporte datos, CellStyle encabezado) {
        int fila = 0;
        fila = titulo(hoja, fila, "Reporte de gestión aduanera", encabezado);
        fila = dato(hoja, fila, "Desde", Textos.fecha(datos.desde()));
        fila = dato(hoja, fila, "Hasta", Textos.fecha(datos.hasta()));
        fila++;
        fila = titulo(hoja, fila, "Indicadores", encabezado);
        fila = dato(hoja, fila, "Total de despachos", datos.totalDespachos());
        fila = dato(hoja, fila, "Despachos con levante", datos.conLevante());
        Row promedio = hoja.createRow(fila++);
        promedio.createCell(0).setCellValue("Tiempo promedio de despacho (días)");
        if (datos.tiempoPromedioDias() == null) {
            promedio.createCell(1).setCellValue("Sin despachos con levante");
        } else {
            promedio.createCell(1).setCellValue(datos.tiempoPromedioDias());
        }
        fila++;
        fila = titulo(hoja, fila, "Despachos por mes", encabezado);
        for (Map.Entry<YearMonth, Long> mes : datos.despachosPorMes().entrySet()) {
            fila = dato(hoja, fila, Textos.mes(mes.getKey()), mes.getValue());
        }
        fila++;
        fila = titulo(hoja, fila, "Despachos por canal", encabezado);
        for (Map.Entry<String, Long> canal : datos.porCanal().entrySet()) {
            fila = dato(hoja, fila, Textos.canal(canal.getKey()), canal.getValue());
        }
        dato(hoja, fila, "Sin canal asignado", datos.sinCanal());
        // Anchos fijos (en caracteres): autoSizeColumn necesita fuentes que la imagen JRE no trae.
        hoja.setColumnWidth(0, 38 * 256);
        hoja.setColumnWidth(1, 28 * 256);
    }

    private static void hojaDespachos(Sheet hoja, DatosReporte datos, CellStyle encabezado, CellStyle fecha) {
        Row cabecera = hoja.createRow(0);
        for (int i = 0; i < ENCABEZADOS.length; i++) {
            cabecera.createCell(i).setCellValue(ENCABEZADOS[i]);
            cabecera.getCell(i).setCellStyle(encabezado);
        }
        int numeroFila = 1;
        for (FilaReporte d : datos.despachos()) {
            Row fila = hoja.createRow(numeroFila++);
            fila.createCell(0).setCellValue(d.numero() == null ? "" : d.numero());
            fila.createCell(1).setCellValue(d.cliente());
            fila.createCell(2).setCellValue(d.aduana() == null ? "" : d.aduana());
            fila.createCell(3).setCellValue(d.estado().name());
            fila.createCell(4).setCellValue(d.canalControl() == null ? "" : d.canalControl());
            fila.createCell(5).setCellValue(d.fechaApertura());
            fila.getCell(5).setCellStyle(fecha);
            if (d.fechaLevante() != null) {
                fila.createCell(6).setCellValue(d.fechaLevante());
                fila.getCell(6).setCellStyle(fecha);
            }
        }
        for (int i = 0; i < ANCHOS.length; i++) {
            hoja.setColumnWidth(i, ANCHOS[i] * 256);
        }
        hoja.createFreezePane(0, 1);
    }

    private static int titulo(Sheet hoja, int fila, String texto, CellStyle estilo) {
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue(texto);
        row.getCell(0).setCellStyle(estilo);
        return fila + 1;
    }

    private static int dato(Sheet hoja, int fila, String etiqueta, String valor) {
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue(etiqueta);
        row.createCell(1).setCellValue(valor);
        return fila + 1;
    }

    private static int dato(Sheet hoja, int fila, String etiqueta, long valor) {
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue(etiqueta);
        row.createCell(1).setCellValue(valor);
        return fila + 1;
    }
}
