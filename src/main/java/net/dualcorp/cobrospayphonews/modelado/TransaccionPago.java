package net.dualcorp.cobrospayphonews.modelado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dualcorp.cobrospayphonews.modelado.enums.EstadoTransaccionEnum;

/**
 * Entidad que guarda el detalle completo de las transacciones PayPhone.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transaccion_pago")
public class TransaccionPago extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion_pago")
    private Long idTransaccionPago;

    @Column(name = "client_transaction_id", nullable = false, length = 120, unique = true)
    private String clientTransactionId;

    @Column(name = "axe_codigo", nullable = false)
    private Long axeCodigo;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "amount_without_tax", nullable = false)
    private Integer amountWithoutTax;

    @Column(name = "amount_with_tax", nullable = false)
    private Integer amountWithTax;

    @Column(name = "tax", nullable = false)
    private Integer tax;

    @Column(name = "service", nullable = false)
    private Integer service;

    @Column(name = "tip", nullable = false)
    private Integer tip;

    @Column(name = "reference", nullable = false, length = 255)
    private String reference;

    @Column(name = "response_url", nullable = false, length = 255)
    private String responseUrl;

    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    @Column(name = "id_cobro")
    private Long idCobro;

    @Column(name = "store_id", length = 120)
    private String storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoTransaccionEnum estado;

    @Column(name = "transaction_id_payphone", length = 120)
    private String transactionIdPayphone;

    @Column(name = "authorization_code", length = 120)
    private String authorizationCode;

    @Column(name = "mensaje_respuesta", length = 1000)
    private String mensajeRespuesta;

    @Lob
    @Column(name = "detalles_request")
    private String detallesRequest;

    @Lob
    @Column(name = "detalles_response")
    private String detallesResponse;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Ajusta la fecha de auditoria antes de insertar.
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaSolicitud = ahora;
        this.fechaActualizacion = ahora;
    }

    /**
     * Actualiza fecha de auditoria previo a actualizar.
     */
    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
