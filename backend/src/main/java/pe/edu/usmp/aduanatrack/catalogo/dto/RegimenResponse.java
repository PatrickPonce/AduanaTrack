package pe.edu.usmp.aduanatrack.catalogo.dto;

import pe.edu.usmp.aduanatrack.catalogo.entity.RegimenAduanero;

public record RegimenResponse(Long id, String codigo, String descripcion) {

    public static RegimenResponse de(RegimenAduanero regimen) {
        return new RegimenResponse(regimen.getId(), regimen.getCodigo(), regimen.getDescripcion());
    }
}
