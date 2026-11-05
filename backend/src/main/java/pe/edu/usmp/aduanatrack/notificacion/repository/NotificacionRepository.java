package pe.edu.usmp.aduanatrack.notificacion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.notificacion.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Bandeja del usuario, de la más reciente a la más antigua (contrato C9). {@code soloNoLeidas = false}
     * devuelve todas.
     */
    @Query(value = """
            select n from Notificacion n
            left join fetch n.despacho
            where n.usuarioId = :usuarioId and (:soloNoLeidas = false or n.leida = false)
            order by n.creadaEn desc, n.id desc
            """, countQuery = """
            select count(n) from Notificacion n
            where n.usuarioId = :usuarioId and (:soloNoLeidas = false or n.leida = false)
            """)
    Page<Notificacion> bandeja(@Param("usuarioId") Long usuarioId, @Param("soloNoLeidas") boolean soloNoLeidas,
            Pageable pageable);

    /** Contador de la campana. */
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    /** Cada usuario solo puede tocar sus propias notificaciones. */
    @Query("select n from Notificacion n left join fetch n.despacho where n.id = :id and n.usuarioId = :usuarioId")
    Optional<Notificacion> buscarDelUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
