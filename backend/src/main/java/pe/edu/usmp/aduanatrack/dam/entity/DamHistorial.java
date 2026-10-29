package pe.edu.usmp.aduanatrack.dam.entity;

import java.time.LocalDate;
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
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;

/** Rectificación del N° de DAM o de su fecha de numeración (HU-09 · SCRUM-67): valor anterior, motivo y autor. */
@Entity
@Table(name = "dam_historial")
public class DamHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    @Column(name = "numero_anterior", nullable = false, updatable = false)
    private String numeroAnterior;

    @Column(name = "fecha_anterior", updatable = false)
    private LocalDate fechaAnterior;

    @Column(name = "numero_nuevo", nullable = false, updatable = false)
    private String numeroNuevo;

    @Column(name = "fecha_nueva", updatable = false)
    private LocalDate fechaNueva;

    @Column(nullable = false, updatable = false)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rectificado_por", nullable = false, updatable = false)
    private Usuario rectificadoPor;

    @Column(name = "rectificado_en", nullable = false, updatable = false)
    private LocalDateTime rectificadoEn;

    protected DamHistorial() {
    }

    public DamHistorial(Long despachoId, String numeroAnterior, LocalDate fechaAnterior, String numeroNuevo,
            LocalDate fechaNueva, String motivo, Usuario rectificadoPor, LocalDateTime rectificadoEn) {
        this.despachoId = despachoId;
        this.numeroAnterior = numeroAnterior;
        this.fechaAnterior = fechaAnterior;
        this.numeroNuevo = numeroNuevo;
        this.fechaNueva = fechaNueva;
        this.motivo = motivo;
        this.rectificadoPor = rectificadoPor;
        this.rectificadoEn = rectificadoEn;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public String getNumeroAnterior() {
        return numeroAnterior;
    }

    public LocalDate getFechaAnterior() {
        return fechaAnterior;
    }

    public String getNumeroNuevo() {
        return numeroNuevo;
    }

    public LocalDate getFechaNueva() {
        return fechaNueva;
    }

    public String getMotivo() {
        return motivo;
    }

    public Usuario getRectificadoPor() {
        return rectificadoPor;
    }

    public LocalDateTime getRectificadoEn() {
        return rectificadoEn;
    }
}
