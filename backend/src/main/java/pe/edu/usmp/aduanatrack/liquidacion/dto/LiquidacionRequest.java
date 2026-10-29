package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de POST /despachos/{id}/liquidacion (contrato C3 · T-08.2): flete y seguro en USD. */
public record LiquidacionRequest(
        @NotNull(message = "Ingrese el flete")
        @DecimalMin(value = "0.00", message = "El flete no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "Flete no válido (USD, máximo 2 decimales)")
        BigDecimal flete,
        @NotNull(message = "Ingrese el seguro")
        @DecimalMin(value = "0.00", message = "El seguro no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "Seguro no válido (USD, máximo 2 decimales)")
        BigDecimal seguro) {
}
