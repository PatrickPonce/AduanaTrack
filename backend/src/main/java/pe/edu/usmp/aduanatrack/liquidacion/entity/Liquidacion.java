package pe.edu.usmp.aduanatrack.liquidacion.entity;

import java.math.BigDecimal;
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
import pe.edu.usmp.aduanatrack.liquidacion.dto.ResultadoCalculo;

/** Liquidación referencial del despacho (HU-08 · T-08.2); una por despacho. Montos en USD. */
@Entity
@Table(name = "liquidacion")
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false, unique = true)
    private Long despachoId;

    private BigDecimal fob;
    private BigDecimal flete;
    private BigDecimal seguro;
    private BigDecimal cif;

    @Column(name = "ad_valorem")
    private BigDecimal adValorem;

    private BigDecimal igv;
    private BigDecimal ipm;
    private BigDecimal percepcion;
    private BigDecimal total;

    @Column(name = "tasa_igv")
    private BigDecimal tasaIgv;

    @Column(name = "tasa_ipm")
    private BigDecimal tasaIpm;

    @Column(name = "tasa_percepcion")
    private BigDecimal tasaPercepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generado_por", nullable = false)
    private Usuario generadoPor;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn;

    protected Liquidacion() {
    }

    public Liquidacion(Long despachoId, ResultadoCalculo calculo, Usuario generadoPor, LocalDateTime generadoEn) {
        this.despachoId = despachoId;
        reemplazar(calculo, generadoPor, generadoEn);
    }

    /** Volver a generar la liquidación antes del pago actualiza la misma fila. */
    public void reemplazar(ResultadoCalculo calculo, Usuario generadoPor, LocalDateTime generadoEn) {
        this.fob = calculo.fob();
        this.flete = calculo.flete();
        this.seguro = calculo.seguro();
        this.cif = calculo.cif();
        this.adValorem = calculo.adValorem();
        this.igv = calculo.igv();
        this.ipm = calculo.ipm();
        this.percepcion = calculo.percepcion();
        this.total = calculo.total();
        this.tasaIgv = calculo.tasas().igv();
        this.tasaIpm = calculo.tasas().ipm();
        this.tasaPercepcion = calculo.tasas().percepcion();
        this.generadoPor = generadoPor;
        this.generadoEn = generadoEn;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public BigDecimal getFob() {
        return fob;
    }

    public BigDecimal getFlete() {
        return flete;
    }

    public BigDecimal getSeguro() {
        return seguro;
    }

    public BigDecimal getCif() {
        return cif;
    }

    public BigDecimal getAdValorem() {
        return adValorem;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public BigDecimal getIpm() {
        return ipm;
    }

    public BigDecimal getPercepcion() {
        return percepcion;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getTasaIgv() {
        return tasaIgv;
    }

    public BigDecimal getTasaIpm() {
        return tasaIpm;
    }

    public BigDecimal getTasaPercepcion() {
        return tasaPercepcion;
    }

    public Usuario getGeneradoPor() {
        return generadoPor;
    }

    public LocalDateTime getGeneradoEn() {
        return generadoEn;
    }
}
