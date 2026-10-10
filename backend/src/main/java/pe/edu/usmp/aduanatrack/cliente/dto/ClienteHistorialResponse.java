package pe.edu.usmp.aduanatrack.cliente.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;

public record ClienteHistorialResponse(String campo, String valorAnterior, String valorNuevo, String usuario,
        LocalDateTime fecha) {

    public static ClienteHistorialResponse de(ClienteHistorial h) {
        return new ClienteHistorialResponse(h.getCampo(), h.getValorAnterior(), h.getValorNuevo(),
                h.getUsuario().getNombre(), h.getFecha());
    }
}
