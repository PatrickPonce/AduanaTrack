package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET /despachos/{id}/liquidacion (contrato C3): la liquidación guardada y el pago registrado, para
 * volver a verlos después de generarla. Los campos son nulos mientras no existan.
 */
public record LiquidacionConsultaResponse(
        LiquidacionResponse liquidacion,
        String generadoPor,
        LocalDateTime generadoEn,
        PagoConsulta pago) {

    public record PagoConsulta(
            LocalDate fecha,
            BigDecimal monto,
            String medioPago,
            String numeroOperacion,
            String registradoPor,
            LocalDateTime registradoEn) {
    }
}
