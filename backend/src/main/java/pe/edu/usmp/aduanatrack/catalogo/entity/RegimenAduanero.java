package pe.edu.usmp.aduanatrack.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lista maestra de regímenes aduaneros (datos semilla y endpoint en T-03.2). */
@Entity
@Table(name = "regimen_aduanero")
public class RegimenAduanero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    protected RegimenAduanero() {
    }

    public RegimenAduanero(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }
}
