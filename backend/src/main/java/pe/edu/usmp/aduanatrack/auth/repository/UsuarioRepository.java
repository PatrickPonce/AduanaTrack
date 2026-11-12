package pe.edu.usmp.aduanatrack.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.usmp.aduanatrack.auth.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** El login acepta el nombre de usuario o el correo corporativo. */
    @Query("select u from Usuario u where lower(u.usuario) = lower(:valor) or lower(u.correo) = lower(:valor)")
    Optional<Usuario> buscarPorUsuarioOCorreo(@Param("valor") String valor);

    /** HU-16: empresa asignada al usuario; vacío si no tiene. Se consulta en cada petición del CLIENTE. */
    @Query("select u.clienteId from Usuario u where u.id = :id")
    Optional<Long> clienteIdDe(@Param("id") Long id);

    /** HU-16: usuarios con rol CLIENTE para la pantalla del administrador. */
    @Query("select u from Usuario u where u.rol.codigo = 'CLIENTE' order by lower(u.usuario)")
    List<Usuario> listarClientes();

    /** HU-16: usuarios CLIENTE activos de una empresa (destinatarios de los avisos de la HU-14). */
    @Query("select u from Usuario u where u.rol.codigo = 'CLIENTE' and u.activo = true and u.clienteId = :clienteId")
    List<Usuario> clientesActivosDe(@Param("clienteId") Long clienteId);
}
