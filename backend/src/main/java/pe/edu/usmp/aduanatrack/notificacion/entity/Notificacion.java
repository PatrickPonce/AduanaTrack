package pe.edu.usmp.aduanatrack.notificacion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;

/** Notificación de un usuario sobre un evento de sus despachos (HU-14 · SCRUM-90). */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despacho_id", updatable = false)
    private Despacho despacho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Severidad severidad;

    @Column(nullable = false, updatable = false)
    private String titulo;

    @Column(nullable = false, updatable = false)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "leida_en")
    private LocalDateTime leidaEn;

    protected Notificacion() {
    }

    public Notificacion(Long usuarioId, Despacho despacho, TipoNotificacion tipo, Severidad severidad,
            String titulo, String mensaje, LocalDateTime creadaEn) {
        this.usuarioId = usuarioId;
        this.despacho = despacho;
        this.tipo = tipo;
        this.severidad = severidad;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.creadaEn = creadaEn;
    }

    /** CA4: la primera lectura queda registrada; volver a marcarla no cambia la fecha. */
    public void marcarLeida(LocalDateTime ahora) {
        if (!leida) {
            leida = true;
            leidaEn = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public Severidad getSeveridad() {
        return severidad;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public LocalDateTime getLeidaEn() {
        return leidaEn;
    }
}
