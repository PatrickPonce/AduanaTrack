package pe.edu.usmp.aduanatrack.levante.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.common.AlmacenPdf;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf.ArchivoGuardado;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.DocumentoProperties;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;
import pe.edu.usmp.aduanatrack.levante.dto.LevanteRequest;
import pe.edu.usmp.aduanatrack.levante.dto.LevanteResponse;

/**
 * Registro manual del levante de la mercancía (HU-13 · T-13.1 y T-13.2, contrato C8). El agente obtiene
 * el levante en SUNAT Operaciones en Línea y aquí deja el número, la fecha y la constancia en PDF.
 *
 * <p>Los requisitos previos son el pago de tributos y la inspección conforme (o canal verde). El sistema
 * advierte pero no bloquea: sin {@code confirmarSinRequisitos} responde 409 con la lista de pendientes y,
 * con la confirmación, guarda el levante marcado con advertencia (CA2).
 */
@Service
public class LevanteService {

    static final int MAX_NUMERO_LEVANTE = 30;
    static final String PENDIENTE_PAGO = "Pago de tributos";
    static final String PENDIENTE_INSPECCION = "Inspección conforme";
    private static final Pattern FORMATO_LEVANTE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/-]*");
    private static final String SUBCARPETA_CONSTANCIA = "constancia-levante";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DespachoRepository despachoRepository;
    private final HitoDespachoService hitos;
    private final Clock clock;
    private final Path directorio;

    public LevanteService(DespachoRepository despachoRepository, HitoDespachoService hitos, Clock clock,
            DocumentoProperties propiedades) {
        this.despachoRepository = despachoRepository;
        this.hitos = hitos;
        this.clock = clock;
        this.directorio = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    /** Siempre responde: los datos del levante vienen nulos mientras el despacho no lo tiene. */
    @Transactional(readOnly = true)
    public LevanteResponse obtener(Long despachoId) {
        return LevanteResponse.de(buscarDespacho(despachoId));
    }

    /**
     * Guarda el levante y pasa el despacho a {@code LEVANTE_OTORGADO} (CA1). 400 si el número o la fecha no
     * son válidos, 404 si el despacho no existe, 409 {@code LEVANTE_YA_REGISTRADO} si ya tiene levante, 409
     * {@code LEVANTE_REQUISITOS_PENDIENTES} con {@code pendientes} si falta el pago o la inspección y no hay
     * confirmación (CA2), y 409 {@code LEVANTE_DUPLICADO} si el número ya está en otro despacho.
     */
    @Transactional
    public LevanteResponse registrar(Long despachoId, LevanteRequest solicitud, MultipartFile constancia) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.esBorrador()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DESPACHO_EN_BORRADOR,
                    "Confirme el despacho antes de registrar el levante.");
        }
        String numero = validarNumero(solicitud.numeroLevante());
        LocalDate fecha = validarFecha(solicitud.fechaLevante());

        if (despacho.getEstado().alcanzo(EstadoDespacho.LEVANTE_OTORGADO)) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.LEVANTE_YA_REGISTRADO,
                    "El levante de este despacho ya está registrado.");
        }
        List<String> pendientes = requisitosPendientes(despacho);
        if (!pendientes.isEmpty() && !solicitud.confirmaSinRequisitos()) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.LEVANTE_REQUISITOS_PENDIENTES,
                    "Faltan requisitos previos al levante: " + String.join(", ", pendientes).toLowerCase() + ".",
                    Map.of("pendientes", pendientes));
        }
        if (despachoRepository.existsByNumeroLevante(numero)) {
            throw duplicado();
        }

        ArchivoGuardado guardado = null;
        String nombreConstancia = null;
        String rutaConstancia = null;
        if (constancia != null && !constancia.isEmpty()) {
            guardado = AlmacenPdf.guardar(directorio.resolve(despachoId.toString()).resolve(SUBCARPETA_CONSTANCIA),
                    constancia);
            nombreConstancia = AlmacenPdf.nombreOriginal(constancia, "constancia-levante.pdf");
            rutaConstancia = despachoId + "/" + SUBCARPETA_CONSTANCIA + "/" + guardado.ruta().getFileName();
        }
        try {
            boolean conAdvertencia = !pendientes.isEmpty();
            despacho.registrarLevante(numero, fecha, nombreConstancia, rutaConstancia, conAdvertencia);
            despachoRepository.saveAndFlush(despacho);
            hitos.registrar(despachoId, TipoHito.LEVANTE_OTORGADO,
                    "Levante otorgado: " + numero + " (" + FECHA.format(fecha) + ")");
            if (conAdvertencia) {
                hitos.registrar(despachoId, TipoHito.LEVANTE_CON_ADVERTENCIA,
                        "Levante registrado con requisitos pendientes: " + String.join(", ", pendientes));
            }
            return LevanteResponse.de(despacho);
        } catch (DataIntegrityViolationException e) {
            borrarConstancia(guardado);
            throw duplicado();
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            borrarConstancia(guardado);
            throw e;
        }
    }

    /** PDF de la constancia; la ruta se resuelve dentro del directorio configurado. */
    @Transactional(readOnly = true)
    public ArchivoConstancia constancia(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.getConstanciaLevanteRuta() == null) {
            throw ApiException.noEncontrado("El levante no tiene constancia adjunta.");
        }
        Path ruta = directorio.resolve(despacho.getConstanciaLevanteRuta()).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("El archivo de la constancia no está disponible.");
        }
        return new ArchivoConstancia(ruta, despacho.getConstanciaLevanteNombre());
    }

    /**
     * T-13.2: pago de tributos (el despacho alcanzó {@code TRIBUTOS_PAGADOS}) e inspección conforme. El
     * canal verde no tiene inspección, así que cuenta como cumplida.
     */
    static List<String> requisitosPendientes(Despacho despacho) {
        EstadoDespacho estado = despacho.getEstado();
        List<String> pendientes = new ArrayList<>();
        if (!estado.alcanzo(EstadoDespacho.TRIBUTOS_PAGADOS)) {
            pendientes.add(PENDIENTE_PAGO);
        }
        boolean canalVerde = "VERDE".equalsIgnoreCase(despacho.getCanalControl())
                && estado.alcanzo(EstadoDespacho.CANAL_ASIGNADO);
        if (!canalVerde && !estado.alcanzo(EstadoDespacho.INSPECCION_CONFORME)) {
            pendientes.add(PENDIENTE_INSPECCION);
        }
        return pendientes;
    }

    private static String validarNumero(String numeroLevante) {
        String numero = numeroLevante == null ? "" : numeroLevante.trim();
        if (numero.isEmpty()) {
            throw ApiException.validacion(Map.of("numeroLevante", "Ingrese el N° de levante"));
        }
        if (numero.length() > MAX_NUMERO_LEVANTE) {
            throw ApiException.validacion(Map.of("numeroLevante", "Máximo " + MAX_NUMERO_LEVANTE + " caracteres"));
        }
        if (!FORMATO_LEVANTE.matcher(numero).matches()) {
            throw ApiException.validacion(
                    Map.of("numeroLevante", "Use solo letras, números, guiones y barras"));
        }
        return numero;
    }

    private LocalDate validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw ApiException.validacion(Map.of("fechaLevante", "Ingrese la fecha del levante"));
        }
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw ApiException.validacion(Map.of("fechaLevante", "La fecha del levante no puede ser futura"));
        }
        return fecha;
    }

    private static ApiException duplicado() {
        return new ApiException(HttpStatus.CONFLICT, CodigoError.LEVANTE_DUPLICADO,
                "Ese N° de levante ya está registrado en otro despacho.");
    }

    private static void borrarConstancia(ArchivoGuardado guardado) {
        if (guardado != null) {
            AlmacenPdf.borrar(guardado.ruta());
        }
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    public record ArchivoConstancia(Path ruta, String nombre) {
    }
}
