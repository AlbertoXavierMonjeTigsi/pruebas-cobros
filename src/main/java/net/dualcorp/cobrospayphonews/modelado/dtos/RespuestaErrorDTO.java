package net.dualcorp.cobrospayphonews.modelado.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO generico para devolver errores uniformes.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RespuestaErrorDTO {

    private String codigo;
    private String mensaje;
    private String detalle;
}

