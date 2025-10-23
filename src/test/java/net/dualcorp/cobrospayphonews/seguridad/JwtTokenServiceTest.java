package net.dualcorp.cobrospayphonews.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias para JwtTokenService.
 */
class JwtTokenServiceTest {

    private static final String CLAVE = "clave-prueba";
    private final JwtTokenService jwtTokenService = new JwtTokenService(new ObjectMapper(), CLAVE);

    @Test
    void extraerAxeCodigoCuandoTokenValido() {
        String token = construirToken("{\"axe_codigo\":123}", CLAVE);
        Long axeCodigo = jwtTokenService.extraerAxeCodigo(token);
        assertEquals(123L, axeCodigo);
    }

    @Test
    void extraerAxeCodigoCuandoTokenSinCampo() {
        String token = construirToken("{\"otro\":1}", CLAVE);
        assertThrows(IllegalArgumentException.class, () -> jwtTokenService.extraerAxeCodigo(token));
    }

    @Test
    void extraerAxeCodigoCuandoFirmaInvalida() {
        String token = construirToken("{\"axe_codigo\":123}", "otra-clave");
        assertThrows(IllegalArgumentException.class, () -> jwtTokenService.extraerAxeCodigo(token));
    }

    private String construirToken(String payloadJson, String claveFirmado) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String firma = firmar(header + "." + payload, claveFirmado);
        return header + "." + payload + "." + firma;
    }

    private String firmar(String data, String claveFirmado) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(claveFirmado.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] firmaBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firmaBytes);
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo firmar token de prueba", e);
        }
    }
}
