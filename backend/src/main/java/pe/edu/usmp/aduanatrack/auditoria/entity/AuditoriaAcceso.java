package pe.edu.usmp.aduanatrack.auditoria.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.usmp.aduanatrack.auditoria.ResultadoAcceso;

/** Un intento de inicio de sesión (HU-01 · T-01.2). Nunca guarda la contraseña. */
@Entity
@Table(name = "auditoria_acceso")
public class AuditoriaAcceso {

    public static final int MAX_USUARIO_INGRESADO = 150;
    public static final int MAX_IP = 45;
    public static final int MAX_USER_AGENT = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texto que se escribió en el campo usuario (nombre de usuario o correo). */
    @Column(name = "usuario_ingresado", nullable = false, length = MAX_USUARIO_INGRESADO)
    private String usuarioIngresado;

    /** Nulo cuando el usuario ingresado no existe. */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = MAX_IP)
    private String ip;

    @Column(name = "user_agent", length = MAX_USER_AGENT)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoAcceso resultado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected AuditoriaAcceso() {
    }

    public AuditoriaAcceso(String usuarioIngresado, Long usuarioId, String ip, String userAgent,
            ResultadoAcceso resultado, LocalDateTime fecha) {
        this.usuarioIngresado = usuarioIngresado;
        this.usuarioId = usuarioId;
        this.ip = ip;
        this.userAgent = userAgent;
        this.resultado = resultado;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getUsuarioIngresado() {
        return usuarioIngresado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public ResultadoAcceso getResultado() {
        return resultado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
