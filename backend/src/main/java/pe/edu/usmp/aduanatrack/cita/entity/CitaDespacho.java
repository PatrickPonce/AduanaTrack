package pe.edu.usmp.aduanatrack.cita.entity;

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

/** Cita de inspección o de retiro coordinada con un depósito (HU-11 · SCRUM-78). */
@Entity
@Table(name = "cita_despacho")
public class CitaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposito_id", nullable = false, updatable = false)
    private Deposito deposito;

    @Column(name = "numero_cita", nullable = false)
    private String numeroCita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCita tipo;

    @Column(name = "fecha_hora_confirmada", nullable = false)
    private LocalDateTime fechaHoraConfirmada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCita estado;

    @Column(name = "transportista_ruc")
    private String transportistaRuc;

    @Column(name = "transportista_razon_social")
    private String transportistaRazonSocial;

    @Column(name = "transportista_placa")
    private String transportistaPlaca;

    @Column(name = "transportista_conductor")
    private String transportistaConductor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false, updatable = false)
    private Usuario registradoPor;

    @Column(name = "registrado_en", nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    protected CitaDespacho() {
    }

    public CitaDespacho(Long despachoId, Deposito deposito, String numeroCita, TipoCita tipo,
            LocalDateTime fechaHoraConfirmada, Usuario registradoPor, LocalDateTime registradoEn) {
        this.despachoId = despachoId;
        this.deposito = deposito;
        this.numeroCita = numeroCita;
        this.tipo = tipo;
        this.fechaHoraConfirmada = fechaHoraConfirmada;
        this.estado = EstadoCita.PROGRAMADA;
        this.registradoPor = registradoPor;
        this.registradoEn = registradoEn;
    }

    /** Datos del transportista que retira la mercancía (solo en las citas de retiro). */
    public void asignarTransportista(String ruc, String razonSocial, String placa, String conductor) {
        this.transportistaRuc = ruc;
        this.transportistaRazonSocial = razonSocial;
        this.transportistaPlaca = placa;
        this.transportistaConductor = conductor;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public Deposito getDeposito() {
        return deposito;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public TipoCita getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaHoraConfirmada() {
        return fechaHoraConfirmada;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public String getTransportistaRuc() {
        return transportistaRuc;
    }

    public String getTransportistaRazonSocial() {
        return transportistaRazonSocial;
    }

    public String getTransportistaPlaca() {
        return transportistaPlaca;
    }

    public String getTransportistaConductor() {
        return transportistaConductor;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }
}
