package pe.edu.usmp.aduanatrack.acceso.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.entity.AccesoDenegado;
import pe.edu.usmp.aduanatrack.acceso.repository.AccesoDenegadoRepository;

/** Guarda los intentos bloqueados en {@code acceso_denegado} con transacción propia. */
@Service
public class AccesoDenegadoService {

    private final AccesoDenegadoRepository repositorio;
    private final Clock clock;

    public AccesoDenegadoService(AccesoDenegadoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    /** REQUIRES_NEW: el intento queda guardado aunque la petición termine en 404. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, Long despachoId, Long clienteIdDelDespacho,
            MotivoAccesoDenegado motivo) {
        repositorio.save(new AccesoDenegado(LocalDateTime.now(clock), usuarioId, despachoId,
                clienteIdDelDespacho, motivo));
    }
}
