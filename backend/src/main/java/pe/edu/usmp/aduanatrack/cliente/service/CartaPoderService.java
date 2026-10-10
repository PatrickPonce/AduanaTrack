package pe.edu.usmp.aduanatrack.cliente.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.cliente.repository.CartaPoderRepository;
import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;

/** Vigencia de la carta poder al abrir un despacho (HU-03 · T-03.3). */
@Service
public class CartaPoderService {

    static final String MENSAJE_NO_VIGENTE = "El cliente no cuenta con carta poder vigente. "
            + "Actualice la carta poder antes de abrir el despacho.";

    private final CartaPoderRepository repositorio;

    public CartaPoderService(CartaPoderRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Hay al menos una carta con fecha de vencimiento igual o posterior a hoy. */
    @Transactional(readOnly = true)
    public boolean tieneVigente(Long clienteId, LocalDate hoy) {
        return repositorio.existsByClienteIdAndFechaVencimientoGreaterThanEqual(clienteId, hoy);
    }

    /** 422 CARTA_PODER_NO_VIGENTE si el cliente no tiene carta vigente (HU-03, CA2). */
    public void exigirVigente(Long clienteId, LocalDate hoy) {
        if (!tieneVigente(clienteId, hoy)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, CodigoError.CARTA_PODER_NO_VIGENTE,
                    MENSAJE_NO_VIGENTE);
        }
    }
}
