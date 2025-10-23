package net.dualcorp.cobrospayphonews.modelado.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dualcorp.cobrospayphonews.modelado.enums.EstadoTransaccionEnum;

/**
 * DTO que representa la respuesta despues de cancelar o reversar.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccionTransaccionResponseDTO {

    private String clientTransactionId;
    private EstadoTransaccionEnum estado;
    private String mensaje;
}

