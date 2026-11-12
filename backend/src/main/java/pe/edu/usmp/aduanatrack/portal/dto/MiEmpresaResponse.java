package pe.edu.usmp.aduanatrack.portal.dto;

/** Empresa del usuario cliente para el banner y el pie del portal; todo nulo si aún no tiene empresa. */
public record MiEmpresaResponse(Long clienteId, String ruc, String razonSocial) {
}
