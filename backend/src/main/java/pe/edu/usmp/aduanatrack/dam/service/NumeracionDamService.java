package pe.edu.usmp.aduanatrack.dam.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import pe.edu.usmp.aduanatrack.dam.dto.DamResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;
import pe.edu.usmp.aduanatrack.despacho.service.HitoDespachoService;

/**
 * Registro manual de la numeración de la DAM (HU-09 · T-09.1, contrato C4). El número se obtiene
 * en el sistema externo de SUNAT; aquí se guarda con su fecha y, si se adjunta, el cargo en PDF
 * (con la misma validación de {@link AlmacenPdf}). Exige {@code TRIBUTOS_PAGADOS}.
 */
@Service
public class NumeracionDamService {

    static final int MAX_NUMERO_DAM = 30;
    private static final Pattern FORMATO_DAM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/-]*");
    private static final String SUBCARPETA_CARGO = "cargo-dam";

    private final DespachoRepository despachoRepository;
    private final HitoDespachoService hitoService;
    private final Clock clock;
    private final Path directorio;

    public NumeracionDamService(DespachoRepository despachoRepository, HitoDespachoService hitoService, Clock clock,
            DocumentoProperties propiedades) {
        this.despachoRepository = despachoRepository;
        this.hitoService = hitoService;
        this.clock = clock;
        this.directorio = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public DamResponse obtener(Long despachoId) {
        return respuesta(buscarDespacho(despachoId));
    }

    /**
     * Guarda el N° de DAM y la fecha de numeración (CA1) y pasa el despacho a {@code DAM_NUMERADA}
     * (CA3). {@code fechaNumeracion} es opcional (hoy por defecto) y {@code cargo} también (CA4).
     * 400 si el número está vacío o no es válido (CA2) o la fecha es futura, 404 si el despacho no
     * existe, 409 {@code TRIBUTOS_PENDIENTES} si no está en {@code TRIBUTOS_PAGADOS} y 409
     * {@code DAM_DUPLICADA} si el número ya está en otro despacho.
     */
    @Transactional
    public DamResponse registrar(Long despachoId, String numeroDam, String fechaNumeracion, MultipartFile cargo) {
        Despacho despacho = buscarDespacho(despachoId);
        String numero = numeroDam == null ? "" : numeroDam.trim();
        LocalDate fecha = validar(numero, fechaNumeracion);

        if (despacho.getEstado() != EstadoDespacho.TRIBUTOS_PAGADOS) {
            String mensaje = despacho.getEstado().alcanzo(EstadoDespacho.DAM_NUMERADA)
                    ? "La DAM de este despacho ya está registrada."
                    : "Registre el pago de tributos antes de numerar la DAM.";
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.TRIBUTOS_PENDIENTES, mensaje);
        }
        if (despachoRepository.existsByNumeroDam(numero)) {
            throw duplicada();
        }

        ArchivoGuardado guardado = null;
        String nombreCargo = null;
        String rutaCargo = null;
        if (cargo != null && !cargo.isEmpty()) {
            guardado = AlmacenPdf.guardar(directorio.resolve(despachoId.toString()).resolve(SUBCARPETA_CARGO), cargo);
            nombreCargo = AlmacenPdf.nombreOriginal(cargo, "cargo-dam.pdf");
            rutaCargo = despachoId + "/" + SUBCARPETA_CARGO + "/" + guardado.ruta().getFileName();
        }
        try {
            despacho.registrarDam(numero, fecha, nombreCargo, rutaCargo);
            despachoRepository.saveAndFlush(despacho);
            hitoService.registrar(despachoId, TipoHito.DAM_NUMERADA, "DAM numerada: " + numero);
            return respuesta(despacho);
        } catch (DataIntegrityViolationException e) {
            borrarCargo(guardado);
            throw duplicada();
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            borrarCargo(guardado);
            throw e;
        }
    }

    /** PDF del cargo de numeración; la ruta se resuelve dentro del directorio configurado. */
    @Transactional(readOnly = true)
    public ArchivoCargo cargo(Long despachoId) {
        Despacho despacho = buscarDespacho(despachoId);
        if (despacho.getCargoRuta() == null) {
            throw ApiException.noEncontrado("La DAM no tiene cargo de numeración adjunto.");
        }
        Path ruta = directorio.resolve(despacho.getCargoRuta()).normalize();
        if (!ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("El archivo del cargo no está disponible.");
        }
        return new ArchivoCargo(ruta, despacho.getCargoNombre());
    }

    private LocalDate validar(String numero, String fechaNumeracion) {
        if (numero.isEmpty()) {
            throw ApiException.validacion(Map.of("numeroDam", "Ingrese el N° de DAM"));
        }
        if (numero.length() > MAX_NUMERO_DAM) {
            throw ApiException.validacion(Map.of("numeroDam", "Máximo " + MAX_NUMERO_DAM + " caracteres"));
        }
        if (!FORMATO_DAM.matcher(numero).matches()) {
            throw ApiException.validacion(Map.of("numeroDam", "Use solo letras, números, guiones y barras"));
        }
        LocalDate hoy = LocalDate.now(clock);
        if (fechaNumeracion == null || fechaNumeracion.isBlank()) {
            return hoy;
        }
        try {
            LocalDate fecha = LocalDate.parse(fechaNumeracion.trim());
            if (fecha.isAfter(hoy)) {
                throw ApiException.validacion(
                        Map.of("fechaNumeracion", "La fecha de numeración no puede ser futura"));
            }
            return fecha;
        } catch (DateTimeParseException e) {
            throw ApiException.validacion(Map.of("fechaNumeracion", "Fecha no válida (use AAAA-MM-DD)"));
        }
    }

    private static DamResponse respuesta(Despacho despacho) {
        return new DamResponse(despacho.getNumeroDam(), despacho.getFechaNumeracion(), despacho.getCargoNombre(),
                despacho.getEstado().name());
    }

    private static ApiException duplicada() {
        return new ApiException(HttpStatus.CONFLICT, CodigoError.DAM_DUPLICADA,
                "Ese N° de DAM ya está registrado en otro despacho.");
    }

    private static void borrarCargo(ArchivoGuardado guardado) {
        if (guardado != null) {
            AlmacenPdf.borrar(guardado.ruta());
        }
    }

    private Despacho buscarDespacho(Long despachoId) {
        return despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
    }

    public record ArchivoCargo(Path ruta, String nombre) {
    }
}
