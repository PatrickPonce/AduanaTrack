package pe.edu.usmp.aduanatrack.dashboard.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/** Fila tal como sale de la consulta del tablero (antes de calcular plazos y alertas). */
public record DespachoTableroFila(
        Long id,
        String numero,
        Long clienteId,
        String cliente,
        String aduanaCodigo,
        String aduana,
        String numeroBl,
        LocalDate fechaEta,
        LocalDate fechaLimiteAlmacenaje,
        EstadoDespacho estado) {
}
