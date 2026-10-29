package pe.edu.usmp.aduanatrack.despacho.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.usmp.aduanatrack.common.ApiException;
import pe.edu.usmp.aduanatrack.common.CodigoError;
import pe.edu.usmp.aduanatrack.despacho.dto.CanalResponse;
import pe.edu.usmp.aduanatrack.despacho.entity.CanalControl;
import pe.edu.usmp.aduanatrack.despacho.entity.Despacho;
import pe.edu.usmp.aduanatrack.despacho.entity.EstadoDespacho;
import pe.edu.usmp.aduanatrack.despacho.entity.TipoHito;
import pe.edu.usmp.aduanatrack.despacho.repository.DespachoRepository;

/**
 * Registro del canal de control (HU-10 · T-10.2). Exige {@code DAM_NUMERADA}; guarda el canal en
 * {@code canal_control}, pasa el despacho a {@code CANAL_ASIGNADO} y registra el hito. Mientras el
 * despacho siga en {@code CANAL_ASIGNADO} el canal se puede corregir y cada cambio registra otro hito.
 */
@Service
public class CanalControlService {

    /** Acción que se sugiere al agente cuando SUNAT asigna canal rojo (reconocimiento físico). */
    public static final String HITO_SUGERIDO_ROJO = "Programar inspección física";

    private final DespachoRepository despachoRepository;
    private final HitoDespachoService hitos;

    public CanalControlService(DespachoRepository despachoRepository, HitoDespachoService hitos) {
        this.despachoRepository = despachoRepository;
        this.hitos = hitos;
    }

    @Transactional
    public CanalResponse guardar(Long despachoId, CanalControl canal) {
        Despacho despacho = despachoRepository.findById(despachoId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el despacho solicitado."));
        if (despacho.getEstado() != EstadoDespacho.DAM_NUMERADA
                && despacho.getEstado() != EstadoDespacho.CANAL_ASIGNADO) {
            throw new ApiException(HttpStatus.CONFLICT, CodigoError.DAM_NO_NUMERADA,
                    "Registre la numeración de la DAM antes de asignar el canal de control.");
        }

        String anterior = despacho.getCanalControl();
        boolean cambio = !canal.name().equals(anterior);
        despacho.asignarCanal(canal);

        String hitoSugerido = null;
        if (cambio) {
            hitos.registrar(despachoId, TipoHito.CANAL_ASIGNADO, anterior == null
                    ? "Canal de control asignado: " + canal
                    : "Canal de control cambiado de " + anterior + " a " + canal);
            if (canal == CanalControl.ROJO) {
                hitoSugerido = HITO_SUGERIDO_ROJO;
                hitos.registrarSugerido(despachoId, hitoSugerido);
            }
        } else if (canal == CanalControl.ROJO) {
            hitoSugerido = HITO_SUGERIDO_ROJO;
        }
        return new CanalResponse(canal, despacho.getEstado(), canal.levanteHabilitado(), hitoSugerido);
    }
}
