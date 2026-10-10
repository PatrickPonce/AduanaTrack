package pe.edu.usmp.aduanatrack.cliente.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteExisteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteHistorialResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteRequest;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResponse;
import pe.edu.usmp.aduanatrack.cliente.dto.ClienteResumen;
import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.entity.ClienteHistorial;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteHistorialRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.common.PaginaResponse;

/** Registro y mantenimiento del cliente importador (HU-04 · T-04.1 y T-04.3). */
@Service
public class ClienteService {

    static final int TAMANO_MAXIMO_PAGINA = 50;

    /** Campos auditados en cliente_historial: nombre del campo, lectura y escritura. */
    private static final List<CampoAuditado> CAMPOS = List.of(
            new CampoAuditado("ruc", Cliente::getRuc, Cliente::setRuc, ClienteRequest::ruc),
            new CampoAuditado("razonSocial", Cliente::getRazonSocial, Cliente::setRazonSocial,
                    ClienteRequest::razonSocial),
            new CampoAuditado("direccion", Cliente::getDireccion, Cliente::setDireccion, ClienteRequest::direccion),
            new CampoAuditado("correo", Cliente::getCorreo, Cliente::setCorreo, ClienteRequest::correo),
            new CampoAuditado("telefono", Cliente::getTelefono, Cliente::setTelefono, ClienteRequest::telefono),
            new CampoAuditado("contacto", Cliente::getContacto, Cliente::setContacto, ClienteRequest::contacto));

    private final ClienteRepository clienteRepository;
    private final ClienteHistorialRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final CartaPoderEstadoService cartaPoderEstado;
    private final Clock clock;

    public ClienteService(ClienteRepository clienteRepository, ClienteHistorialRepository historialRepository,
            UsuarioRepository usuarioRepository, CartaPoderEstadoService cartaPoderEstado, Clock clock) {
        this.clienteRepository = clienteRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.cartaPoderEstado = cartaPoderEstado;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResumen> buscar(String q, int pagina, int tamano) {
        String filtro = q == null ? "" : q.trim();
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA),
                Sort.by("razonSocial"));
        Page<Cliente> resultado = clienteRepository.buscar(filtro, pageable);
        Map<Long, EstadoCartaPoder> estados = cartaPoderEstado.estados(
                resultado.getContent().stream().map(Cliente::getId).toList());
        return PaginaResponse.de(resultado, c -> ClienteResumen.de(c, estados.get(c.getId())));
    }

    @Transactional(readOnly = true)
    public ClienteExisteResponse existe(String ruc) {
        return clienteRepository.findByRuc(ruc == null ? "" : ruc.trim())
                .map(c -> new ClienteExisteResponse(true, c.getId(), c.getRazonSocial()))
                .orElse(new ClienteExisteResponse(false, null, null));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        Cliente cliente = buscarCliente(id);
        return ClienteResponse.de(cliente, cartaPoderEstado.estado(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest solicitud) {
        String ruc = solicitud.ruc().trim();
        clienteRepository.findByRuc(ruc).ifPresent(ClienteService::lanzarDuplicado);

        Cliente cliente = new Cliente(ruc, LocalDateTime.now(clock));
        CAMPOS.forEach(campo -> campo.escribir(cliente, solicitud));
        Cliente guardado = guardar(cliente);
        return ClienteResponse.de(guardado, cartaPoderEstado.estado(guardado.getId()));
    }

    /** Actualiza el cliente y deja una fila de historial por cada campo modificado, en la misma transacción. */
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest solicitud, Long usuarioId) {
        Cliente cliente = buscarCliente(id);
        String ruc = solicitud.ruc().trim();
        if (!ruc.equals(cliente.getRuc())) {
            clienteRepository.findByRuc(ruc).ifPresent(ClienteService::lanzarDuplicado);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<ClienteHistorial> cambios = new ArrayList<>();
        for (CampoAuditado campo : CAMPOS) {
            String anterior = campo.leer(cliente);
            String nuevo = campo.nuevoValor(solicitud);
            if (!Objects.equals(anterior, nuevo)) {
                cambios.add(new ClienteHistorial(id, campo.nombre(), anterior, nuevo,
                        usuarioRepository.getReferenceById(usuarioId), ahora));
                campo.escribir(cliente, solicitud);
            }
        }

        if (!cambios.isEmpty()) {
            cliente.setActualizadoEn(ahora);
            guardar(cliente);
            historialRepository.saveAll(cambios);
        }
        return ClienteResponse.de(cliente, cartaPoderEstado.estado(id));
    }

    @Transactional(readOnly = true)
    public List<ClienteHistorialResponse> historial(Long id) {
        buscarCliente(id);
        return historialRepository.historial(id).stream().map(ClienteHistorialResponse::de).toList();
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el cliente solicitado."));
    }

    /**
     * Respaldo ante dos registros simultáneos del mismo RUC: la restricción UNIQUE de la BD.
     * Tras el fallo la transacción queda abortada, así que no se consulta el cliente existente.
     */
    private Cliente guardar(Cliente cliente) {
        try {
            return clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.CLIENTE_DUPLICADO,
                    "Este RUC ya está registrado.");
        }
    }

    private static void lanzarDuplicado(Cliente existente) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("clienteId", existente.getId());
        datos.put("razonSocial", existente.getRazonSocial());
        throw new ApiException(HttpStatus.CONFLICT, CodigoError.CLIENTE_DUPLICADO,
                "Este RUC ya está registrado: " + existente.getRazonSocial(), datos);
    }

    /** Normaliza textos opcionales: sin espacios sobrantes y vacío como nulo. */
    static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private record CampoAuditado(String nombre, Function<Cliente, String> getter,
            BiConsumer<Cliente, String> setter, Function<ClienteRequest, String> origen) {

        String leer(Cliente cliente) {
            return getter.apply(cliente);
        }

        String nuevoValor(ClienteRequest solicitud) {
            return limpiar(origen.apply(solicitud));
        }

        void escribir(Cliente cliente, ClienteRequest solicitud) {
            setter.accept(cliente, nuevoValor(solicitud));
        }
    }
}
