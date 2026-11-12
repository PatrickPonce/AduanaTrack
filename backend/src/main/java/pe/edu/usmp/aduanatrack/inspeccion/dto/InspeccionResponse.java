package pe.edu.usmp.aduanatrack.inspeccion.dto;

import java.time.LocalDateTime;
import java.util.List;

import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.inspeccion.entity.InspeccionDespacho;
import pe.edu.usmp.aduanatrack.inspeccion.entity.ResultadoInspeccion;

/**
 * Inspección registrada (contrato C7). {@code estadoDespacho} y {@code levanteHabilitado} reflejan el estado
 * actual del despacho: el levante se habilita desde {@code INSPECCION_CONFORME}.
 */
public record InspeccionResponse(ResultadoInspeccion resultado, EstadoDespacho estadoDespacho,
        boolean levanteHabilitado, String actaNombre, String observaciones, List<Item> items, String registradaPor,
        LocalDateTime registradaEn) {

    public record Item(String descripcion, boolean conforme, String observacion) {
    }

    public static InspeccionResponse de(InspeccionDespacho inspeccion, EstadoDespacho estadoDespacho) {
        List<Item> items = inspeccion.getItems().stream()
                .map(i -> new Item(i.getDescripcion(), i.isConforme(), i.getObservacion())).toList();
        return new InspeccionResponse(inspeccion.getResultado(), estadoDespacho,
                estadoDespacho.alcanzo(EstadoDespacho.INSPECCION_CONFORME), inspeccion.getActaNombre(),
                inspeccion.getObservaciones(), items, inspeccion.getRegistradaPor().getNombre(),
                inspeccion.getRegistradaEn());
    }
}
