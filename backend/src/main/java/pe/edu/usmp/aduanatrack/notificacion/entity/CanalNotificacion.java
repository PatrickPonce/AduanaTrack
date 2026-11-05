package pe.edu.usmp.aduanatrack.notificacion.entity;

/** Canal preferido del usuario para recibir notificaciones (HU-14, columna usuario.canal_notificacion). */
public enum CanalNotificacion {
    SISTEMA,
    CORREO,
    AMBOS;

    /** El aviso también debe salir por correo (lo envía CorreoService cuando está habilitado). */
    public boolean incluyeCorreo() {
        return this != SISTEMA;
    }
}
