package pe.edu.usmp.aduanatrack.embarque.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import pe.edu.usmp.aduanatrack.embarque.entity.Contenedor;
import pe.edu.usmp.aduanatrack.embarque.entity.Embarque;
import pe.edu.usmp.aduanatrack.embarque.entity.TipoBl;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService.Plazo;

/**
 * Embarque con su plazo de almacenaje. {@code etaModificada} y {@code etaAnterior} avisan
 * que la ETA cambió en este guardado (HU-05, regla de negocio 3).
 */
public record EmbarqueResponse(
        Long id,
        Long despachoId,
        String numeroDespacho,
        String numeroBl,
        TipoBl tipoBl,
        String naviera,
        String buque,
        String numeroViaje,
        String puertoEmbarque,
        String puertoDescarga,
        LocalDate fechaEmbarque,
        LocalDate fechaEta,
        LocalDate fechaLimiteAlmacenaje,
        Long diasRestantes,
        boolean alertaAlmacenaje,
        boolean etaModificada,
        LocalDate etaAnterior,
        List<ContenedorResponse> contenedores) {

    public record ContenedorResponse(Long id, String numero, String tipo, String precinto, BigDecimal pesoBrutoKg) {

        static ContenedorResponse de(Contenedor c) {
            return new ContenedorResponse(c.getId(), c.getNumero(), c.getTipo(), c.getPrecinto(), c.getPesoBrutoKg());
        }
    }

    public static EmbarqueResponse de(Embarque e, Plazo plazo, LocalDate etaAnterior) {
        return new EmbarqueResponse(
                e.getId(),
                e.getDespacho().getId(),
                e.getDespacho().getNumero(),
                e.getNumeroBl(),
                e.getTipoBl(),
                e.getNaviera(),
                e.getBuque(),
                e.getNumeroViaje(),
                e.getPuertoEmbarque(),
                e.getPuertoDescarga(),
                e.getFechaEmbarque(),
                e.getFechaEta(),
                e.getFechaLimiteAlmacenaje(),
                plazo.diasRestantes(),
                plazo.alerta(),
                etaAnterior != null,
                etaAnterior,
                e.getContenedores().stream().map(ContenedorResponse::de).toList());
    }
}
