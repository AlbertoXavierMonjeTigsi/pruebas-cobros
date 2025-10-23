package net.dualcorp.cobrospayphonews.dao;

import java.util.Optional;
import net.dualcorp.cobrospayphonews.modelado.ConfiguracionEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO para recuperar configuraciones de acceso a PayPhone.
 */
@Repository
public interface ConfiguracionEmpresaDao extends JpaRepository<ConfiguracionEmpresa, Long> {

    /**
     * Busca la configuracion habilitada por axeCodigo.
     *
     * @param axeCodigo identificador empresa
     * @return opcional de configuracion
     */
    Optional<ConfiguracionEmpresa> findByAxeCodigoAndHabilitadoTrue(Long axeCodigo);
}

