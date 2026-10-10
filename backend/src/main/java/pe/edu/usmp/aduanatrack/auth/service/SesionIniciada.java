package pe.edu.usmp.aduanatrack.auth.service;

import java.time.Duration;

import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** Resultado de un login exitoso: el JWT y cuánto dura la sesión. */
public record SesionIniciada(UsuarioAutenticado usuario, String token, Duration duracion) {
}
