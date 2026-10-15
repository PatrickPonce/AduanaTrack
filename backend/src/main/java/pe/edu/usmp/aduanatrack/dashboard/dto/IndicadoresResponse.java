package pe.edu.usmp.aduanatrack.dashboard.dto;

/**
 * Tarjetas del Dashboard (HU-02 · T-02.1), sobre los despachos no borradores que cumplen los filtros.
 * {@code despachosActivos}: aún sin levante. {@code conAlertasCriticas}: la misma regla que
 * {@code alertaCritica} en la tabla (carta poder vencida o por vencer, o almacenaje con ≤ 3 días).
 */
public record IndicadoresResponse(
        long despachosActivos,
        long enCanalNaranjaRojo,
        long conLevante,
        long conAlertasCriticas) {
}
