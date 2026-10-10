package pe.edu.usmp.aduanatrack.cliente.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.dto.EstadoCartaPoder;
import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.config.service.ParametroService;

/** Estado de la carta poder del cliente: VIGENTE, POR_VENCER (≤ 15 días), VENCIDA o SIN_CARTA. */
@Service
public class CartaPoderEstadoService {

    private final CartaPoderRepository repositorio;
    private final ParametroService parametros;
    private final Clock clock;

    public CartaPoderEstadoService(CartaPoderRepository repositorio, ParametroService parametros, Clock clock) {
        this.repositorio = repositorio;
        this.parametros = parametros;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EstadoCartaPoder estado(Long clienteId) {
        return calcular(repositorio.maximoVencimiento(clienteId), LocalDate.now(clock),
                parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER));
    }

    /** Estado de varios clientes con una sola consulta (listados y Dashboard). */
    @Transactional(readOnly = true)
    public Map<Long, EstadoCartaPoder> estados(Collection<Long> clienteIds) {
        Map<Long, EstadoCartaPoder> resultado = new HashMap<>();
        if (clienteIds.isEmpty()) {
            return resultado;
        }
        LocalDate hoy = LocalDate.now(clock);
        int diasAlerta = parametros.entero(ParametroService.DIAS_ALERTA_CARTA_PODER);
        Map<Long, LocalDate> vencimientos = new HashMap<>();
        List<Object[]> filas = repositorio.maximoVencimientoPorCliente(clienteIds);
        for (Object[] fila : filas) {
            vencimientos.put((Long) fila[0], (LocalDate) fila[1]);
        }
        for (Long id : clienteIds) {
            resultado.put(id, calcular(vencimientos.get(id), hoy, diasAlerta));
        }
        return resultado;
    }

    static EstadoCartaPoder calcular(LocalDate vencimiento, LocalDate hoy, int diasAlerta) {
        if (vencimiento == null) {
            return EstadoCartaPoder.SIN_CARTA;
        }
        if (vencimiento.isBefore(hoy)) {
            return EstadoCartaPoder.VENCIDA;
        }
        if (!vencimiento.isAfter(hoy.plusDays(diasAlerta))) {
            return EstadoCartaPoder.POR_VENCER;
        }
        return EstadoCartaPoder.VIGENTE;
    }
}
