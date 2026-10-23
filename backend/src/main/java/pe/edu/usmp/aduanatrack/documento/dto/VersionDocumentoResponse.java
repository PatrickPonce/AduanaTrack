package pe.edu.usmp.aduanatrack.documento.dto;

import java.time.LocalDateTime;

/** Contrato C1 · GET /despachos/{id}/documentos/{tipo}/versiones, de la más reciente a la más antigua. */
public record VersionDocumentoResponse(
        Long documentoId,
        int version,
        boolean vigente,
        String archivo,
        long tamanoBytes,
        String cargadoPor,
        LocalDateTime cargadoEn) {
}
