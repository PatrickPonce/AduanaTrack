package pe.edu.usmp.aduanatrack.config.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parámetros configurables (DIAS_ALMACENAJE, DIAS_ALERTA_ALMACENAJE, DIAS_ALERTA_CARTA_PODER). */
@Entity
@Table(name = "parametro_sistema")
public class ParametroSistema {

    @Id
    private String clave;

    @Column(nullable = false)
    private String valor;

    private String descripcion;

    protected ParametroSistema() {
    }

    public ParametroSistema(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        return valor;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
