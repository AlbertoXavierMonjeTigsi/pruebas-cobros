package net.dualcorp.cobrospayphonews.modelado.dtos;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dualcorp.cobrospayphonews.modelado.enums.EstadoTransaccionEnum;

/**
 * DTO que resume el estado de una transaccion PayPhone.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoTransaccionDTO {

    private String clientTransactionId;
    private String transactionIdPayphone;
    private EstadoTransaccionEnum estado;
    private String mensaje;
    private String authorizationCode;
    private Long idTransaccion;
    private String numeroDocumento;
    private Long idCobro;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaActualizacion;
}
