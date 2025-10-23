package net.dualcorp.cobrospayphonews.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * Servicio que valida la firma del token y expone el campo axe_codigo.
 */
@Slf4j
@Service
public class JwtTokenService {

    private static final String ALG_ESPERADO = "HS256";

    private final ObjectMapper objectMapper;
    private final String claveToken;

    public JwtTokenService(ObjectMapper objectMapper, @Value("${seguridad.token.clave}") String claveToken) {
        this.objectMapper = objectMapper;
        this.claveToken = claveToken;
    }

    /**
     * Obtiene el axeCodigo del token si existe.
     *
     * @param token token JWT sin prefijo
     * @return axeCodigo
     * @throws IllegalArgumentException cuando el token es invalido
     */
    public Long extraerAxeCodigo(String token) {
        if (claveToken == null || claveToken.isBlank()) {
            throw new IllegalStateException("clave de token no configurada");
        }
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                throw new IllegalArgumentException("token no cumple estructura JWT");
            }
            String headerJson = decodificarBase64(partes[0]);
            JsonNode header = objectMapper.readTree(headerJson);
            String algoritmo = header.path("alg").asText();
            if (!ALG_ESPERADO.equalsIgnoreCase(algoritmo)) {
                throw new IllegalArgumentException("algoritmo no soportado");
            }

            validarFirma(partes[0], partes[1], partes[2]);

            String payloadJson = decodificarBase64(partes[1]);
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode campo = payload.get("axe_codigo");
            if (campo == null || campo.isNull()) {
                throw new IllegalArgumentException("token sin axe_codigo");
            }
            if (campo.isNumber()) {
                return campo.longValue();
            }
            if (campo.isTextual()) {
                return Long.parseLong(campo.asText());
            }
            throw new IllegalArgumentException("axe_codigo con formato invalido");
        } catch (IllegalArgumentException e) {
            log.warn("Token sin axeCodigo valido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Fallo al validar token", e);
            throw new IllegalArgumentException("token invalido");
        }
    }

    private void validarFirma(String header, String payload, String firmaToken) throws Exception {
        String data = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(claveToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] firmaCalculada = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String firmaBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(firmaCalculada);
        if (!MessageDigest.isEqual(firmaBase64.getBytes(StandardCharsets.UTF_8), firmaToken.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("token firma invalida");
        }
    }

    private String decodificarBase64(String valor) {
        byte[] bytes = Base64.getUrlDecoder().decode(valor);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void generar(){
        JwtTokenService jwtService = new JwtTokenService(new ObjectMapper(), "miClaveSecretaParaJwtDePruebasToken");
        String token = jwtService.generarToken(1L, 36000); // axeCodigo=1001, expira en 1 hora
        System.out.println(token);
        
        
    }

    /**
     * Genera un token JWT con axe_codigo en el payload.
     *
     * @param axeCodigo código de la empresa
     * @param expiracionSegundos tiempo de expiración en segundos desde ahora
     * @return token JWT como String
     */
    public String generarToken(Long axeCodigo, long expiracionSegundos) {
        try {
            // Header
            ObjectNode header = objectMapper.createObjectNode();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String headerBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(header));

            // Payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("axe_codigo", axeCodigo);
            long ahora = System.currentTimeMillis() / 1000;
            payload.put("iat", ahora);
            payload.put("exp", ahora + expiracionSegundos);
            String payloadBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));

            // Firma
            String data = headerBase64 + "." + payloadBase64;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(claveToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] firma = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String firmaBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(firma);

            // Token completo
            return headerBase64 + "." + payloadBase64 + "." + firmaBase64;

        } catch (Exception e) {
            throw new IllegalStateException("Error generando token JWT", e);
        }
    }
}
