package pe.edu.usmp.aduanatrack.embarque.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;

/** Embarque marítimo y BL del despacho (HU-05): uno por despacho, con uno o más contenedores. */
@Entity
@Table(name = "embarque")
public class Embarque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "despacho_id", nullable = false, unique = true)
    private Despacho despacho;

    @Column(name = "numero_bl", nullable = false)
    private String numeroBl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_bl", nullable = false)
    private TipoBl tipoBl;

    @Column(nullable = false)
    private String naviera;

    private String buque;

    @Column(name = "numero_viaje")
    private String numeroViaje;

    @Column(name = "puerto_embarque", nullable = false)
    private String puertoEmbarque;

    @Column(name = "puerto_descarga")
    private String puertoDescarga;

    @Column(name = "fecha_embarque")
    private LocalDate fechaEmbarque;

    @Column(name = "fecha_eta", nullable = false)
    private LocalDate fechaEta;

    @Column(name = "fecha_limite_almacenaje", nullable = false)
    private LocalDate fechaLimiteAlmacenaje;

    @OneToMany(mappedBy = "embarque", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Contenedor> contenedores = new ArrayList<>();

    protected Embarque() {
    }

    public Embarque(Despacho despacho) {
        this.despacho = despacho;
    }

    /**
     * Sincroniza la lista de contenedores por número: conserva y actualiza los que siguen,
     * elimina los que ya no vienen y agrega los nuevos. Así no se borra y reinserta el mismo
     * número en un guardado (chocaría con la restricción UNIQUE por embarque).
     */
    public void sincronizarContenedores(List<DatosContenedor> datos) {
        Map<String, DatosContenedor> porNumero = datos.stream()
                .collect(Collectors.toMap(DatosContenedor::numero, Function.identity(), (a, b) -> a,
                        java.util.LinkedHashMap::new));
        contenedores.removeIf(c -> !porNumero.containsKey(c.getNumero()));
        Set<String> existentes = contenedores.stream().map(Contenedor::getNumero).collect(Collectors.toSet());
        for (Contenedor c : contenedores) {
            DatosContenedor d = porNumero.get(c.getNumero());
            c.setTipo(d.tipo());
            c.setPrecinto(d.precinto());
            c.setPesoBrutoKg(d.pesoBrutoKg());
        }
        porNumero.values().stream().filter(d -> !existentes.contains(d.numero())).forEach(d -> {
            Contenedor nuevo = new Contenedor(this, d.numero());
            nuevo.setTipo(d.tipo());
            nuevo.setPrecinto(d.precinto());
            nuevo.setPesoBrutoKg(d.pesoBrutoKg());
            contenedores.add(nuevo);
        });
    }

    public record DatosContenedor(String numero, String tipo, String precinto, BigDecimal pesoBrutoKg) {
    }

    public Long getId() {
        return id;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public String getNumeroBl() {
        return numeroBl;
    }

    public void setNumeroBl(String numeroBl) {
        this.numeroBl = numeroBl;
    }

    public TipoBl getTipoBl() {
        return tipoBl;
    }

    public void setTipoBl(TipoBl tipoBl) {
        this.tipoBl = tipoBl;
    }

    public String getNaviera() {
        return naviera;
    }

    public void setNaviera(String naviera) {
        this.naviera = naviera;
    }

    public String getBuque() {
        return buque;
    }

    public void setBuque(String buque) {
        this.buque = buque;
    }

    public String getNumeroViaje() {
        return numeroViaje;
    }

    public void setNumeroViaje(String numeroViaje) {
        this.numeroViaje = numeroViaje;
    }

    public String getPuertoEmbarque() {
        return puertoEmbarque;
    }

    public void setPuertoEmbarque(String puertoEmbarque) {
        this.puertoEmbarque = puertoEmbarque;
    }

    public String getPuertoDescarga() {
        return puertoDescarga;
    }

    public void setPuertoDescarga(String puertoDescarga) {
        this.puertoDescarga = puertoDescarga;
    }

    public LocalDate getFechaEmbarque() {
        return fechaEmbarque;
    }

    public void setFechaEmbarque(LocalDate fechaEmbarque) {
        this.fechaEmbarque = fechaEmbarque;
    }

    public LocalDate getFechaEta() {
        return fechaEta;
    }

    public void setFechaEta(LocalDate fechaEta) {
        this.fechaEta = fechaEta;
    }

    public LocalDate getFechaLimiteAlmacenaje() {
        return fechaLimiteAlmacenaje;
    }

    public void setFechaLimiteAlmacenaje(LocalDate fechaLimiteAlmacenaje) {
        this.fechaLimiteAlmacenaje = fechaLimiteAlmacenaje;
    }

    public List<Contenedor> getContenedores() {
        return Collections.unmodifiableList(contenedores);
    }
}
