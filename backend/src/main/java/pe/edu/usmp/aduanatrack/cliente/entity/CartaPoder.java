package pe.edu.usmp.aduanatrack.cliente.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Carta poder del cliente. La carga del PDF y la alerta programada son de T-04.2;
 * aquí se usa para calcular el estado de la carta y la vigencia al abrir un despacho.
 */
@Entity
@Table(name = "carta_poder")
public class CartaPoder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "alerta_generada_en")
    private LocalDateTime alertaGeneradaEn;

    protected CartaPoder() {
    }

    public CartaPoder(Long clienteId, LocalDate fechaEmision, LocalDate fechaVencimiento, String rutaArchivo,
            String nombreArchivo, long tamanoBytes, String sha256) {
        this.clienteId = clienteId;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.rutaArchivo = rutaArchivo;
        this.nombreArchivo = nombreArchivo;
        this.tamanoBytes = tamanoBytes;
        this.sha256 = sha256;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public LocalDateTime getAlertaGeneradaEn() {
        return alertaGeneradaEn;
    }

    public void setAlertaGeneradaEn(LocalDateTime alertaGeneradaEn) {
        this.alertaGeneradaEn = alertaGeneradaEn;
    }
}
