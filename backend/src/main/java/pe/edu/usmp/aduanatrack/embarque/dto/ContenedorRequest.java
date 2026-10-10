package pe.edu.usmp.aduanatrack.embarque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fila de la tabla "Contenedores asociados". Aquí solo se revisa la forma del número
 * (3 letras, categoría U/J/Z y 7 dígitos); el dígito de control ISO 6346 lo agrega la
 * anotación {@code @ContenedorIso6346} de T-05.2 sobre este mismo campo.
 */
public record ContenedorRequest(
        @NotBlank(message = "Ingrese el número de contenedor")
        @Pattern(regexp = "^\\s*[A-Za-z]{3}[UJZujz]\\d{7}\\s*$",
                message = "Formato ISO 6346 no válido (ej. CSQU3054383)")
        String numero,
        @Size(max = 10) String tipo,
        @Size(max = 30) String precinto) {
}
