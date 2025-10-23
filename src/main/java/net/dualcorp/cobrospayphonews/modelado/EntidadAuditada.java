package net.dualcorp.cobrospayphonews.modelado;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Base para entidades con auditoria simple.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class EntidadAuditada {

    @Basic(optional = false)
    @NotNull
    @Column(name = "registro_eliminado", nullable = false)
    private Boolean registroEliminado = Boolean.FALSE;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "usuario_crea", nullable = false, length = 100, updatable = false)
    private String usuarioCrea = "sistema";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_crea", nullable = false, updatable = false)
    private LocalDateTime fechaCrea;

    @Size(max = 100)
    @Column(name = "usuario_modifica", length = 100)
    private String usuarioModifica;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "fecha_modifica")
    private LocalDateTime fechaModifica;

    /**
     * Calcula la fecha de creacion antes de persistir.
     */
    @PrePersist
    protected void onCreate() {
        if (fechaCreaisNull()) {
            fechaCrea = LocalDateTime.now();
        }
    }

    /**
     * Calcula la fecha de modificacion antes de actualizar.
     */
    @PreUpdate
    protected void onUpdate() {
        fechaModifica = LocalDateTime.now();
    }

    private boolean fechaCreaisNull() {
        return fechaCrea == null;
    }
}

