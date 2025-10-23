package net.dualcorp.cobrospayphonews.dao;

import java.time.LocalDateTime;
import java.util.Optional;
import net.dualcorp.cobrospayphonews.modelado.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * DAO para la entidad TransaccionPago.
 */
@Repository
public interface TransaccionPagoDao extends JpaRepository<TransaccionPago, Long> {

    /**
     * Recupera transaccion por clientTransactionId y axeCodigo.
     *
     * @param clientTransactionId identificador cliente
     * @param axeCodigo empresa
     * @return opcion de transaccion
     */
    Optional<TransaccionPago> findByClientTransactionIdAndAxeCodigo(String clientTransactionId, Long axeCodigo);

    /**
     * Verifica existencia de transaccion duplicada.
     *
     * @param clientTransactionId identificador cliente
     * @param axeCodigo empresa
     * @return verdadero si ya existe
     */
    boolean existsByClientTransactionIdAndAxeCodigo(String clientTransactionId, Long axeCodigo);

    /**
     * Cuenta consultas recientes para control de limites por empresa.
     *
     * @param axeCodigo empresa
     * @param limite tiempo limite
     * @return cantidad de consultas
     */
    @Query("SELECT COUNT(t) FROM TransaccionPago t WHERE t.axeCodigo = :axeCodigo AND t.fechaActualizacion >= :limite")
    long contarConsultasRecientes(@Param("axeCodigo") Long axeCodigo, @Param("limite") LocalDateTime limite);
}

