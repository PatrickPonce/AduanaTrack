package pe.edu.usmp.aduanatrack.acceso.dto;

import java.time.LocalDateTime;

/**
 * Fila de «Usuarios cliente». {@code estado}: ASIGNADO o SIN_ASIGNAR. Sin empresa, {@code clienteId},
 * {@code ruc}, {@code razonSocial} y {@code asignadoEn} vienen nulos.
 */
public record UsuarioClienteResponse(
        Long id,
        String usuario,
        String correo,
        String nombre,
        boolean activo,
        Long clienteId,
        String ruc,
        String razonSocial,
        LocalDateTime asignadoEn,
        String estado) {
}
