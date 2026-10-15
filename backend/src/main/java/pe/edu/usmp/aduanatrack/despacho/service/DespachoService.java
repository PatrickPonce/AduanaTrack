package pe.edu.usmp.aduanatrack.despacho.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.catalogo.repository.AduanaRepository;
import pe.edu.usmp.aduanatrack.catalogo.repository.RegimenAduaneroRepository;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderService;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoRequest;
import pe.edu.usmp.aduanatrack.despacho.dto.DespachoResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.repository.CorrelativoDespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

/** Registro del despacho con número DESP-AAAA-#### y validación de carta poder (HU-03 · T-03.1 y T-03.3). */
@Service
public class DespachoService {

    static final String OBLIGATORIO = "Campo obligatorio";

    private final DespachoRepository despachoRepository;
    private final CorrelativoDespachoRepository correlativo;
    private final ClienteRepository clienteRepository;
    private final RegimenAduaneroRepository regimenRepository;
    private final AduanaRepository aduanaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CartaPoderService cartaPoder;
    private final CartaPoderEstadoService cartaPoderEstado;
    private final Clock clock;

    public DespachoService(DespachoRepository despachoRepository, CorrelativoDespachoRepository correlativo,
            ClienteRepository clienteRepository, RegimenAduaneroRepository regimenRepository,
            AduanaRepository aduanaRepository, UsuarioRepository usuarioRepository, CartaPoderService cartaPoder,
            CartaPoderEstadoService cartaPoderEstado, Clock clock) {
        this.despachoRepository = despachoRepository;
        this.correlativo = correlativo;
        this.clienteRepository = clienteRepository;
        this.regimenRepository = regimenRepository;
        this.aduanaRepository = aduanaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cartaPoder = cartaPoder;
        this.cartaPoderEstado = cartaPoderEstado;
        this.clock = clock;
    }

    @Transactional
    public DespachoResponse crear(DespachoRequest solicitud, Long agenteId) {
        validarObligatorios(solicitud, !solicitud.borrador());
        Despacho despacho = new Despacho(null, usuarioRepository.getReferenceById(agenteId),
                LocalDateTime.now(clock));
        aplicar(despacho, solicitud);
        if (!solicitud.borrador()) {
            abrir(despacho);
        }
        return respuesta(despachoRepository.save(despacho));
    }

    /**
     * Completa un borrador y, con {@code borrador=false}, lo confirma. Un despacho ya abierto
     * no vuelve a borrador: se valida completo y conserva su número.
     */
    @Transactional
    public DespachoResponse actualizar(Long id, DespachoRequest solicitud) {
        Despacho despacho = buscar(id);
        boolean eraBorrador = despacho.esBorrador();
        boolean completo = !solicitud.borrador() || !eraBorrador;
        validarObligatorios(solicitud, completo);

        Long clienteAnterior = despacho.getCliente().getId();
        aplicar(despacho, solicitud);
        if (eraBorrador && !solicitud.borrador()) {
            abrir(despacho);
        } else if (!eraBorrador && !clienteAnterior.equals(despacho.getCliente().getId())) {
            cartaPoder.exigirVigente(despacho.getCliente().getId(), LocalDate.now(clock));
        }
        return respuesta(despacho);
    }

    @Transactional(readOnly = true)
    public DespachoResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    /**
     * Valida la carta poder y recién entonces consume el correlativo: si el cliente no tiene
     * carta vigente no se guarda nada ni se gasta un número (T-03.3).
     */
    private void abrir(Despacho despacho) {
        LocalDate hoy = LocalDate.now(clock);
        cartaPoder.exigirVigente(despacho.getCliente().getId(), hoy);
        int anio = hoy.getYear();
        despacho.abrir(formatearNumero(anio, correlativo.siguiente(anio)));
    }

    static String formatearNumero(int anio, int correlativo) {
        return String.format("DESP-%d-%04d", anio, correlativo);
    }

    /** CA3: devuelve todos los obligatorios vacíos a la vez para que el frontend los resalte. */
    private static void validarObligatorios(DespachoRequest s, boolean completo) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (s.clienteId() == null) {
            campos.put("clienteId", OBLIGATORIO);
        }
        if (completo) {
            if (s.regimenId() == null) {
                campos.put("regimenId", OBLIGATORIO);
            }
            if (s.aduanaId() == null) {
                campos.put("aduanaId", OBLIGATORIO);
            }
            if (s.descripcionMercancia() == null || s.descripcionMercancia().isBlank()) {
                campos.put("descripcionMercancia", OBLIGATORIO);
            }
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }
    }

    private void aplicar(Despacho despacho, DespachoRequest s) {
        Map<String, String> campos = new LinkedHashMap<>();
        Cliente cliente = clienteRepository.findById(s.clienteId()).filter(Cliente::isActivo).orElse(null);
        if (cliente == null) {
            campos.put("clienteId", "El cliente seleccionado no existe");
        }
        var regimen = s.regimenId() == null ? null : regimenRepository.findByIdAndActivoTrue(s.regimenId()).orElse(null);
        if (s.regimenId() != null && regimen == null) {
            campos.put("regimenId", "Régimen aduanero no válido");
        }
        var aduana = s.aduanaId() == null ? null : aduanaRepository.findByIdAndActivoTrue(s.aduanaId()).orElse(null);
        if (s.aduanaId() != null && aduana == null) {
            campos.put("aduanaId", "Aduana de destino no válida");
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }
        despacho.setCliente(cliente);
        despacho.setRegimen(regimen);
        despacho.setAduana(aduana);
        despacho.setReferenciaCliente(limpiar(s.referenciaCliente()));
        despacho.setDescripcionMercancia(limpiar(s.descripcionMercancia()));
        despacho.setTipoOperacion(s.tipoOperacion());
        despacho.setModalidadDespacho(s.modalidadDespacho());
    }

    private Despacho buscar(Long id) {
        return despachoRepository.buscarDetalle(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    private DespachoResponse respuesta(Despacho despacho) {
        return DespachoResponse.de(despacho, cartaPoderEstado.estado(despacho.getCliente().getId()));
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
