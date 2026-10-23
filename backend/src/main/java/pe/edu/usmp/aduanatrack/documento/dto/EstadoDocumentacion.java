package pe.edu.usmp.aduanatrack.documento.dto;

/** Estado general de la documentación del despacho (HU-06, CA4). */
public enum EstadoDocumentacion {
    /** Todos los documentos obligatorios tienen una versión vigente. */
    COMPLETO,
    /** Falta al menos un documento obligatorio. */
    PENDIENTE
}
