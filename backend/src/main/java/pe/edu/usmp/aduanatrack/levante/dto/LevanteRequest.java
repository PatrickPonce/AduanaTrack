package pe.edu.usmp.aduanatrack.levante.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Parte JSON {@code datos} de {@code POST /despachos/{id}/levante} (contrato C8 · HU-13). La constancia
 * en PDF viaja aparte, en la parte {@code constancia}. {@code confirmarSinRequisitos} es {@code true}
 * cuando el agente acepta registrar el levante pese a los requisitos pendientes (CA2).
 */
public record LevanteRequest(
        @NotBlank(message = "Ingrese el N° de levante")
        @Size(max = 30, message = "Máximo 30 caracteres")
        String numeroLevante,
        @NotNull(message = "Ingrese la fecha del levante")
        LocalDate fechaLevante,
        Boolean confirmarSinRequisitos) {

    public boolean confirmaSinRequisitos() {
        return Boolean.TRUE.equals(confirmarSinRequisitos);
    }
}
