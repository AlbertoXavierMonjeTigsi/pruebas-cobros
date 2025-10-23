package net.dualcorp.cobrospayphonews;

import net.dualcorp.cobrospayphonews.seguridad.JwtTokenService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


/**
 * Aplicacion principal para cobros-payphone-ws.
 */
@SpringBootApplication
public class NetDualcorpCobrosPayphoneWsApplication {

    /**
     * Punto de arranque de la aplicacion.
     *
     * @param args argumentos de linea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(NetDualcorpCobrosPayphoneWsApplication.class, args);
    }

     @Bean
    public ApplicationRunner init(JwtTokenService jwtTokenService) {
        return args -> {
            // Generar token de ejemplo para la empresa con axeCodigo=1001, expira en 1 hora
             jwtTokenService.generar();
            
        };
    }
}

