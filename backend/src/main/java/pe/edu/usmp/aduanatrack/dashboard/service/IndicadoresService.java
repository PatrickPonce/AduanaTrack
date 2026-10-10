package pe.edu.usmp.aduanatrack.dashboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.service.CartaPoderEstadoService;
import pe.edu.usmp.aduanatrack.dashboard.dto.IndicadoresResponse;
import pe.edu.usmp.aduanatrack.dashboard.repository.TableroRepository;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.embarque.service.PlazoAlmacenajeService;

/**
 * Indicadores del Dashboard con los mismos filtros que la tabla (HU-02 · T-02.1). Los conteos
 * se hacen en la base de datos; las fechas de corte de la alerta crítica salen de los mismos
 * servicios que usa la tabla, para que la tarjeta y las filas marcadas siempre coincidan.
 */
@Service
public class IndicadoresService {

    private final TableroRepository tableroRepository;
    private final PlazoAlmacenajeService plazoAlmacenaje;
    private final CartaPoderEstadoService cartaPoderEstado;

    public IndicadoresService(TableroRepository tableroRepository, PlazoAlmacenajeService plazoAlmacenaje,
            CartaPoderEstadoService cartaPoderEstado) {
        this.tableroRepository = tableroRepository;
        this.plazoAlmacenaje = plazoAlmacenaje;
        this.cartaPoderEstado = cartaPoderEstado;
    }

    @Transactional(readOnly = true)
    public IndicadoresResponse indicadores(Long aduanaId, Long clienteId) {
        return tableroRepository.indicadores(aduanaId, clienteId, EstadoDespacho.BORRADOR,
                plazoAlmacenaje.fechaLimiteEnAlerta(), cartaPoderEstado.vencimientoCritico());
    }
}
