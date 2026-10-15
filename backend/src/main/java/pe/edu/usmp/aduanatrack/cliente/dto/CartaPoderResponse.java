package pe.edu.usmp.aduanatrack.cliente.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;

/** Carta poder recién cargada y el estado en que deja al cliente (HU-04 · T-04.2). */
public record CartaPoderResponse(
        Long id,
        Long clienteId,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String nombreArchivo,
        long tamanoBytes,
        String sha256,
        EstadoCartaPoder estadoCartaPoder) {

    public static CartaPoderResponse de(CartaPoder carta, EstadoCartaPoder estado) {
        return new CartaPoderResponse(carta.getId(), carta.getClienteId(), carta.getFechaEmision(),
                carta.getFechaVencimiento(), carta.getNombreArchivo(), carta.getTamanoBytes(), carta.getSha256(),
                estado);
    }
}
