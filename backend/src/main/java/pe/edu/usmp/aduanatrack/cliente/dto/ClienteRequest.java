package pe.edu.usmp.aduanatrack.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.cliente.validacion.RucValido;

public record ClienteRequest(
        @NotBlank(message = "Ingrese el RUC")
        @Pattern(regexp = "\\d{11}", message = "Debe tener 11 dígitos")
        @RucValido
        String ruc,

        @NotBlank(message = "Ingrese la razón social") @Size(max = 200) String razonSocial,
        @Size(max = 250) String direccion,
        @Email(message = "Correo no válido") @Size(max = 150) String correo,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,20}$", message = "Teléfono no válido") String telefono,
        @Size(max = 150) String contacto) {
}
