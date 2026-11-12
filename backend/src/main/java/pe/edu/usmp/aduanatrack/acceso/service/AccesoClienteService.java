package pe.edu.usmp.aduanatrack.acceso.service;

import org.springframework.stereotype.Service;

import pe.edu.usmp.aduanatrack.acceso.MotivoAccesoDenegado;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/**
 * Punto único del aislamiento por empresa (HU-16). Un usuario CLIENTE solo alcanza lo de la empresa
 * que tiene asignada; ADMIN y AGENTE no tienen restricción. La empresa se lee de la base en cada
 * petición, así que una reasignación del administrador rige de inmediato.
 *
 * <ul>
 *   <li>Rutas con despacho o cliente en la URL: las valida {@code AccesoClienteInterceptor}.</li>
 *   <li>Listados (dashboard, portal, clientes): llaman a {@link #filtroCliente}.</li>
 * </ul>
 */
@Service
public class AccesoClienteService {

    /** Rol que queda restringido a su empresa. */
    public static final String ROL_CLIENTE = "CLIENTE";
    /** Filtro de empresa que no coincide con ninguna fila: el CLIENTE sin empresa ve listas vacías. */
    public static final long SIN_RESULTADOS = -1L;
    /** Mismo texto para «es de otra empresa» y «no existe»: no revela que el despacho existe. */
    public static final String DESPACHO_NO_ENCONTRADO = "Despacho no encontrado";
    public static final String CLIENTE_NO_ENCONTRADO = "Cliente no encontrado";

    private final UsuarioRepository usuarioRepository;
    private final DespachoRepository despachoRepository;
    private final AccesoDenegadoService denegados;

    public AccesoClienteService(UsuarioRepository usuarioRepository, DespachoRepository despachoRepository,
            AccesoDenegadoService denegados) {
        this.usuarioRepository = usuarioRepository;
        this.despachoRepository = despachoRepository;
        this.denegados = denegados;
    }

    public static boolean esCliente(UsuarioAutenticado usuario) {
        return usuario != null && ROL_CLIENTE.equals(usuario.rol());
    }

    /** Empresa asignada al usuario, o {@code null} si no tiene. */
    public Long clienteIdDe(UsuarioAutenticado usuario) {
        return usuarioRepository.clienteIdDe(usuario.id()).orElse(null);
    }

    /**
     * Filtro de empresa para un listado. ADMIN y AGENTE conservan el filtro que pidieron
     * ({@code solicitado}, puede ser nulo); el CLIENTE siempre recibe su empresa, aunque pida otra, o
     * {@link #SIN_RESULTADOS} si no tiene.
     */
    public Long filtroCliente(UsuarioAutenticado usuario, Long solicitado) {
        if (!esCliente(usuario)) {
            return solicitado;
        }
        Long propio = clienteIdDe(usuario);
        return propio == null ? SIN_RESULTADOS : propio;
    }

    /**
     * Deja pasar al ADMIN y al AGENTE. Al CLIENTE solo si el despacho es de su empresa y no es
     * borrador; si no, registra el intento en {@code acceso_denegado} y responde 404
     * «Despacho no encontrado», igual que si el despacho no existiera.
     */
    public void exigirDespacho(UsuarioAutenticado usuario, Long despachoId) {
        if (!esCliente(usuario)) {
            return;
        }
        Long propio = clienteIdDe(usuario);
        var acceso = despachoRepository.accesoDe(despachoId)
                .orElseThrow(AccesoClienteService::despachoNoEncontrado);
        MotivoAccesoDenegado motivo = null;
        if (propio == null) {
            motivo = MotivoAccesoDenegado.SIN_EMPRESA;
        } else if (!propio.equals(acceso.getClienteId())) {
            motivo = MotivoAccesoDenegado.OTRA_EMPRESA;
        } else if (acceso.getEstado() == EstadoDespacho.BORRADOR) {
            motivo = MotivoAccesoDenegado.BORRADOR;
        }
        if (motivo != null) {
            denegados.registrar(usuario.id(), despachoId, acceso.getClienteId(), motivo);
            throw despachoNoEncontrado();
        }
    }

    /** El CLIENTE solo consulta su propia empresa; cualquier otra responde 404. */
    public void exigirCliente(UsuarioAutenticado usuario, Long clienteId) {
        if (!esCliente(usuario)) {
            return;
        }
        Long propio = clienteIdDe(usuario);
        if (propio == null || !propio.equals(clienteId)) {
            throw ApiException.noEncontrado(CLIENTE_NO_ENCONTRADO);
        }
    }

    private static ApiException despachoNoEncontrado() {
        return ApiException.noEncontrado(DESPACHO_NO_ENCONTRADO);
    }
}
