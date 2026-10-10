package pe.edu.usmp.aduanatrack.cliente.dto;

public record ClienteExisteResponse(boolean existe, Long clienteId, String razonSocial) {
}
