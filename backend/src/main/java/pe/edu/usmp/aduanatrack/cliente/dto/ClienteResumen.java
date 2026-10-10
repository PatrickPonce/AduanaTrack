package pe.edu.usmp.aduanatrack.cliente.dto;

import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;

/** Fila del listado de clientes y del autocompletar "Cliente / Importador". */
public record ClienteResumen(Long id, String ruc, String razonSocial, EstadoCartaPoder estadoCartaPoder) {

    public static ClienteResumen de(Cliente c, EstadoCartaPoder estadoCartaPoder) {
        return new ClienteResumen(c.getId(), c.getRuc(), c.getRazonSocial(), estadoCartaPoder);
    }
}
