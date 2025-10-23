package net.dualcorp.cobrospayphonews.util;

import net.dualcorp.cobrospayphonews.seguridad.AxeCodigoContext;

/**
 * Utilitario para recuperar el axeCodigo desde el contexto.
 */
public final class AxeCodigoProveedorUtil {

    private AxeCodigoProveedorUtil() {
        // utilitario
    }

    /**
     * Recupera el axeCodigo del contexto.
     *
     * @return axeCodigo
     * @throws IllegalStateException si no existe en contexto
     */
    public static Long obtenerAxeCodigo() {
        Long axeCodigo = AxeCodigoContext.obtener();
        if (axeCodigo == null) {
            throw new IllegalStateException("axeCodigo no disponible en contexto");
        }
        return axeCodigo;
    }
}

