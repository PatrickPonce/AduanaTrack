package pe.edu.usmp.aduanatrack.liquidacion.entity;

import java.math.BigDecimal;
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

/** Pago de tributos hecho fuera del sistema (HU-08 · T-08.2, CA3); uno por despacho. */
@Entity
@Table(name = "pago_tributos")
public class PagoTributos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false, unique = true)
    private Long despachoId;

    @Column(nullable = false)
    private LocalDate fecha;

    /** USD, 2 decimales. */
    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "medio_pago", nullable = false)
    private String medioPago;

    @Column(name = "numero_operacion", nullable = false)
    private String numeroOperacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    protected PagoTributos() {
    }

    public PagoTributos(Long despachoId, LocalDate fecha, BigDecimal monto, String medioPago,
            String numeroOperacion, Usuario registradoPor, LocalDateTime registradoEn) {
        this.despachoId = despachoId;
        this.fecha = fecha;
        this.monto = monto;
        this.medioPago = medioPago;
        this.numeroOperacion = numeroOperacion;
        this.registradoPor = registradoPor;
        this.registradoEn = registradoEn;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }
}
