package pe.edu.usmp.aduanatrack.notificacion.dto;

import java.time.LocalDate;

import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;

/** Despacho sin levante cuyo plazo de almacenaje está por vencer (o ya venció) y su fecha límite. */
public record AlmacenajePorVencer(Despacho despacho, LocalDate fechaLimite) {
}
