package net.dualcorp.cobrospayphonews.rest;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dualcorp.cobrospayphonews.modelado.dtos.AccionTransaccionResponseDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.CobroRequestDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.CobroResponseDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.EstadoTransaccionDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.IdRequestDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.RespuestaErrorDTO;
import net.dualcorp.cobrospayphonews.servicio.PagoServicio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



/**
 * Controlador REST para operaciones de pago PayPhone.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pagos")
public class PagoRest {

    private final PagoServicio pagoServicio;

    /**
     * Genera un cobro en PayPhone.
     *
     * @param request cuerpo del cobro
     * @return respuesta con estado pendiente
     */
    @PostMapping("/cobrar")
    public ResponseEntity<?> generarCobro(@Valid @RequestBody CobroRequestDTO request) {
        try {
            CobroResponseDTO respuesta = pagoServicio.generarCobro(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion generar cobro {}", e.getMessage());
            return construirError(HttpStatus.BAD_REQUEST, "cobro_invalido", e.getMessage());
        } catch (Exception e) {
            log.error("Fallo al generar cobro", e);
            return construirError(HttpStatus.INTERNAL_SERVER_ERROR, "error_generar_cobro", e.getMessage());
        }
    }

    /**
     * Consulta el estado de un cobro.
     *
     * @param clientTransactionId identificador interno
     * @return estado actual
     */
    @GetMapping("/estado/{clientTransactionId}")
    public ResponseEntity<?> consultarEstado(@PathVariable String clientTransactionId) {
        try {
            EstadoTransaccionDTO estado = pagoServicio.consultarEstado(clientTransactionId);
            return ResponseEntity.ok(estado);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion consultar estado {}", e.getMessage());
            return construirError(HttpStatus.NOT_FOUND, "transaccion_no_encontrada", e.getMessage());
        } catch (Exception e) {
            log.error("Fallo al consultar estado", e);
            return construirError(HttpStatus.INTERNAL_SERVER_ERROR, "error_consultar_estado", e.getMessage());
        }
    }

    /**
     * Cancela una transaccion pendiente.
     *
     * @param clientTransactionId identificador interno
     * @return resultado de la cancelacion
     */
    @PostMapping("/cancelar/{clientTransactionId}")
    public ResponseEntity<?> cancelar(@PathVariable String clientTransactionId) {
        try {
            AccionTransaccionResponseDTO respuesta = pagoServicio.cancelar(clientTransactionId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion cancelar {}", e.getMessage());
            return construirError(HttpStatus.BAD_REQUEST, "transaccion_no_cancelable", e.getMessage());
        } catch (Exception e) {
            log.error("Fallo al cancelar transaccion", e);
            return construirError(HttpStatus.INTERNAL_SERVER_ERROR, "error_cancelar_transaccion", e.getMessage());
        }
    }

    /**
     * Reversa una transaccion aprobada.
     *
     * @param clientTransactionId identificador interno
     * @return resultado del reverso
     */
    @PostMapping("/reversar/{clientTransactionId}")
    public ResponseEntity<?> reversar(@PathVariable String clientTransactionId) {
        try {
            AccionTransaccionResponseDTO respuesta = pagoServicio.reversar(clientTransactionId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            log.warn("Validacion reversar {}", e.getMessage());
            return construirError(HttpStatus.BAD_REQUEST, "transaccion_no_reversible", e.getMessage());
        } catch (Exception e) {
            log.error("Fallo al reversar transaccion", e);
            return construirError(HttpStatus.INTERNAL_SERVER_ERROR, "error_reversar_transaccion", e.getMessage());
        }
    }

    private ResponseEntity<RespuestaErrorDTO> construirError(HttpStatus status, String codigo, String detalle) {
        RespuestaErrorDTO errorDTO = new RespuestaErrorDTO(codigo, "operacion no completada", detalle);
        return ResponseEntity.status(status).body(errorDTO);
    }


    @PostMapping("/callback")
    public ResponseEntity<String> recibirCallback(@RequestBody Map<String, Object> data) {
        log.info("🔔 Callback recibido de PayPhone: {}", data);

        String estado = (String) data.get("status");
        String idCliente = data.get("clientTransactionId").toString();

        // Ejemplo: actualizar estado en tu BD
        // pagoRepository.actualizarEstado(idCliente, estado);

        return ResponseEntity.ok("OK");
    }


    @PostMapping("/link")
    public ResponseEntity<?> generarLinkPago(@RequestBody IdRequestDTO id) {
        try {
            log.info("Generando link de pago para ID: {}", id.getId());
            var link = pagoServicio.crearLinkPago(id.getId());
            return ResponseEntity.ok(link);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}

