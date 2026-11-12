package pe.edu.usmp.aduanatrack.portal.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.acceso.service.AccesoClienteService;
import pe.edu.usmp.aduanatrack.cliente.entity.Cliente;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.portal.dto.MiEmpresaResponse;
import pe.edu.usmp.aduanatrack.portal.dto.PortalDespachoResponse;
import pe.edu.usmp.aduanatrack.portal.repository.PortalRepository;
import pe.edu.usmp.aduanatrack.seguridad.UsuarioAutenticado;

/** «Mis despachos» del portal del cliente (HU-16): solo los de la empresa asignada. */
@Service
public class PortalService {

    private final AccesoClienteService acceso;
    private final ClienteRepository clienteRepository;
    private final PortalRepository portalRepository;

    public PortalService(AccesoClienteService acceso, ClienteRepository clienteRepository,
            PortalRepository portalRepository) {
        this.acceso = acceso;
        this.clienteRepository = clienteRepository;
        this.portalRepository = portalRepository;
    }

    @Transactional(readOnly = true)
    public MiEmpresaResponse miEmpresa(UsuarioAutenticado usuario) {
        Long clienteId = acceso.clienteIdDe(usuario);
        Cliente empresa = clienteId == null ? null : clienteRepository.findById(clienteId).orElse(null);
        return empresa == null ? new MiEmpresaResponse(null, null, null)
                : new MiEmpresaResponse(empresa.getId(), empresa.getRuc(), empresa.getRazonSocial());
    }

    /**
     * Lista los despachos de la empresa con filtros opcionales: {@code numero} (contiene, sin distinguir
     * mayúsculas), {@code estado} y rango de ETA (ambos extremos incluidos). Sin empresa devuelve lista vacía.
     */
    @Transactional(readOnly = true)
    public List<PortalDespachoResponse> despachos(UsuarioAutenticado usuario, String numero, String estado,
            String etaDesde, String etaHasta) {
        Map<String, String> errores = new LinkedHashMap<>();
        EstadoDespacho estadoFiltro = parsearEstado(estado, errores);
        LocalDate desde = parsearFecha("etaDesde", etaDesde, errores);
        LocalDate hasta = parsearFecha("etaHasta", etaHasta, errores);
        if (errores.isEmpty() && desde != null && hasta != null && desde.isAfter(hasta)) {
            errores.put("etaHasta", "La fecha final no puede ser anterior a la inicial");
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
        Long clienteId = acceso.clienteIdDe(usuario);
        if (clienteId == null) {
            return List.of();
        }
        String buscado = numero == null ? "" : numero.trim().toUpperCase(Locale.ROOT);
        return portalRepository.despachosDe(clienteId, EstadoDespacho.BORRADOR).stream()
                .filter(d -> buscado.isEmpty() || d.numero().toUpperCase(Locale.ROOT).contains(buscado))
                .filter(d -> estadoFiltro == null || d.estado() == estadoFiltro)
                .filter(d -> desde == null || (d.fechaEta() != null && !d.fechaEta().isBefore(desde)))
                .filter(d -> hasta == null || (d.fechaEta() != null && !d.fechaEta().isAfter(hasta)))
                .toList();
    }

    private static EstadoDespacho parsearEstado(String valor, Map<String, String> errores) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            EstadoDespacho estado = EstadoDespacho.valueOf(valor.trim().toUpperCase(Locale.ROOT));
            if (estado == EstadoDespacho.BORRADOR) {
                throw new IllegalArgumentException("borrador");
            }
            return estado;
        } catch (IllegalArgumentException e) {
            errores.put("estado", "Estado no válido");
            return null;
        }
    }

    private static LocalDate parsearFecha(String campo, String valor, Map<String, String> errores) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            errores.put(campo, "Use el formato AAAA-MM-DD");
            return null;
        }
    }
}
