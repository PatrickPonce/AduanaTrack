package pe.edu.usmp.aduanatrack.cita.dto;

import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.cita.entity.CitaDespacho;
import pe.edu.usmp.aduanatrack.cita.entity.EstadoCita;
import pe.edu.usmp.aduanatrack.cita.entity.TipoCita;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/**
 * Cita registrada (contrato C6). {@code estadoDespacho} es el estado del despacho al responder y
 * {@code transportista} es nulo en las citas de inspección.
 */
public record CitaResponse(Long id, Long depositoId, String deposito, String numeroCita, TipoCita tipo,
        LocalDateTime fechaHoraConfirmada, EstadoCita estado, Transportista transportista, String registradaPor,
        LocalDateTime registradaEn, EstadoDespacho estadoDespacho) {

    public record Transportista(String ruc, String razonSocial, String placa, String conductor) {
    }

    public static CitaResponse de(CitaDespacho c, EstadoDespacho estadoDespacho) {
        Transportista transportista = c.getTransportistaRuc() == null ? null
                : new Transportista(c.getTransportistaRuc(), c.getTransportistaRazonSocial(),
                        c.getTransportistaPlaca(), c.getTransportistaConductor());
        return new CitaResponse(c.getId(), c.getDeposito().getId(), c.getDeposito().getNombre(), c.getNumeroCita(),
                c.getTipo(), c.getFechaHoraConfirmada(), c.getEstado(), transportista,
                c.getRegistradoPor().getNombre(), c.getRegistradoEn(), estadoDespacho);
    }
}
