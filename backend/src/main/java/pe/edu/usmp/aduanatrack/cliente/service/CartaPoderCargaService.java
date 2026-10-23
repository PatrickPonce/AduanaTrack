package pe.edu.usmp.aduanatrack.cliente.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResponse;
import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf;
import pe.edu.usmp.aduanatrack.common.AlmacenPdf.ArchivoGuardado;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.config.CartaPoderProperties;

/**
 * Carga del PDF de la carta poder (HU-04 · T-04.2). La validación y el guardado del PDF están
 * en {@link AlmacenPdf}, que también usan los documentos del despacho (HU-06).
 */
@Service
public class CartaPoderCargaService {

    static final long TAMANO_MAXIMO_BYTES = AlmacenPdf.TAMANO_MAXIMO_BYTES;

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

        ArchivoGuardado guardado = AlmacenPdf.guardar(directorio, archivo);
        try {
            CartaPoder carta = repositorio.saveAndFlush(new CartaPoder(clienteId, emision, vencimiento,
                    guardado.ruta().toString(), AlmacenPdf.nombreOriginal(archivo, "carta-poder.pdf"),
                    guardado.tamanoBytes(), guardado.sha256()));
            return CartaPoderResponse.de(carta, estadoService.estado(clienteId));
        } catch (RuntimeException e) {
            // Sin fila en la base de datos el PDF quedaría huérfano en el volumen.
            AlmacenPdf.borrar(guardado.ruta());
            throw e;
        }
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
}
