package pe.edu.usmp.aduanatrack.liquidacion.dto;

/** Respuesta de POST /despachos/{id}/pago (contrato C3 · T-08.2): estado del despacho ({@code TRIBUTOS_PAGADOS}). */
public record PagoResponse(String estado) {
}
