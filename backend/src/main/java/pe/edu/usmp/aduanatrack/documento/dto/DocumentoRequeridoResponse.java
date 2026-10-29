package pe.edu.usmp.aduanatrack.documento.dto;

import java.time.LocalDateTime;

/**
 * Fila de «Documentos requeridos del despacho» (contrato C1). Mientras el documento está
 * PENDIENTE, los datos de la versión vigente ({@code version} a {@code cargadoEn}) son nulos.
 */
public record DocumentoRequeridoResponse(
        String tipo,
        String nombre,
        boolean obligatorio,
        EstadoDocumento estado,
        Integer version,
        Long documentoId,
        String archivo,
        Long tamanoBytes,
        String cargadoPor,
        LocalDateTime cargadoEn) {

    public static DocumentoRequeridoResponse pendiente(String tipo, String nombre, boolean obligatorio) {
        return new DocumentoRequeridoResponse(tipo, nombre, obligatorio, EstadoDocumento.PENDIENTE, null, null, null,
                null, null, null);
    }
}
