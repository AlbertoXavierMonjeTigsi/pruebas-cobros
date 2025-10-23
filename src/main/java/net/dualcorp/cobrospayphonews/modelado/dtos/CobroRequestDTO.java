package net.dualcorp.cobrospayphonews.modelado.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO que representa el cuerpo para generar cobros PayPhone.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CobroRequestDTO {

    @NotBlank(message = "phoneNumber es obligatorio")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "phoneNumber invalido")
    private String phoneNumber;

    @NotBlank(message = "countryCode es obligatorio")
    @Pattern(regexp = "^[0-9]{1,5}$", message = "countryCode invalido")
    private String countryCode;

    @NotNull(message = "amount es obligatorio")
    @Min(value = 0, message = "amount no puede ser negativo")
    private Integer amount;

    @NotNull(message = "amountWithoutTax es obligatorio")
    @Min(value = 0, message = "amountWithoutTax no puede ser negativo")
    private Integer amountWithoutTax;

    @NotNull(message = "amountWithTax es obligatorio")
    @Min(value = 0, message = "amountWithTax no puede ser negativo")
    private Integer amountWithTax;

    @NotNull(message = "tax es obligatorio")
    @Min(value = 0, message = "tax no puede ser negativo")
    private Integer tax;

    @Min(value = 0, message = "service no puede ser negativo")
    private Integer service = 0;

    @Min(value = 0, message = "tip no puede ser negativo")
    private Integer tip = 0;

    @NotBlank(message = "reference es obligatoria")
    @Size(max = 255, message = "reference excede longitud permitida")
    private String reference;

    @jakarta.validation.constraints.Positive(message = "idTransaccion debe ser positivo")
    private Long idTransaccion;

    @Size(max = 50, message = "numeroDocumento supera longitud maxima")
    private String numeroDocumento;

    @jakarta.validation.constraints.Positive(message = "idCobro debe ser positivo")
    private Long idCobro;

    @Valid
    @NotNull(message = "order es obligatorio")
    private CobroRequestOrderDTO order;

    @NotBlank(message = "responseUrl es obligatoria")
    @Size(max = 255, message = "responseUrl supera longitud maxima")
    private String responseUrl;

    /**
     * DTO para el bloque de orden PayPhone.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CobroRequestOrderDTO {

        @Valid
        @NotNull(message = "billTo es obligatorio")
        private CobroRequestBillToDTO billTo;

        @Valid
        @NotNull(message = "lineItems es obligatorio")
        @Size(min = 1, message = "lineItems requiere al menos un item")
        private List<CobroRequestLineItemDTO> lineItems;
    }

    /**
     * DTO para informacion de facturacion.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CobroRequestBillToDTO {

        @NotBlank(message = "documentId es obligatorio")
        private String documentId;

        @NotBlank(message = "email es obligatorio")
        private String email;

        @NotBlank(message = "name es obligatorio")
        private String name;
    }

    /**
     * DTO que representa un item dentro de lineItems.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CobroRequestLineItemDTO {

        @NotBlank(message = "item es obligatorio")
        private String item;

        @NotNull(message = "quantity es obligatorio")
        @Min(value = 1, message = "quantity no puede ser menor a 1")
        private Integer quantity;

        @NotNull(message = "amount es obligatorio")
        @Min(value = 0, message = "amount no puede ser negativo")
        private Integer amount;
    }
}
