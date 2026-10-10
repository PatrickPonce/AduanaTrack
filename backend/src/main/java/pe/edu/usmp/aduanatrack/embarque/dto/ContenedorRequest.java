package pe.edu.usmp.aduanatrack.embarque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.embarque.validacion.ContenedorIso6346;

/**
 * Fila de la tabla "Contenedores asociados". {@code @Pattern} revisa la forma del número
 * (3 letras, categoría U/J/Z y 7 dígitos) y {@code @ContenedorIso6346} el dígito de control.
 */
public record ContenedorRequest(
        @NotBlank(message = "Ingrese el número de contenedor")
        @Pattern(regexp = "^\\s*[A-Za-z]{3}[UJZujz]\\d{7}\\s*$",
                message = "Formato ISO 6346 no válido (ej. CSQU3054383)")
        @ContenedorIso6346
        String numero,
        @Size(max = 10) String tipo,
        @Size(max = 30) String precinto) {
}
