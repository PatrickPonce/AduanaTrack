package pe.edu.usmp.aduanatrack.despacho.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.usmp.aduanatrack.catalogo.entity.Aduana;
import pe.edu.usmp.aduanatrack.catalogo.entity.RegimenAduanero;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;

/** Detalle del despacho (cabecera de /despachos/{id}, T-02.4 y T-03.5). */
public record DespachoResponse(
        Long id,
        String numero,
        EstadoDespacho estado,
        ClienteRef cliente,
        CatalogoRef regimen,
        CatalogoRef aduana,
        String referenciaCliente,
        String descripcionMercancia,
        String canalControl,
        LocalDate fechaLevante,
        String agente,
        LocalDateTime creadoEn) {

    public record ClienteRef(Long id, String ruc, String razonSocial, EstadoCartaPoder estadoCartaPoder) {
    }

    public record CatalogoRef(Long id, String codigo, String descripcion) {
    }

    public static DespachoResponse de(Despacho d, EstadoCartaPoder estadoCartaPoder) {
        Cliente c = d.getCliente();
        RegimenAduanero r = d.getRegimen();
        Aduana a = d.getAduana();
        return new DespachoResponse(
                d.getId(),
                d.getNumero(),
                d.getEstado(),
                new ClienteRef(c.getId(), c.getRuc(), c.getRazonSocial(), estadoCartaPoder),
                r == null ? null : new CatalogoRef(r.getId(), r.getCodigo(), r.getDescripcion()),
                a == null ? null : new CatalogoRef(a.getId(), a.getCodigo(), a.getNombre()),
                d.getReferenciaCliente(),
                d.getDescripcionMercancia(),
                d.getCanalControl(),
                d.getFechaLevante(),
                d.getAgente().getNombre(),
                d.getCreadoEn());
    }
}
