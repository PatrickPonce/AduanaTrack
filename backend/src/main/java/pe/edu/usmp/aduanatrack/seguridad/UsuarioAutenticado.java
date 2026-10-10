package pe.edu.usmp.aduanatrack.seguridad;

/** Principal que el {@link JwtCookieFilter} deja en el SecurityContext. */
public record UsuarioAutenticado(Long id, String usuario, String nombre, String rol) {
}
