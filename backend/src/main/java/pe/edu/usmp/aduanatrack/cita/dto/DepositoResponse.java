package pe.edu.usmp.aduanatrack.cita.dto;

import pe.edu.usmp.aduanatrack.cita.entity.Deposito;
import pe.edu.usmp.aduanatrack.cita.entity.TipoDeposito;

/** Fila de {@code GET /catalogos/depositos} (contrato C6). */
public record DepositoResponse(Long id, String codigo, String nombre, TipoDeposito tipo) {

    public static DepositoResponse de(Deposito deposito) {
        return new DepositoResponse(deposito.getId(), deposito.getCodigo(), deposito.getNombre(), deposito.getTipo());
    }
}
