package pe.edu.usmp.aduanatrack.cliente.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.dto.CartaPoderResumen;
import pe.edu.usmp.aduanatrack.cliente.entity.CartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.cliente.repository.ClienteRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.config.CartaPoderProperties;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;

/** Cartas poder ya cargadas del cliente: tabla del formulario de HU-04 y descarga del PDF. */
@Service
public class CartaPoderConsultaService {

    private final CartaPoderRepository repositorio;
    private final ClienteRepository clienteRepository;
    private final ParametroService parametros;
    private final Clock clock;
    private final Path directorio;

    public CartaPoderConsultaService(CartaPoderRepository repositorio, ClienteRepository clienteRepository,
            ParametroService parametros, Clock clock, CartaPoderProperties propiedades) {
        this.repositorio = repositorio;
        this.clienteRepository = clienteRepository;
        this.parametros = parametros;
        this.clock = clock;
        this.directorio = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<CartaPoderResumen> listar(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw ApiException.noEncontrado("No existe el cliente solicitado.");
        }
        LocalDate hoy = LocalDate.now(clock);
        int diasAlerta = parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER);
        return repositorio.findByClienteIdOrderByFechaVencimientoDescIdDesc(clienteId).stream()
                .map(c -> CartaPoderResumen.de(c,
                        CartaPoderEstadoService.calcular(c.getFechaVencimiento(), hoy, diasAlerta)))
                .toList();
    }

    /**
     * PDF de una carta del cliente. De la ruta guardada solo se toma el nombre ({uuid}.pdf):
     * el archivo siempre se lee del directorio configurado, nunca de otra carpeta.
     */
    @Transactional(readOnly = true)
    public ArchivoCartaPoder archivo(Long clienteId, Long cartaId) {
        CartaPoder carta = repositorio.findByIdAndClienteId(cartaId, clienteId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe la carta poder solicitada."));
        Path nombre = Path.of(carta.getRutaArchivo()).getFileName();
        Path ruta = nombre == null ? null : directorio.resolve(nombre).normalize();
        if (ruta == null || !ruta.startsWith(directorio) || !Files.isRegularFile(ruta)) {
            throw ApiException.noEncontrado("El archivo de la carta poder no está disponible.");
        }
        return new ArchivoCartaPoder(ruta, carta.getNombreArchivo());
    }

    public record ArchivoCartaPoder(Path ruta, String nombre) {
    }
}
