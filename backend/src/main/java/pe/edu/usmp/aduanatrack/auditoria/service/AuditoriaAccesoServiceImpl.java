package pe.edu.usmp.aduanatrack.auditoria.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auditoria.AuditoriaAccesoService;
import pe.edu.usmp.aduanatrack.auditoria.ResultadoAcceso;
import pe.edu.usmp.aduanatrack.auditoria.entity.AuditoriaAcceso;
import pe.edu.usmp.aduanatrack.auditoria.repository.AuditoriaAccesoRepository;

/** Guarda cada intento de acceso en {@code auditoria_acceso} (HU-01 · T-01.2). */
@Service
public class AuditoriaAccesoServiceImpl implements AuditoriaAccesoService {

    private final AuditoriaAccesoRepository repositorio;
    private final Clock clock;

    public AuditoriaAccesoServiceImpl(AuditoriaAccesoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    /**
     * Transacción propia: el intento queda guardado aunque el login termine en 401 o 423.
     * Los textos que envía el cliente se recortan al tamaño de la columna para que un
     * User-Agent muy largo no impida registrar el intento.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuarioIngresado, Long usuarioId, String ip, String userAgent,
            ResultadoAcceso resultado) {
        repositorio.save(new AuditoriaAcceso(
                recortar(usuarioIngresado, AuditoriaAcceso.MAX_USUARIO_INGRESADO),
                usuarioId,
                recortar(ip, AuditoriaAcceso.MAX_IP),
                recortar(userAgent, AuditoriaAcceso.MAX_USER_AGENT),
                resultado,
                LocalDateTime.now(clock)));
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
