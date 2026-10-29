package pe.edu.usmp.aduanatrack.despacho.entity;

import java.time.LocalDate;
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
import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;
import pe.edu.usmp.aduanatrack.catalogo.entity.RegimenAduanero;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;

/** Despacho de importación marítima (HU-03). */
@Entity
@Table(name = "despacho")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DESP-AAAA-####; nulo mientras el despacho es borrador. */
    @Column(unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regimen_id")
    private RegimenAduanero regimen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aduana_id")
    private Aduana aduana;

    @Column(name = "referencia_cliente")
    private String referenciaCliente;

    @Column(name = "descripcion_mercancia")
    private String descripcionMercancia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion")
    private TipoOperacion tipoOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_despacho")
    private ModalidadDespacho modalidadDespacho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDespacho estado;

    /** Se registra en HU-10 (Sprint 2); nulo en el Sprint 1. */
    @Column(name = "canal_control")
    private String canalControl;

    /** Se registra en HU-13 (Sprint 3); nulo en el Sprint 1. */
    @Column(name = "fecha_levante")
    private LocalDate fechaLevante;

    /** HU-09: N° de DAM numerada en el sistema externo; nulo hasta DAM_NUMERADA. */
    @Column(name = "numero_dam", unique = true)
    private String numeroDam;

    @Column(name = "fecha_numeracion")
    private LocalDate fechaNumeracion;

    /** Cargo de numeración en PDF (opcional): nombre original y ruta relativa al directorio de documentos. */
    @Column(name = "cargo_nombre")
    private String cargoNombre;

    @Column(name = "cargo_ruta")
    private String cargoRuta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private Usuario agente;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Despacho() {
    }

    public Despacho(Cliente cliente, Usuario agente, LocalDateTime creadoEn) {
        this.cliente = cliente;
        this.agente = agente;
        this.creadoEn = creadoEn;
        this.estado = EstadoDespacho.BORRADOR;
    }

    /** Confirma el despacho: recibe su número y pasa a APERTURA (HU-03, CA1 y CA4). */
    public void abrir(String numero) {
        this.numero = numero;
        this.estado = EstadoDespacho.APERTURA;
    }

    /**
     * Avanza el despacho en los Sprints 2 y 3. Quien lo llama valida antes el estado anterior
     * y registra el hito con {@code HitoDespachoService} en la misma transacción.
     */
    public void cambiarEstado(EstadoDespacho nuevo) {
        this.estado = nuevo;
    }

    /** Guarda el canal que informó SUNAT y pasa a CANAL_ASIGNADO (HU-10 · T-10.2). */
    public void asignarCanal(CanalControl canal) {
        this.canalControl = canal.name();
        this.estado = EstadoDespacho.CANAL_ASIGNADO;
    }

    /** HU-09: guarda la numeración y pasa a DAM_NUMERADA; el cargo es opcional. */
    public void registrarDam(String numeroDam, LocalDate fechaNumeracion, String cargoNombre, String cargoRuta) {
        this.numeroDam = numeroDam;
        this.fechaNumeracion = fechaNumeracion;
        this.cargoNombre = cargoNombre;
        this.cargoRuta = cargoRuta;
        this.estado = EstadoDespacho.DAM_NUMERADA;
    }

    /**
     * HU-09: corrige el número y la fecha de numeración sin cambiar el estado. El cargo solo se
     * reemplaza si llega uno nuevo ({@code cargoNombre} no nulo).
     */
    public void rectificarDam(String numeroDam, LocalDate fechaNumeracion, String cargoNombre, String cargoRuta) {
        this.numeroDam = numeroDam;
        this.fechaNumeracion = fechaNumeracion;
        if (cargoNombre != null) {
            this.cargoNombre = cargoNombre;
            this.cargoRuta = cargoRuta;
        }
    }

    public boolean esBorrador() {
        return estado == EstadoDespacho.BORRADOR;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public RegimenAduanero getRegimen() {
        return regimen;
    }

    public void setRegimen(RegimenAduanero regimen) {
        this.regimen = regimen;
    }

    public Aduana getAduana() {
        return aduana;
    }

    public void setAduana(Aduana aduana) {
        this.aduana = aduana;
    }

    public String getReferenciaCliente() {
        return referenciaCliente;
    }

    public void setReferenciaCliente(String referenciaCliente) {
        this.referenciaCliente = referenciaCliente;
    }

    public String getDescripcionMercancia() {
        return descripcionMercancia;
    }

    public void setDescripcionMercancia(String descripcionMercancia) {
        this.descripcionMercancia = descripcionMercancia;
    }

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(TipoOperacion tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public ModalidadDespacho getModalidadDespacho() {
        return modalidadDespacho;
    }

    public void setModalidadDespacho(ModalidadDespacho modalidadDespacho) {
        this.modalidadDespacho = modalidadDespacho;
    }

    public EstadoDespacho getEstado() {
        return estado;
    }

    public String getCanalControl() {
        return canalControl;
    }

    public LocalDate getFechaLevante() {
        return fechaLevante;
    }

    public String getNumeroDam() {
        return numeroDam;
    }

    public LocalDate getFechaNumeracion() {
        return fechaNumeracion;
    }

    public String getCargoNombre() {
        return cargoNombre;
    }

    public String getCargoRuta() {
        return cargoRuta;
    }

    public Usuario getAgente() {
        return agente;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}

