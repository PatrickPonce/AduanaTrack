package pe.edu.usmp.aduanatrack.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code usuario} acepta el nombre de usuario o el correo corporativo. */
public record LoginRequest(
        @NotBlank(message = "Ingrese su usuario o correo corporativo") @Size(max = 150) String usuario,
        @NotBlank(message = "Ingrese su contraseña") @Size(max = 128) String password,
        boolean recordarme) {

    @Override
    public String toString() {
        return "LoginRequest[usuario=" + usuario + ", recordarme=" + recordarme + "]";
    }
}
