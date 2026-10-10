package pe.edu.usmp.aduanatrack.embarque.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.embarque.entity.TipoBl;

/** Obligatorios (HU-05, CA4): N° de BL, tipo de BL, naviera, puerto de embarque, ETA y al menos 1 contenedor. */
public record EmbarqueRequest(
        @NotBlank(message = "Ingrese el N° de BL") @Size(max = 50) String numeroBl,
        @NotNull(message = "Seleccione el tipo de BL") TipoBl tipoBl,
        @NotBlank(message = "Ingrese la naviera") @Size(max = 100) String naviera,
        @Size(max = 100) String buque,
        @NotBlank(message = "Ingrese el puerto de embarque") @Size(max = 100) String puertoEmbarque,
        @Size(max = 100) String puertoDescarga,
        LocalDate fechaEmbarque,
        @NotNull(message = "Ingrese la fecha ETA") LocalDate fechaEta,
        @NotEmpty(message = "Registre al menos un contenedor") List<@Valid ContenedorRequest> contenedores) {
}
