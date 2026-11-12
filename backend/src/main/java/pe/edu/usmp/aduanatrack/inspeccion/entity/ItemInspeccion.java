package pe.edu.usmp.aduanatrack.inspeccion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un ítem del checklist de la inspección (HU-12 · T-12.1). La observación solo existe si no es conforme. */
@Entity
@Table(name = "inspeccion_item")
public class ItemInspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private boolean conforme;

    private String observacion;

    protected ItemInspeccion() {
    }

    public ItemInspeccion(int orden, String descripcion, boolean conforme, String observacion) {
        this.orden = orden;
        this.descripcion = descripcion;
        this.conforme = conforme;
        this.observacion = conforme ? null : observacion;
    }

    public int getOrden() {
        return orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isConforme() {
        return conforme;
    }

    public String getObservacion() {
        return observacion;
    }
}
