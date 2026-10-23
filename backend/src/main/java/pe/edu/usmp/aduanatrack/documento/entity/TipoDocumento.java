package pe.edu.usmp.aduanatrack.documento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Documento que puede o debe tener el despacho (catálogo de HU-06, semilla en V006). */
@Entity
@Table(name = "tipo_documento")
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private boolean obligatorio;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    protected TipoDocumento() {
    }

    public TipoDocumento(String codigo, String nombre, boolean obligatorio, int orden) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.obligatorio = obligatorio;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
