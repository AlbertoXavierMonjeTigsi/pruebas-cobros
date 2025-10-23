package net.dualcorp.cobrospayphonews.modelado.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dualcorp.cobrospayphonews.modelado.enums.EstadoTransaccionEnum;

/**
 * DTO de respuesta para la generacion de cobro.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CobroResponseDTO {

    private String clientTransactionId;
    private EstadoTransaccionEnum estado;
    private String mensaje;
    private String transactionIdPayphone;
    private Long idTransaccion;
    private String numeroDocumento;
    private Long idCobro;
}
