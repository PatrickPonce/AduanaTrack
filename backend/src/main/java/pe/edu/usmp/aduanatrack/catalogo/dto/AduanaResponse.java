package pe.edu.usmp.aduanatrack.catalogo.dto;

import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;

public record AduanaResponse(Long id, String codigo, String nombre) {

    public static AduanaResponse de(Aduana aduana) {
        return new AduanaResponse(aduana.getId(), aduana.getCodigo(), aduana.getNombre());
    }
}
