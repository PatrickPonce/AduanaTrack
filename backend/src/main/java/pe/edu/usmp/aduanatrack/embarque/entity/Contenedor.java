package pe.edu.usmp.aduanatrack.embarque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Contenedor del embarque; su número es único dentro del embarque. */
@Entity
@Table(name = "contenedor")
public class Contenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "embarque_id", nullable = false)
    private Embarque embarque;

    @Column(nullable = false, length = 11)
    private String numero;

    private String tipo;

    private String precinto;

    protected Contenedor() {
    }

    Contenedor(Embarque embarque, String numero) {
        this.embarque = embarque;
        this.numero = numero;
    }

    public Long getId() {
        return id;
    }

    public Embarque getEmbarque() {
        return embarque;
    }

    public String getNumero() {
        return numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getPrecinto() {
        return precinto;
    }

    public void setPrecinto(String precinto) {
        this.precinto = precinto;
    }
}
