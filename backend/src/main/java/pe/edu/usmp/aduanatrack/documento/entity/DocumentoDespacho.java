package pe.edu.usmp.aduanatrack.documento.entity;

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

/**
 * Una versión de un documento del despacho (HU-06). Cada carga crea una fila nueva; al
 * reemplazar, la anterior solo deja de estar vigente (CA3).
 */
@Entity
@Table(name = "documento_despacho")
public class DocumentoDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "despacho_id", nullable = false, updatable = false)
    private Long despachoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false, updatable = false)
    private TipoDocumento tipo;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false)
    private boolean vigente;

    @Column(name = "nombre_archivo", nullable = false, updatable = false)
    private String nombreArchivo;

    /** Relativa al directorio de documentos: {@code {despachoId}/{uuid}.pdf}. */
    @Column(name = "ruta_archivo", nullable = false, updatable = false)
    private String rutaArchivo;

    @Column(name = "tamano_bytes", nullable = false, updatable = false)
    private long tamanoBytes;

    @Column(nullable = false, length = 64, updatable = false)
    private String sha256;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "cargado_en", nullable = false, updatable = false)
    private LocalDateTime cargadoEn;

    protected DocumentoDespacho() {
    }

    /** Nueva versión vigente del documento. */
    public DocumentoDespacho(Long despachoId, TipoDocumento tipo, int version, String nombreArchivo,
            String rutaArchivo, long tamanoBytes, String sha256, Usuario usuario, LocalDateTime cargadoEn) {
        this.despachoId = despachoId;
        this.tipo = tipo;
        this.version = version;
        this.vigente = true;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.tamanoBytes = tamanoBytes;
        this.sha256 = sha256;
        this.usuario = usuario;
        this.cargadoEn = cargadoEn;
    }

    /** La versión pasa al historial: se conserva el archivo y deja de estar vigente. */
    public void reemplazar() {
        this.vigente = false;
    }

    public Long getId() {
        return id;
    }

    public Long getDespachoId() {
        return despachoId;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public int getVersion() {
        return version;
    }

    public boolean isVigente() {
        return vigente;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getCargadoEn() {
        return cargadoEn;
    }
}
