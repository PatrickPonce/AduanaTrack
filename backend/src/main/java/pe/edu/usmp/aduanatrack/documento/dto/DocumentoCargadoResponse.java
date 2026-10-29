package pe.edu.usmp.aduanatrack.documento.dto;

/** Contrato C1 · 201 de POST /despachos/{id}/documentos/{tipo}. */
public record DocumentoCargadoResponse(Long documentoId, String tipo, int version, EstadoDocumento estado) {
}
