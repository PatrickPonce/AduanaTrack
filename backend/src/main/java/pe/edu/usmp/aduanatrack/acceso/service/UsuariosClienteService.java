package pe.edu.usmp.aduanatrack.acceso.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.acceso.dto.AccesoDenegadoResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.EmpresaAsignableResponse;
import pe.edu.usmp.aduanatrack.acceso.dto.UsuarioClienteResponse;
import pe.edu.usmp.aduanatrack.acceso.repository.AccesoDenegadoRepository;
import pe.edu.usmp.aduanatrack.auth.entity.Usuario;
import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.cliente.validacion.RucValidator;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;

/** Pantalla «Usuarios cliente» del administrador: empresa de cada usuario e intentos denegados (HU-16). */
@Service
public class UsuariosClienteService {

    static final int TAMANO_MAXIMO_PAGINA = 50;
    /** Campo de {@code cliente_historial} donde queda la asignación de un usuario a la empresa. */
    static final String CAMPO_HISTORIAL = "usuarioAsignado";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteHistorialRepository historialRepository;
    private final AccesoDenegadoRepository denegadoRepository;
    private final Clock clock;

    public UsuariosClienteService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            ClienteHistorialRepository historialRepository, AccesoDenegadoRepository denegadoRepository,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.historialRepository = historialRepository;
        this.denegadoRepository = denegadoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UsuarioClienteResponse> listar() {
        List<Usuario> usuarios = usuarioRepository.listarClientes();
        Map<Long, Cliente> empresas = clienteRepository
                .findAllById(usuarios.stream().map(Usuario::getClienteId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Cliente::getId, Function.identity()));
        return usuarios.stream().map(u -> respuesta(u, empresas.get(u.getClienteId()))).toList();
    }

    /** Empresas activas con RUC válido (HU-04), por razón social. */
    @Transactional(readOnly = true)
    public List<EmpresaAsignableResponse> empresasAsignables() {
        return clienteRepository.findAll().stream()
                .filter(c -> c.isActivo() && RucValidator.esValido(c.getRuc()))
                .sorted(Comparator.comparing(c -> c.getRazonSocial().toLowerCase(Locale.ROOT)))
                .map(c -> new EmpresaAsignableResponse(c.getId(), c.getRuc(), c.getRazonSocial()))
                .toList();
    }

    /**
     * Asigna o cambia la empresa del usuario cliente. Solo empresas activas con RUC válido. Deja una
     * fila en {@code cliente_historial} por cada empresa afectada (la que pierde y la que gana al
     * usuario) con el administrador que lo hizo. Repetir la misma empresa no cambia nada.
     */
    @Transactional
    public UsuarioClienteResponse asignar(Long usuarioId, Long clienteId, Long adminId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario solicitado."));
        if (!"CLIENTE".equals(usuario.getRol().getCodigo())) {
            throw ApiException.validacion(Map.of("usuarioId", "El usuario no tiene el rol Cliente"));
        }
        Cliente empresa = clienteRepository.findById(clienteId).filter(Cliente::isActivo).orElse(null);
        if (empresa == null) {
            throw ApiException.validacion(Map.of("clienteId", "La empresa seleccionada no existe"));
        }
        if (!RucValidator.esValido(empresa.getRuc())) {
            throw ApiException.validacion(Map.of("clienteId", "Solo se pueden asignar empresas con RUC válido"));
        }
        Long anterior = usuario.getClienteId();
        if (clienteId.equals(anterior)) {
            return respuesta(usuario, empresa);
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario admin = usuarioRepository.getReferenceById(adminId);
        if (anterior != null) {
            historialRepository.save(new ClienteHistorial(anterior, CAMPO_HISTORIAL, usuario.getUsuario(), null,
                    admin, ahora));
        }
        historialRepository.save(new ClienteHistorial(clienteId, CAMPO_HISTORIAL, null, usuario.getUsuario(),
                admin, ahora));
        usuario.asignarCliente(clienteId, ahora);
        return respuesta(usuario, empresa);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AccesoDenegadoResponse> accesosDenegados(int pagina, int tamano) {
        var resultado = denegadoRepository.listar(
                PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA)));
        return PaginaResponse.de(resultado, Function.identity());
    }

    private static UsuarioClienteResponse respuesta(Usuario u, Cliente empresa) {
        boolean asignado = empresa != null;
        return new UsuarioClienteResponse(u.getId(), u.getUsuario(), u.getCorreo(), u.getNombre(), u.isActivo(),
                asignado ? empresa.getId() : null, asignado ? empresa.getRuc() : null,
                asignado ? empresa.getRazonSocial() : null, asignado ? u.getClienteAsignadoEn() : null,
                asignado ? "ASIGNADO" : "SIN_ASIGNAR");
    }
}
