package pe.edu.usmp.aduanatrack.acceso.dto;

/** Empresa que se puede asignar a un usuario cliente (activa y con RUC válido). */
public record EmpresaAsignableResponse(Long id, String ruc, String razonSocial) {
}
