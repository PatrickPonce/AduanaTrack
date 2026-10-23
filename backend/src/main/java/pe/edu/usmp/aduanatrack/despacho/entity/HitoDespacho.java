package pe.edu.usmp.aduanatrack.despacho.entity;

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
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;

/** Hito de la línea de tiempo del despacho (HU-10 · T-10.1). No se modifica después de registrarse. */
@Entity
@Table(name = "hito_despacho")
public class HitoDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoHito tipo;

    @Column(nullable = false, updatable = false)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private boolean sugerido;

    /** Nulo cuando el hito lo registra el sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected HitoDespacho() {
    }

    public HitoDespacho(Long despachoId, TipoHito tipo, String descripcion, boolean sugerido, Usuario usuario,
            LocalDateTime fecha) {
        this.despachoId = despachoId;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.sugerido = sugerido;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public TipoHito getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isSugerido() {
        return sugerido;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
