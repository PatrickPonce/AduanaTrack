package pe.edu.usmp.aduanatrack.acceso.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;

/** Intento de un usuario CLIENTE de abrir un despacho que no le corresponde (HU-16). */
@Entity
@Table(name = "acceso_denegado")
public class AccesoDenegado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Id del despacho que se pidió en la URL. */
    @Column(name = "despacho_id", nullable = false)
    private Long despachoId;

    /** Empresa dueña de ese despacho. */
    @Column(name = "cliente_id")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 30)
    private MotivoAccesoDenegado resultado;

    protected AccesoDenegado() {
    }

    public AccesoDenegado(LocalDateTime fecha, Long usuarioId, Long despachoId, Long clienteId,
            MotivoAccesoDenegado resultado) {
        this.fecha = fecha;
        this.usuarioId = usuarioId;
        this.despachoId = despachoId;
        this.clienteId = clienteId;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public MotivoAccesoDenegado getResultado() {
        return resultado;
    }
}
