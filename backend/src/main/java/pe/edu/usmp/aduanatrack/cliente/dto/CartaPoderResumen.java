package pe.edu.usmp.aduanatrack.cliente.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;

/**
 * Fila de la tabla "Carta poder / autorización vigente" del cliente (prototipo de HU-04).
 * {@code estado} es el de esta carta según su propia fecha de vencimiento.
 */
public record CartaPoderResumen(
        Long id,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String nombreArchivo,
        long tamanoBytes,
        EstadoCartaPoder estado) {

    public static CartaPoderResumen de(CartaPoder carta, EstadoCartaPoder estado) {
        return new CartaPoderResumen(carta.getId(), carta.getFechaEmision(), carta.getFechaVencimiento(),
                carta.getNombreArchivo(), carta.getTamanoBytes(), estado);
    }
}
