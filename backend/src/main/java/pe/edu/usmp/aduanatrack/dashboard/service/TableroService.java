package pe.edu.usmp.aduanatrack.dashboard.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroFila;
import pe.edu.usmp.aduanatrack.dashboard.dto.DespachoTableroResponse;
import pe.edu.usmp.aduanatrack.dashboard.repository.TableroRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService.Plazo;

/** Tabla "Despachos recientes" del Dashboard con filtros por aduana y cliente (HU-02 · T-02.2). */
@Service
public class TableroService {

    private final TableroRepository tableroRepository;
    private final PlazoAlmacenajeService plazoAlmacenaje;
    private final CartaPoderEstadoService cartaPoderEstado;

    public TableroService(TableroRepository tableroRepository, PlazoAlmacenajeService plazoAlmacenaje,
            CartaPoderEstadoService cartaPoderEstado) {
        this.tableroRepository = tableroRepository;
        this.plazoAlmacenaje = plazoAlmacenaje;
        this.cartaPoderEstado = cartaPoderEstado;
    }

    @Transactional(readOnly = true)
    public List<DespachoTableroResponse> despachos(Long aduanaId, Long clienteId) {
        List<DespachoTableroFila> filas = tableroRepository.despachos(aduanaId, clienteId, EstadoDespacho.BORRADOR);
        Set<Long> clientes = filas.stream().map(DespachoTableroFila::clienteId).collect(Collectors.toSet());
        Map<Long, EstadoCartaPoder> cartas = cartaPoderEstado.estados(clientes);
        return filas.stream().map(f -> fila(f, cartas.get(f.clienteId()))).toList();
    }

    private DespachoTableroResponse fila(DespachoTableroFila f, EstadoCartaPoder carta) {
        Plazo plazo = plazoAlmacenaje.plazo(f.fechaLimiteAlmacenaje());
        boolean alertaCritica = plazo.alerta() || (carta != null && carta.esCritico());
        long totalContenedores = f.totalContenedores() == null ? 0 : f.totalContenedores();
        return new DespachoTableroResponse(f.id(), f.numero(), f.clienteId(), f.cliente(), f.aduanaCodigo(),
                f.aduana(), f.numeroBl(), f.contenedor(), totalContenedores, f.fechaEta(), f.estado(),
                f.canalControl(), plazo.diasRestantes(), plazo.alerta(), carta, alertaCritica);
    }
}
