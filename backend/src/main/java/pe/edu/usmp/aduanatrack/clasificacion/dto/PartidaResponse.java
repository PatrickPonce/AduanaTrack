package pe.edu.usmp.aduanatrack.clasificacion.dto;

import java.math.BigDecimal;

import pe.edu.usmp.aduanatrack.clasificacion.entity.PartidaReferencial;

/** Resultado del buscador de partida arancelaria (referencial), contrato C2. */
public record PartidaResponse(Long id, String codigo, String codigoFormateado, String descripcion,
        BigDecimal tasaAdValorem) {

    public static PartidaResponse de(PartidaReferencial partida) {
        return new PartidaResponse(partida.getId(), partida.getCodigo(), partida.getCodigoFormateado(),
                partida.getDescripcion(), partida.getTasaAdValorem());
    }
}
