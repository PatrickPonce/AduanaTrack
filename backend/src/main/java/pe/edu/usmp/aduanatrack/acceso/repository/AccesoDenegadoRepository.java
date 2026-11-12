package pe.edu.usmp.aduanatrack.acceso.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse;
import pe.edu.usmp.aduanatrack.acceso.entity.AccesoDenegado;

public interface AccesoDenegadoRepository extends JpaRepository<AccesoDenegado, Long> {

    /** Del intento más reciente al más antiguo, con el usuario, el N° de despacho y la empresa dueña. */
    @Query(value = """
            select new pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse(
                a.id, a.fecha, u.usuario, a.despachoId, d.numero, c.id, c.razonSocial, a.resultado)
            from AccesoDenegado a
            join Usuario u on u.id = a.usuarioId
            left join Despacho d on d.id = a.despachoId
            left join Cliente c on c.id = a.clienteId
            order by a.fecha desc, a.id desc
            """,
            countQuery = "select count(a) from AccesoDenegado a")
    Page<AccesoDenegadoResponse> listar(Pageable pageable);
}
