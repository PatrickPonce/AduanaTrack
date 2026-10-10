package pe.edu.usmp.aduanatrack.dashboard.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroResponse;
import pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse;
import pe.edu.usmp.aduanatrack.dashboard.service.IndicadoresService;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroExcelService;
import pe.edu.usmp.aduanatrack.dashboard.service.TableroService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IndicadoresService indicadoresService;
    private final TableroService tableroService;
    private final TableroExcelService excelService;
    private final Clock clock;

    public DashboardController(IndicadoresService indicadoresService, TableroService tableroService,
            TableroExcelService excelService, Clock clock) {
        this.indicadoresService = indicadoresService;
        this.tableroService = tableroService;
        this.excelService = excelService;
        this.clock = clock;
    }

    /** Mismos filtros que la tabla. */
    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores(@RequestParam(required = false) Long aduanaId,
            @RequestParam(required = false) Long clienteId) {
        return indicadoresService.indicadores(aduanaId, clienteId);
    }

    @GetMapping("/despachos")
    public List<DespachoTableroResponse> despachos(@RequestParam(required = false) Long aduanaId,
            @RequestParam(required = false) Long clienteId) {
        return tableroService.despachos(aduanaId, clienteId);
    }

    /** Mismos filtros y filas que la tabla, en .xlsx. */
    @GetMapping("/despachos/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) Long aduanaId,
            @RequestParam(required = false) Long clienteId) {
        byte[] archivo = excelService.generar(tableroService.despachos(aduanaId, clienteId));
        String nombre = "despachos-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build()
                        .toString())
                .body(archivo);
    }
}
