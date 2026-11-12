package pe.edu.usmp.aduanatrack.clasificacion.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Subpartida nacional del catálogo interno referencial (HU-07 · T-07.1), cargado con el Arancel de
 * Aduanas 2022 (V011). Es una copia local: no se consulta en línea a SUNAT.
 */
@Entity
@Table(name = "partida_referencial")
public class PartidaReferencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 10 dígitos sin separadores, p. ej. {@code 8482100000}. */
    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    /** Porcentaje Ad Valorem: {@code 6.00} es 6 %. */
    @Column(name = "tasa_ad_valorem", nullable = false)
    private BigDecimal tasaAdValorem;

    /** Descripción en minúsculas y sin tildes para el buscador. */
    @Column(nullable = false)
    private String busqueda;

    @Column(nullable = false)
    private boolean activo = true;

    protected PartidaReferencial() {
    }

    public PartidaReferencial(String codigo, String descripcion, BigDecimal tasaAdValorem, String busqueda) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tasaAdValorem = tasaAdValorem;
        this.busqueda = busqueda;
    }

    /** {@code 8482100000} → {@code 8482.10.00.00}. */
    public String getCodigoFormateado() {
        return codigo.substring(0, 4) + "." + codigo.substring(4, 6) + "." + codigo.substring(6, 8) + "."
                + codigo.substring(8, 10);
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

    public BigDecimal getTasaAdValorem() {
        return tasaAdValorem;
    }

    public boolean isActivo() {
        return activo;
    }
}
