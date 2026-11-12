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
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guardado de archivos de hasta 10 MB que comparten la carta poder (HU-04), los documentos del
 * despacho (HU-06) y el cargo de la DAM (HU-09). El formato se reconoce por su firma (no por la
 * extensión) y el archivo se guarda como {@code {uuid}.pdf} o {@code {uuid}.jpg}: el nombre
 * original solo se registra en la base de datos y nunca forma parte de la ruta.
 */
public final class AlmacenPdf {

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024 * 1024;
    public static final int MAX_NOMBRE_ARCHIVO = 255;
    public static final String MENSAJE_MUY_GRANDE = "El archivo supera el tamaño máximo de 10 MB.";
    public static final String MENSAJE_NO_ES_PDF = "El archivo no es un PDF válido.";
    public static final String MENSAJE_NO_ES_PDF_NI_JPG = "El archivo no es un PDF ni una imagen JPG válida.";

    private static final Logger log = LoggerFactory.getLogger(AlmacenPdf.class);
    private static final int LARGO_CABECERA = 5;

    /** Formatos admitidos y su firma («número mágico») al inicio del archivo. */
    public enum Formato {
        PDF("pdf", "application/pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII)),
        JPG("jpg", "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

        private final String extension;
        private final String tipoContenido;
        private final byte[] firma;

        Formato(String extension, String tipoContenido, byte[] firma) {
            this.extension = extension;
            this.tipoContenido = tipoContenido;
            this.firma = firma;
        }

        public String extension() {
            return extension;
        }

        public String tipoContenido() {
            return tipoContenido;
        }

        /** Formato según la extensión con que se guardó el archivo; PDF si no es {@code .jpg}. */
        public static Formato deRuta(String ruta) {
            return ruta.toLowerCase(Locale.ROOT).endsWith("." + JPG.extension) ? JPG : PDF;
        }

        private boolean coincide(byte[] cabecera) {
            return cabecera.length >= firma.length && Arrays.equals(cabecera, 0, firma.length, firma, 0, firma.length);
        }
    }

    private AlmacenPdf() {
    }

    /** Solo PDF (carta poder y cargo de la DAM). */
    public static ArchivoGuardado guardar(Path directorio, MultipartFile archivo) {
        return guardar(directorio, archivo, EnumSet.of(Formato.PDF));
    }

    /**
     * Guarda el archivo en {@code directorio} (lo crea si no existe). 413 si pasa de 10 MB y 415
     * si su firma no es la de uno de los {@code permitidos}; en los dos casos no queda nada en disco.
     */
    public static ArchivoGuardado guardar(Path directorio, MultipartFile archivo, Set<Formato> permitidos) {
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw muyGrande();
        }
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(LARGO_CABECERA);
            Formato formato = permitidos.stream().filter(f -> f.coincide(cabecera)).findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            CodigoError.FORMATO_NO_PERMITIDO,
                            permitidos.contains(Formato.JPG) ? MENSAJE_NO_ES_PDF_NI_JPG : MENSAJE_NO_ES_PDF));
            return escribir(directorio.resolve(UUID.randomUUID() + "." + formato.extension()), cabecera, entrada,
                    formato);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
    }

    private static ArchivoGuardado escribir(Path destino, byte[] cabecera, InputStream entrada, Formato formato) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            Files.createDirectories(destino.getParent());
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
            return new ArchivoGuardado(destino, tamano, HexFormat.of().formatHex(sha256.digest()), formato);
        } catch (IOException e) {
            borrar(destino);
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
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

    public record ArchivoGuardado(Path ruta, long tamanoBytes, String sha256, Formato formato) {
    }
}
