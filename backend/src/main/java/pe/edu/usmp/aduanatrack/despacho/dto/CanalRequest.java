package pe.edu.usmp.aduanatrack.despacho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de PUT /despachos/{id}/canal (contrato C5 · T-10.2): VERDE, NARANJA o ROJO. */
public record CanalRequest(
        @NotBlank(message = "Seleccione el canal informado por SUNAT")
        @Pattern(regexp = "VERDE|NARANJA|ROJO", message = "El canal debe ser VERDE, NARANJA o ROJO")
        String canal) {
}
