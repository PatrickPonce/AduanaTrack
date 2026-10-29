package pe.edu.usmp.aduanatrack.common;

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
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guardado de PDF de hasta 10 MB que comparten la carta poder (HU-04) y los documentos del
 * despacho (HU-06). El archivo se guarda como {@code {uuid}.pdf}: el nombre original solo se
 * registra en la base de datos y nunca forma parte de la ruta.
 */
public final class AlmacenPdf {

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    public static final int MAX_NOMBRE_ARCHIVO = 255;
    public static final String MENSAJE_MUY_GRANDE = "El archivo supera el tamaño máximo de 10 MB.";
    public static final String MENSAJE_NO_ES_PDF = "El archivo no es un PDF válido.";

    private static final Logger log = LoggerFactory.getLogger(AlmacenPdf.class);
    private static final byte[] CABECERA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private AlmacenPdf() {
    }

    /**
     * Guarda el archivo en {@code directorio} (lo crea si no existe). 413 si pasa de 10 MB y 415
     * si el contenido no empieza con {@code %PDF-}; en los dos casos no queda nada en disco.
     */
    public static ArchivoGuardado guardar(Path directorio, MultipartFile archivo) {
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw muyGrande();
        }
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
            throw new UncheckedIOException("No se pudo guardar el PDF", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    /** Nombre que envió el navegador, sin carpetas y recortado al tamaño de la columna. */
    public static String nombreOriginal(MultipartFile archivo, String porDefecto) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return porDefecto;
        }
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1).trim();
        if (nombre.isEmpty()) {
            return porDefecto;
        }
        return nombre.length() <= MAX_NOMBRE_ARCHIVO ? nombre
                : nombre.substring(nombre.length() - MAX_NOMBRE_ARCHIVO);
    }

    /** Se usa para no dejar PDF huérfanos cuando falla el guardado en la base de datos. */
    public static void borrar(Path ruta) {
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo {}", ruta, e);
        }
    }

    private static ApiException muyGrande() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, CodigoError.ARCHIVO_MUY_GRANDE, MENSAJE_MUY_GRANDE);
    }

    public record ArchivoGuardado(Path ruta, long tamanoBytes, String sha256) {
    }
}
