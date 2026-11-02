package pe.edu.usmp.aduanatrack.reporte.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.reporte.dto.DatosReporte;
import pe.edu.usmp.aduanatrack.reporte.service.DatosReporteService;
import pe.edu.usmp.aduanatrack.reporte.service.ReporteExcelService;
import pe.edu.usmp.aduanatrack.reporte.service.ReportePdfService;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * {@code GET /api/v1/reportes/exportar?formato=xlsx|pdf&desde=&hasta=} (contrato C10 · HU-15 · T-15.2,
 * CA3). Descarga los indicadores y la tabla de despachos del rango como {@code reporte-gestion_AAAA-MM-DD}.
 * El perfil CLIENTE no accede: el reporte reúne los despachos de todos los clientes.
 */
@RestController
@RequestMapping("/api/v1/reportes/exportar")
public class ReporteExportController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DatosReporteService datosService;
    private final ReporteExcelService excelService;
    private final ReportePdfService pdfService;
    private final Clock clock;

    public ReporteExportController(DatosReporteService datosService, ReporteExcelService excelService,
            ReportePdfService pdfService, Clock clock) {
        this.datosService = datosService;
        this.excelService = excelService;
        this.pdfService = pdfService;
        this.clock = clock;
    }

    /** Los tres parámetros son opcionales aquí para que el servicio responda 400 con el campo marcado. */
    @GetMapping
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String formato,
            @RequestParam(required = false) String desde, @RequestParam(required = false) String hasta,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        if (usuario == null || "CLIENTE".equals(usuario.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO,
                    "Solo el administrador y el agente de aduana exportan reportes.");
        }
        boolean pdf = esPdf(formato);
        DatosReporte datos = datosService.generar(desde, hasta);
        byte[] archivo = pdf ? pdfService.generar(datos) : excelService.generar(datos);
        String nombre = "reporte-gestion_" + LocalDate.now(clock) + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build()
                        .toString())
                .body(archivo);
    }

    private static boolean esPdf(String formato) {
        String valor = formato == null ? "" : formato.trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case "pdf" -> true;
            case "xlsx" -> false;
            default -> throw ApiException.validacion(Map.of("formato", "El formato debe ser xlsx o pdf"));
        };
    }
}
