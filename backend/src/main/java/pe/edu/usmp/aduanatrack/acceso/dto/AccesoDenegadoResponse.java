package pe.edu.usmp.aduanatrack.acceso.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;

/**
 * Fila de «Intentos de acceso denegados». {@code resultado} es el motivo interno; {@code mensaje} lo que
 * recibió el cliente («Despacho no encontrado»). {@code empresaDespacho} es la dueña real del despacho.
 */
public record AccesoDenegadoResponse(
        Long id,
        LocalDateTime fecha,
        String usuario,
        Long despachoId,
        String numeroDespacho,
        Long clienteId,
        String empresaDespacho,
        MotivoAccesoDenegado resultado,
        String mensaje) {

    public AccesoDenegadoResponse(Long id, LocalDateTime fecha, String usuario, Long despachoId,
            String numeroDespacho, Long clienteId, String empresaDespacho, MotivoAccesoDenegado resultado) {
        this(id, fecha, usuario, despachoId, numeroDespacho, clienteId, empresaDespacho, resultado,
                "Despacho no encontrado");
    }
}
