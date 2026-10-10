package pe.edu.usmp.aduanatrack.auth.dto;

import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

public record UsuarioSesionResponse(String usuario, String nombre, String rol) {

    public static UsuarioSesionResponse de(UsuarioAutenticado u) {
        return new UsuarioSesionResponse(u.usuario(), u.nombre(), u.rol());
    }
}
