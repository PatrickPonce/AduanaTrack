package pe.edu.usmp.aduanatrack.cliente.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;

public record ClienteResponse(Long id, String ruc, String razonSocial, String direccion, String correo,
        String telefono, String contacto, boolean activo, EstadoCartaPoder estadoCartaPoder,
        LocalDateTime creadoEn, LocalDateTime actualizadoEn) {

    public static ClienteResponse de(Cliente c, EstadoCartaPoder estadoCartaPoder) {
        return new ClienteResponse(c.getId(), c.getRuc(), c.getRazonSocial(), c.getDireccion(), c.getCorreo(),
                c.getTelefono(), c.getContacto(), c.isActivo(), estadoCartaPoder, c.getCreadoEn(),
                c.getActualizadoEn());
    }
}
