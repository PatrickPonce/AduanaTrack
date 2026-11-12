package pe.edu.usmp.aduanatrack.reporte.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.util.Map;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.dto.FilaReporte;

/**
 * PDF del reporte de gestión (HU-15 · T-15.2, CA3) con OpenPDF: el resumen de indicadores y la tabla de
 * despachos del rango. Usa solo fuentes estándar del PDF, así no depende de las fuentes del sistema.
 */
@Service
public class ReportePdfService {

    private static final Color FONDO_ENCABEZADO = new Color(0xE2, 0xE8, 0xF0);
    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 10);
    private static final Font TEXTO_CHICO = new Font(Font.HELVETICA, 8);
    private static final Font ENCABEZADO = new Font(Font.HELVETICA, 8, Font.BOLD);

    public byte[] generar(DatosReporte datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Reporte de gestión aduanera");
            documento.open();

            documento.add(new Paragraph("Reporte de gestión aduanera", TITULO));
            Paragraph rango = new Paragraph("Del " + Textos.fecha(datos.desde()) + " al " + Textos.fecha(datos.hasta()),
                    TEXTO);
            rango.setSpacingAfter(12);
            documento.add(rango);

            documento.add(new Paragraph("Indicadores", SUBTITULO));
            documento.add(resumen(datos));

            documento.add(espaciado(new Paragraph("Despachos por mes", SUBTITULO)));
            documento.add(conteos(datos.despachosPorMes().isEmpty() ? Map.of() : porMes(datos), "Mes"));

            documento.add(espaciado(new Paragraph("Despachos por canal", SUBTITULO)));
            documento.add(conteos(porCanal(datos), "Canal"));

            documento.add(espaciado(new Paragraph("Despachos del rango (" + datos.totalDespachos() + ")", SUBTITULO)));
            documento.add(tablaDespachos(datos));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte de gestión", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static PdfPTable resumen(DatosReporte datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 3, 1 });
        tabla.setWidthPercentage(50);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        fila(tabla, "Total de despachos", String.valueOf(datos.totalDespachos()));
        fila(tabla, "Despachos con levante", String.valueOf(datos.conLevante()));
        fila(tabla, "Tiempo promedio de despacho (días)", Textos.dias(datos.tiempoPromedioDias()));
        return tabla;
    }

    private static Map<String, Long> porMes(DatosReporte datos) {
        Map<String, Long> filas = new java.util.LinkedHashMap<>();
        for (Map.Entry<YearMonth, Long> mes : datos.despachosPorMes().entrySet()) {
            filas.put(Textos.mes(mes.getKey()), mes.getValue());
        }
        return filas;
    }

    private static Map<String, Long> porCanal(DatosReporte datos) {
        Map<String, Long> filas = new java.util.LinkedHashMap<>();
        datos.porCanal().forEach((canal, cantidad) -> filas.put(Textos.canal(canal), cantidad));
        filas.put("Sin canal asignado", datos.sinCanal());
        return filas;
    }

    private static PdfPTable conteos(Map<String, Long> filas, String columna) {
        PdfPTable tabla = new PdfPTable(new float[] { 3, 1 });
        tabla.setWidthPercentage(50);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        encabezado(tabla, columna);
        encabezado(tabla, "Despachos");
        if (filas.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("Sin despachos en el rango", TEXTO));
            vacia.setColspan(2);
            tabla.addCell(vacia);
        }
        filas.forEach((nombre, cantidad) -> fila(tabla, nombre, String.valueOf(cantidad)));
        return tabla;
    }

    private static PdfPTable tablaDespachos(DatosReporte datos) {
        PdfPTable tabla = new PdfPTable(new float[] { 2.2f, 5, 3.5f, 3, 1.6f, 2.2f, 2.2f });
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : ReporteExcelService.ENCABEZADOS) {
            encabezado(tabla, titulo);
        }
        for (FilaReporte d : datos.despachos()) {
            celda(tabla, d.numero() == null ? "" : d.numero());
            celda(tabla, d.cliente());
            celda(tabla, d.aduana() == null ? "" : d.aduana());
            celda(tabla, d.estado().name());
            celda(tabla, d.canalControl() == null ? "" : d.canalControl());
            celda(tabla, Textos.fecha(d.fechaApertura()));
            celda(tabla, Textos.fecha(d.fechaLevante()));
        }
        return tabla;
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        tabla.addCell(new PdfPCell(new Phrase(etiqueta, TEXTO)));
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, TEXTO));
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(celdaValor);
    }

    private static void encabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ENCABEZADO));
        celda.setBackgroundColor(FONDO_ENCABEZADO);
        tabla.addCell(celda);
    }

    private static void celda(PdfPTable tabla, String texto) {
        tabla.addCell(new PdfPCell(new Phrase(texto, TEXTO_CHICO)));
    }

    private static Paragraph espaciado(Paragraph parrafo) {
        parrafo.setSpacingBefore(14);
        parrafo.setSpacingAfter(4);
        return parrafo;
    }
}
