package pe.edu.usmp.aduanatrack.dashboard.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/**
 * Fila de la tabla "Despachos recientes" del Dashboard (HU-02 · T-02.2).
 * {@code alertaCritica}: carta poder vencida o por vencer, o almacenaje con ≤ 3 días (sección 2.6).
 */
public record DespachoTableroResponse(
        Long id,
        String numero,
        Long clienteId,
        String cliente,
        String aduanaCodigo,
        String aduana,
        String numeroBl,
        LocalDate fechaEta,
        EstadoDespacho estado,
        Long diasAlmacenaje,
        boolean alertaAlmacenaje,
        EstadoCartaPoder estadoCartaPoder,
        boolean alertaCritica) {
}
