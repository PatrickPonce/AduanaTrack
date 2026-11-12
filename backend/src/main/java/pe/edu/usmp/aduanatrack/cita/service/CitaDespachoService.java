package pe.edu.usmp.aduanatrack.cita.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.auth.repository.UsuarioRepository;
import pe.edu.usmp.aduanatrack.cita.dto.CitaRequest;
import pe.edu.usmp.aduanatrack.cita.dto.CitaResponse;
import pe.edu.usmp.aduanatrack.cita.dto.DepositoResponse;
import pe.edu.usmp.aduanatrack.cita.dto.TransportistaRequest;
import pe.edu.usmp.aduanatrack.cita.entity.CitaDespacho;
import pe.edu.usmp.aduanatrack.cita.entity.Deposito;
import pe.edu.usmp.aduanatrack.cita.entity.TipoCita;
import pe.edu.usmp.aduanatrack.cita.repository.CitaDespachoRepository;
import pe.edu.usmp.aduanatrack.cita.repository.DepositoRepository;
import pe.edu.usmp.aduanatrack.cliente.validacion.RucValidator;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.notificacion.service.AvisoClienteService;

/**
 * Registro de citas en depósito temporal o almacén (HU-11 · T-11.1 y T-11.2, contrato C6). El agente
 * ya coordinó la cita en la plataforma del depósito: aquí solo se guarda para tener la trazabilidad
 * dentro del despacho.
 *
 * <ul>
 * <li>Inspección: exige {@code CANAL_ASIGNADO} con canal naranja o rojo y pasa a
 * {@code INSPECCION_PROGRAMADA}; si la inspección tuvo observaciones se puede registrar otra cita.</li>
 * <li>Retiro: exige el levante otorgado y los datos del transportista, y pasa a
 * {@code RETIRO_PROGRAMADO}.</li>
 * </ul>
 */
@Service
public class CitaDespachoService {

    static final int MAX_NUMERO_CITA = 30;
    static final int MAX_RAZON_SOCIAL = 150;
    static final int MAX_PLACA = 10;
    static final int MAX_CONDUCTOR = 100;
    private static final Pattern FORMATO_PLACA = Pattern.compile("[A-Z0-9][A-Z0-9-]*");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DespachoRepository despachoRepository;
    private final DepositoRepository depositoRepository;
    private final CitaDespachoRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HitoDespachoService hitos;
    private final AvisoClienteService avisosCliente;
    private final Clock clock;

    public CitaDespachoService(DespachoRepository despachoRepository, DepositoRepository depositoRepository,
            CitaDespachoRepository citaRepository, UsuarioRepository usuarioRepository, HitoDespachoService hitos,
            AvisoClienteService avisosCliente, Clock clock) {
        this.despachoRepository = despachoRepository;
        this.depositoRepository = depositoRepository;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.hitos = hitos;
        this.avisosCliente = avisosCliente;
        this.clock = clock;
    }

    /** Depósitos y almacenes activos para el selector de la pantalla «Citas». */
    @Transactional(readOnly = true)
    public List<DepositoResponse> depositos() {
        return depositoRepository.findByActivoTrueOrderByCodigoAsc().stream().map(DepositoResponse::de).toList();
    }

    /** Citas del despacho ordenadas por la fecha y hora confirmadas (CA2 y CA3). */
    @Transactional(readOnly = true)
    public List<CitaResponse> listar(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        return citaRepository.listar(despachoId).stream().map(c -> CitaResponse.de(c, despacho.getEstado())).toList();
    }

    /**
     * Guarda la cita asociada al despacho (CA1) y deja el hito {@code CITA_REGISTRADA}. 400 si falta un
     * dato obligatorio, como la fecha y hora (CA4); 404 si el despacho o el depósito no existen; 409
     * {@code CITA_INSPECCION_NO_APLICA} o {@code CITA_RETIRO_SIN_LEVANTE} si el estado del despacho no
     * permite ese tipo de cita.
     */
    @Transactional
    public CitaResponse registrar(Long despachoId, CitaRequest solicitud, Long usuarioId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho antes de registrar citas.");
        }
        Deposito deposito = depositoRepository.findByIdAndActivoTrue(solicitud.depositoId())
                .orElseThrow(() -> ApiException.validacion(Map.of("depositoId", "Seleccione un depósito válido")));
        String numeroCita = solicitud.numeroCita().trim();
        if (numeroCita.isEmpty() || numeroCita.length() > MAX_NUMERO_CITA) {
            throw ApiException.validacion(Map.of("numeroCita", "Ingrese el N° de cita o ticket (máximo "
                    + MAX_NUMERO_CITA + " caracteres)"));
        }
        TransportistaRequest transportista = solicitud.tipo() == TipoCita.RETIRO
                ? validarTransportista(solicitud.transportista())
                : null;
        validarEstado(despacho, solicitud.tipo());

        CitaDespacho cita = new CitaDespacho(despachoId, deposito, numeroCita, solicitud.tipo(),
                solicitud.fechaHoraConfirmada(), usuarioRepository.getReferenceById(usuarioId),
                LocalDateTime.now(clock));
        if (transportista != null) {
            cita.asignarTransportista(transportista.ruc(), transportista.razonSocial(), transportista.placa(),
                    transportista.conductor());
        }
        citaRepository.save(cita);

        String detalle = numeroCita + " el " + FECHA_HORA.format(cita.getFechaHoraConfirmada()) + " en "
                + deposito.getNombre();
        hitos.registrar(despachoId, TipoHito.CITA_REGISTRADA,
                "Cita de " + solicitud.tipo().texto().toLowerCase(Locale.ROOT) + " registrada: " + detalle);
        avanzarEstado(despachoId, despacho, solicitud.tipo(), detalle);
        if (solicitud.tipo() == TipoCita.RETIRO) {
            avisosCliente.retiroProgramado(despacho, detalle);
        }
        return CitaResponse.de(cita, despacho.getEstado());
    }

    /** CA2 y CA3: el tipo de cita depende de dónde está el despacho. */
    private static void validarEstado(Despacho despacho, TipoCita tipo) {
        EstadoDespacho estado = despacho.getEstado();
        if (tipo == TipoCita.RETIRO) {
            if (!estado.alcanzo(EstadoDespacho.LEVANTE_OTORGADO)) {
                throw new ApiException(HttpStatus.CONFLICT, CodigoError.CITA_RETIRO_SIN_LEVANTE,
                        "Registre el levante antes de programar el retiro de la mercancía.");
            }
            return;
        }
        String canal = despacho.getCanalControl();
        boolean requiereInspeccion = "NARANJA".equalsIgnoreCase(canal) || "ROJO".equalsIgnoreCase(canal);
        boolean enPuntoDeInspeccion = estado == EstadoDespacho.CANAL_ASIGNADO
                || estado == EstadoDespacho.INSPECCION_PROGRAMADA;
        if (enPuntoDeInspeccion && requiereInspeccion) {
            return;
        }
        String mensaje;
        if (!estado.alcanzo(EstadoDespacho.CANAL_ASIGNADO)) {
            mensaje = "Asigne el canal de control antes de programar la inspección.";
        } else if (estado.alcanzo(EstadoDespacho.INSPECCION_CONFORME)) {
            mensaje = "La inspección ya se registró como conforme: continúe con el levante.";
        } else {
            mensaje = "El canal verde no requiere inspección: registre el levante.";
        }
        throw new ApiException(HttpStatus.CONFLICT, CodigoError.CITA_INSPECCION_NO_APLICA, mensaje);
    }

    private void avanzarEstado(Long despachoId, Despacho despacho, TipoCita tipo, String detalle) {
        EstadoDespacho nuevo = tipo == TipoCita.INSPECCION ? EstadoDespacho.INSPECCION_PROGRAMADA
                : EstadoDespacho.RETIRO_PROGRAMADO;
        if (despacho.getEstado() == nuevo) {
            return;
        }
        despacho.cambiarEstado(nuevo);
        hitos.registrar(despachoId, TipoHito.valueOf(nuevo.name()),
                (tipo == TipoCita.INSPECCION ? "Inspección programada: " : "Retiro programado: ") + detalle);
    }

    /** CA3: la cita de retiro guarda RUC, razón social, placa y conductor del transportista. */
    private static TransportistaRequest validarTransportista(TransportistaRequest datos) {
        Map<String, String> errores = new LinkedHashMap<>();
        String ruc = datos == null || datos.ruc() == null ? "" : datos.ruc().trim();
        String razonSocial = datos == null || datos.razonSocial() == null ? "" : datos.razonSocial().trim();
        String placa = datos == null || datos.placa() == null ? "" : datos.placa().trim().toUpperCase(Locale.ROOT);
        String conductor = datos == null || datos.conductor() == null ? "" : datos.conductor().trim();

        if (ruc.isEmpty()) {
            errores.put("transportista.ruc", "Ingrese el RUC del transportista");
        } else if (!RucValidator.esValido(ruc)) {
            errores.put("transportista.ruc", "RUC no válido");
        }
        if (razonSocial.isEmpty()) {
            errores.put("transportista.razonSocial", "Ingrese la razón social del transportista");
        } else if (razonSocial.length() > MAX_RAZON_SOCIAL) {
            errores.put("transportista.razonSocial", "Máximo " + MAX_RAZON_SOCIAL + " caracteres");
        }
        if (placa.isEmpty()) {
            errores.put("transportista.placa", "Ingrese la placa del vehículo");
        } else if (placa.length() > MAX_PLACA || !FORMATO_PLACA.matcher(placa).matches()) {
            errores.put("transportista.placa", "Placa no válida (letras, números y guion)");
        }
        if (conductor.isEmpty()) {
            errores.put("transportista.conductor", "Ingrese el nombre del conductor");
        } else if (conductor.length() > MAX_CONDUCTOR) {
            errores.put("transportista.conductor", "Máximo " + MAX_CONDUCTOR + " caracteres");
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
        return new TransportistaRequest(ruc, razonSocial, placa, conductor);
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }
}
