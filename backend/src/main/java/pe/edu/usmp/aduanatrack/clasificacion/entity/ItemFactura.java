package pe.edu.usmp.aduanatrack.clasificacion.entity;

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

/** Ítem de la factura comercial del despacho (HU-07 · T-07.2). */
@Entity
@Table(name = "item_factura")
public class ItemFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    /** Posición en la factura (1, 2, 3…); la asigna el backend según el orden de la lista. */
    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private String unidad;

    /** USD, 2 decimales. */
    @Column(name = "valor_fob", nullable = false)
    private BigDecimal valorFob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id")
    private PartidaReferencial partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clasificado_por")
    private Usuario clasificadoPor;

    @Column(name = "clasificado_en")
    private LocalDateTime clasificadoEn;

    protected ItemFactura() {
    }

    public ItemFactura(Long despachoId, int numero, String descripcion, BigDecimal cantidad, String unidad,
            BigDecimal valorFob) {
        this.despachoId = despachoId;
        actualizar(numero, descripcion, cantidad, unidad, valorFob);
    }

    public void actualizar(int numero, String descripcion, BigDecimal cantidad, String unidad, BigDecimal valorFob) {
        this.numero = numero;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.valorFob = valorFob;
    }

    /** Asigna la partida y deja constancia de quién y cuándo clasificó (CA2). */
    public void asignarPartida(PartidaReferencial nueva, Usuario usuario, LocalDateTime cuando) {
        this.partida = nueva;
        this.clasificadoPor = usuario;
        this.clasificadoEn = cuando;
    }

    public void quitarPartida() {
        this.partida = null;
        this.clasificadoPor = null;
        this.clasificadoEn = null;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public BigDecimal getValorFob() {
        return valorFob;
    }

    public PartidaReferencial getPartida() {
        return partida;
    }

    public Usuario getClasificadoPor() {
        return clasificadoPor;
    }

    public LocalDateTime getClasificadoEn() {
        return clasificadoEn;
    }
}
