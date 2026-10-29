package pe.edu.usmp.aduanatrack.documento.dto;

import java.util.List;

/** Contrato C1 · GET /despachos/{id}/documentos: un documento por tipo, en el orden del catálogo. */
public record DocumentosDespachoResponse(EstadoDocumentacion estadoGeneral, List<DocumentoRequeridoResponse> documentos) {
}
