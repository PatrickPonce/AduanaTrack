package pe.edu.usmp.aduanatrack.acceso.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarEmpresaRequest(@NotNull(message = "Campo obligatorio") Long clienteId) {
}
