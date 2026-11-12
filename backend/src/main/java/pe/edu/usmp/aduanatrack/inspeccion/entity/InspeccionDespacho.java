package pe.edu.usmp.aduanatrack.inspeccion.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;

/** Inspección aduanera registrada para un despacho (HU-12 · T-12.1, contrato C7). */
@Entity
@Table(name = "inspeccion_despacho")
public class InspeccionDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoInspeccion resultado;

    private String observaciones;

    @Column(name = "acta_nombre")
    private String actaNombre;

    @Column(name = "acta_ruta")
    private String actaRuta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrada_por", nullable = false, updatable = false)
    private Usuario registradaPor;

    @Column(name = "registrada_en", nullable = false, updatable = false)
    private LocalDateTime registradaEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "inspeccion_id", nullable = false)
    @OrderBy("orden asc")
    private List<ItemInspeccion> items = new ArrayList<>();

    protected InspeccionDespacho() {
    }

    public InspeccionDespacho(Long despachoId, ResultadoInspeccion resultado, String observaciones,
            Usuario registradaPor, LocalDateTime registradaEn, List<ItemInspeccion> items) {
        this.despachoId = despachoId;
        this.resultado = resultado;
        this.observaciones = observaciones;
        this.registradaPor = registradaPor;
        this.registradaEn = registradaEn;
        this.items.addAll(items);
    }

    /** Acta en PDF (opcional): nombre original y ruta relativa al directorio de documentos. */
    public void adjuntarActa(String nombre, String ruta) {
        this.actaNombre = nombre;
        this.actaRuta = ruta;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public ResultadoInspeccion getResultado() {
        return resultado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public String getActaNombre() {
        return actaNombre;
    }

    public String getActaRuta() {
        return actaRuta;
    }

    public Usuario getRegistradaPor() {
        return registradaPor;
    }

    public LocalDateTime getRegistradaEn() {
        return registradaEn;
    }

    public List<ItemInspeccion> getItems() {
        return items;
    }
}
