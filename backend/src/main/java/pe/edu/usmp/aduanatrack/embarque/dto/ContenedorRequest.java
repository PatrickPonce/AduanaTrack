package pe.edu.usmp.aduanatrack.embarque.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.embarque.validacion.ContenedorIso6346;

/**
 * Fila de la tabla "Contenedores asociados". {@code @Pattern} revisa la forma del número
 * (3 letras, categoría U/J/Z y 7 dígitos) y {@code @ContenedorIso6346} el dígito de control.
 * El peso bruto (kg) es opcional, como en el prototipo de HU-05.
 */
public record ContenedorRequest(
        @NotBlank(message = "Ingrese el número de contenedor")
        @Pattern(regexp = "^\\s*[A-Za-z]{3}[UJZujz]\\d{7}\\s*$",
                message = "Formato ISO 6346 no válido (ej. CSQU3054383)")
        @ContenedorIso6346
        String numero,
        @Size(max = 10) String tipo,
        @Size(max = 30) String precinto,
        @DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "Peso bruto no válido (máximo 2 decimales)")
        BigDecimal pesoBrutoKg) {
}
