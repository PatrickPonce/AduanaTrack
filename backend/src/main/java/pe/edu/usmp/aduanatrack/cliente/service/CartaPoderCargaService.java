package pe.edu.usmp.aduanatrack.cliente.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResponse;
import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.config.CartaPoderProperties;

/**
 * Carga del PDF de la carta poder (HU-04 · T-04.2). El archivo se guarda como
 * {@code {uuid}.pdf}: el nombre original solo se registra en la base de datos y nunca
 * forma parte de la ruta.
 */
@Service
public class CartaPoderCargaService {

    static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    static final int MAX_NOMBRE_ARCHIVO = 255;
    static final String MENSAJE_MUY_GRANDE = "El archivo supera el tamaño máximo de 10 MB.";
    static final String MENSAJE_NO_ES_PDF = "El archivo no es un PDF válido.";

    private static final Logger log = LoggerFactory.getLogger(CartaPoderCargaService.class);
    private static final byte[] CABECERA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final CartaPoderRepository repositorio;
    private final ClienteRepository clienteRepository;
    private final CartaPoderEstadoService estadoService;
    private final Path directorio;

    public CartaPoderCargaService(CartaPoderRepository repositorio, ClienteRepository clienteRepository,
            CartaPoderEstadoService estadoService, CartaPoderProperties propiedades) {
        this.repositorio = repositorio;
        this.clienteRepository = clienteRepository;
        this.estadoService = estadoService;
        this.directorio = Path.of(propiedades.directorio());
    }

    /**
     * 404 si el cliente no existe, 400 con los campos que faltan, 413 si pasa de 10 MB y
     * 415 si el contenido no empieza con {@code %PDF-}. Las fechas llegan como texto
     * (AAAA-MM-DD) para devolver el error en el campo y no un error genérico.
     */
    @Transactional
    public CartaPoderResponse cargar(Long clienteId, MultipartFile archivo, String fechaEmision,
            String fechaVencimiento) {
        if (!clienteRepository.existsById(clienteId)) {
            throw ApiException.noEncontrado("No existe el cliente solicitado.");
        }
        Map<String, String> campos = new LinkedHashMap<>();
        if (archivo == null || archivo.isEmpty()) {
            campos.put("archivo", "Seleccione el PDF de la carta poder");
        }
        LocalDate emision = fecha(fechaEmision, "fechaEmision", "Ingrese la fecha de emisión", campos);
        LocalDate vencimiento = fecha(fechaVencimiento, "fechaVencimiento", "Ingrese la fecha de vigencia", campos);
        if (emision != null && vencimiento != null && !vencimiento.isAfter(emision)) {
            campos.put("fechaVencimiento", "Debe ser posterior a la fecha de emisión");
        }
        if (!campos.isEmpty()) {
            throw ApiException.validacion(campos);
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw muyGrande();
        }

        ArchivoGuardado guardado = guardar(archivo);
        try {
            CartaPoder carta = repositorio.saveAndFlush(new CartaPoder(clienteId, emision, vencimiento,
                    guardado.ruta().toString(), nombreOriginal(archivo), guardado.tamanoBytes(), guardado.sha256()));
            return CartaPoderResponse.de(carta, estadoService.estado(clienteId));
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            borrar(guardado.ruta());
            throw e;
        }
    }

    private ArchivoGuardado guardar(MultipartFile archivo) {
        Path destino = directorio.resolve(UUID.randomUUID() + ".pdf");
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(CABECERA_PDF.length);
            if (!Arrays.equals(cabecera, CABECERA_PDF)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CodigoError.FORMATO_NO_PERMITIDO,
                        MENSAJE_NO_ES_PDF);
            }
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            Files.createDirectories(directorio);
            long tamano;
            try (OutputStream salida = new DigestOutputStream(
                    Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW), sha256)) {
                salida.write(cabecera);
                tamano = cabecera.length + entrada.transferTo(salida);
            }
            if (tamano > TAMANO_MAXIMO_BYTES) {
                borrar(destino);
                throw muyGrande();
            }
            return new ArchivoGuardado(destino, tamano, HexFormat.of().formatHex(sha256.digest()));
        } catch (IOException e) {
            borrar(destino);
            throw new UncheckedIOException("No se pudo guardar la carta poder", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    /** Nombre que envió el navegador, sin carpetas y recortado al tamaño de la columna. */
    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "carta-poder.pdf";
        }
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1).trim();
        if (nombre.isEmpty()) {
            return "carta-poder.pdf";
        }
        return nombre.length() <= MAX_NOMBRE_ARCHIVO ? nombre
                : nombre.substring(nombre.length() - MAX_NOMBRE_ARCHIVO);
    }

    private static LocalDate fecha(String valor, String campo, String mensajeVacio, Map<String, String> campos) {
        if (valor == null || valor.isBlank()) {
            campos.put(campo, mensajeVacio);
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            campos.put(campo, "Fecha no válida (AAAA-MM-DD)");
            return null;
        }
    }

    private static ApiException muyGrande() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, CodigoError.ARCHIVO_MUY_GRANDE, MENSAJE_MUY_GRANDE);
    }

    private static void borrar(Path ruta) {
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}", ruta, e);
        }
    }

    private record ArchivoGuardado(Path ruta, long tamanoBytes, String sha256) {
    }
}
