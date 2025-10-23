package net.dualcorp.cobrospayphonews.seguridad;

/**
 * Contexto ThreadLocal para exponer el axeCodigo autenticado.
 */
public final class AxeCodigoContext {

    private static final ThreadLocal<Long> CONTEXTO = new ThreadLocal<>();

    private AxeCodigoContext() {
        // ocultar constructor
    }

    /**
     * Almacena el axeCodigo en el contexto.
     *
     * @param axeCodigo identificador de empresa
     */
    public static void establecer(Long axeCodigo) {
        CONTEXTO.set(axeCodigo);
    }

    /**
     * Obtiene el axeCodigo actual.
     *
     * @return axeCodigo o null si no existe
     */
    public static Long obtener() {
        return CONTEXTO.get();
    }

    /**
     * Limpia el contexto para evitar fugas.
     */
    public static void limpiar() {
        CONTEXTO.remove();
    }
}

