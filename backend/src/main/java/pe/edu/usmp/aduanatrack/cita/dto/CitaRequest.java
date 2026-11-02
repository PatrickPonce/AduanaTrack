package pe.edu.usmp.aduanatrack.cita.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.cita.entity.TipoCita;

/** Cuerpo de {@code POST /despachos/{id}/citas} (contrato C6 · HU-11). */
public record CitaRequest(
        @NotNull(message = "Seleccione el depósito o almacén")
        Long depositoId,
        @NotBlank(message = "Ingrese el N° de cita o ticket")
        @Size(max = 30, message = "Máximo 30 caracteres")
        String numeroCita,
        @NotNull(message = "Seleccione el tipo de cita")
        TipoCita tipo,
        @NotNull(message = "Ingrese la fecha y hora confirmada de la cita")
        LocalDateTime fechaHoraConfirmada,
        TransportistaRequest transportista) {
}
