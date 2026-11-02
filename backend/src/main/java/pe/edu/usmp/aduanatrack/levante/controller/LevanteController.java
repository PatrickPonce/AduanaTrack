package pe.edu.usmp.aduanatrack.levante.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.levante.dto.LevanteRequest;
import pe.edu.usmp.aduanatrack.levante.dto.LevanteResponse;
import pe.edu.usmp.aduanatrack.levante.service.LevanteService;
import pe.edu.usmp.aduanatrack.levante.service.LevanteService.ArchivoConstancia;

/** Levante del despacho (HU-13, contrato C8). El POST lo restringe SecurityConfig a ADMIN y AGENTE. */
@RestController
@RequestMapping("/api/v1/despachos/{despachoId}/levante")
public class LevanteController {

    private final LevanteService levanteService;

    public LevanteController(LevanteService levanteService) {
        this.levanteService = levanteService;
    }

    @GetMapping
    public LevanteResponse obtener(@PathVariable Long despachoId) {
        return levanteService.obtener(despachoId);
    }

    /**
     * Multipart: {@code datos} (JSON con {@code numeroLevante}, {@code fechaLevante} y
     * {@code confirmarSinRequisitos}) y {@code constancia} (PDF, opcional). {@code datos} es opcional aquí
     * para que la falta de la parte responda 400 con los campos marcados y no un error genérico.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public LevanteResponse registrar(@PathVariable Long despachoId,
            @RequestPart(value = "datos", required = false) @Valid LevanteRequest datos,
            @RequestPart(value = "constancia", required = false) MultipartFile constancia) {
        if (datos == null) {
            throw ApiException.validacion(Map.of("numeroLevante", "Ingrese el N° de levante", "fechaLevante",
                    "Ingrese la fecha del levante"));
        }
        return levanteService.registrar(despachoId, datos, constancia);
    }

    @GetMapping("/constancia")
    public ResponseEntity<Resource> constancia(@PathVariable Long despachoId) {
        ArchivoConstancia archivo = levanteService.constancia(despachoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(archivo.ruta()));
    }
}
