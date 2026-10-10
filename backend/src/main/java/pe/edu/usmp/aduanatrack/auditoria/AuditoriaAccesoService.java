package pe.edu.usmp.aduanatrack.auditoria;

/**
 * Registro de intentos de acceso en {@code auditoria_acceso} (T-01.2 · SCRUM-27).
 * El login (T-01.1) lo invoca en cada intento; la implementación debe usar
 * {@code @Transactional(propagation = REQUIRES_NEW)} para que el intento quede guardado
 * aunque el login termine en excepción. Nunca recibe ni guarda la contraseña.
 */
public interface AuditoriaAccesoService {

    void registrar(String usuarioIngresado, Long usuarioId, String ip, String userAgent, ResultadoAcceso resultado);
}
