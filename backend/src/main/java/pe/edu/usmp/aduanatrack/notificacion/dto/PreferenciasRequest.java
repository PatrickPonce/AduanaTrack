package pe.edu.usmp.aduanatrack.notificacion.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;

/** Preferencias del usuario en sesión (contrato C9). */
public record PreferenciasRequest(
        @NotNull(message = "Seleccione el canal de notificación") CanalNotificacion canalNotificacion) {
}
