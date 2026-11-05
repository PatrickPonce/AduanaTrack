package pe.edu.usmp.aduanatrack.notificacion.dto;

import pe.edu.usmp.aduanatrack.notificacion.entity.CanalNotificacion;

/** Preferencias del usuario en sesión (contrato C9). */
public record PreferenciasResponse(CanalNotificacion canalNotificacion) {
}
