package pe.edu.usmp.aduanatrack.liquidacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /despachos/{id}/pago (contrato C3 · T-08.2): el pago se hizo fuera del sistema. */
public record PagoRequest(
        @NotNull(message = "Ingrese la fecha del pago")
        LocalDate fecha,
        @NotNull(message = "Ingrese el monto pagado")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        @Digits(integer = 13, fraction = 2, message = "Monto no válido (USD, máximo 2 decimales)")
        BigDecimal monto,
        @NotBlank(message = "Seleccione el medio de pago")
        @Size(max = 30, message = "Máximo 30 caracteres")
        String medioPago,
        @NotBlank(message = "Ingrese el número de operación")
        @Size(max = 50, message = "Máximo 50 caracteres")
        String numeroOperacion) {
}
