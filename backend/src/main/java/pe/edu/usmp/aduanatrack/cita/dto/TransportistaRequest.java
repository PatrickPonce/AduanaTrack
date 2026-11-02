package pe.edu.usmp.aduanatrack.cita.dto;

/**
 * Transportista que retira la mercancía (contrato C6). Obligatorio solo en las citas de retiro: el
 * servicio valida sus campos y marca {@code transportista.ruc}, {@code transportista.placa}, etc.
 */
public record TransportistaRequest(String ruc, String razonSocial, String placa, String conductor) {
}
