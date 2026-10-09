package pe.edu.usmp.aduanatrack.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    /** Intentos fallidos consecutivos que provocan el bloqueo de la cuenta (HU-01, CA3). */
    public static final int MAX_INTENTOS_FALLIDOS = 3;
    /** Duración del bloqueo de la cuenta en minutos (HU-01, CA3). */
    public static final int MINUTOS_BLOQUEO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String usuario;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    protected Usuario() {
    }

    public Usuario(String usuario, String correo, String nombre, String passwordHash, Rol rol) {
        this.usuario = usuario;
        this.correo = correo;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /**
     * Suma un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el contador
     * para que, al vencer el bloqueo, el usuario vuelva a tener los 3 intentos.
     *
     * @return {@code true} si este intento dejó la cuenta bloqueada
     */
    public boolean registrarIntentoFallido(LocalDateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            bloqueadoHasta = ahora.plusMinutes(MINUTOS_BLOQUEO);
            intentosFallidos = 0;
            return true;
        }
        return false;
    }

    public void registrarAccesoExitoso() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
