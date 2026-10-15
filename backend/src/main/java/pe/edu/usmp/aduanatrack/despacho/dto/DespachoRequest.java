package pe.edu.usmp.aduanatrack.despacho.dto;

import jakarta.validation.constraints.Size;
import pe.edu.usmp.aduanatrack.despacho.entity.ModalidadDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoOperacion;

/**
 * Datos generales del despacho. Con {@code borrador=true} solo se exige el cliente
 * ("Guardar como borrador"); con {@code false} se exigen todos los obligatorios y se
 * genera el número ("Guardar y continuar"). Los obligatorios se validan en el servicio
 * porque dependen de {@code borrador}. Tipo de operación y modalidad son opcionales
 * (campos del prototipo de HU-03).
 */
public record DespachoRequest(
        Long clienteId,
        Long regimenId,
        Long aduanaId,
        @Size(max = 100, message = "Máximo 100 caracteres") String referenciaCliente,
        @Size(max = 1000, message = "Máximo 1000 caracteres") String descripcionMercancia,
        boolean borrador,
        TipoOperacion tipoOperacion,
        ModalidadDespacho modalidadDespacho) {
}
