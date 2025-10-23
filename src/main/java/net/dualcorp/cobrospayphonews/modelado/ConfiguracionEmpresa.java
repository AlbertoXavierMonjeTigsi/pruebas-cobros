package net.dualcorp.cobrospayphonews.modelado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dualcorp.cobrospayphonews.modelado.enums.AmbienteEnum;

/**
 * Entidad para la configuracion de acceso a PayPhone por empresa.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "configuracion_empresa")
public class ConfiguracionEmpresa extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion_empresa")
    private Long idConfiguracionEmpresa;

    @Column(name = "axe_codigo", nullable = false, unique = true)
    private Long axeCodigo;

    // Token que se usa para autenticar las peticiones a PayPhone
    @Column(name = "token_payphone", nullable = false, length = 1024)
    private String tokenPayphone;

    // ID o Identificador del comercio (storeId)
    @Column(name = "store_id", nullable = false, length = 120)
    private String storeId;

    // ID del cliente (Client ID)
    @Column(name = "client_id", nullable = false, length = 120)
    private String clientId;

    // Clave secreta asociada al Client ID
    @Column(name = "client_secret", nullable = false, length = 255)
    private String clientSecret;

    // Contraseña de codificacion (opcional, segun el tipo de autenticacion)
    @Column(name = "encoding_password", length = 255)
    private String encodingPassword;

    // Ambiente: PRODUCCION o PRUEBAS
    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    private AmbienteEnum ambiente;

    // URL base de la API de PayPhone (varia entre sandbox y produccion)
    @Column(name = "url_base_api", nullable = false, length = 255)
    private String urlBaseApi;

    // Tiempo maximo de espera para peticiones
    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    // Indica si la configuracion esta activa
    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado;
}
