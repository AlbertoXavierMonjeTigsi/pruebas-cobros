package net.dualcorp.cobrospayphonews.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dualcorp.cobrospayphonews.modelado.ConfiguracionEmpresa;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para interactuar con PayPhone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayphoneClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Ejecuta un POST hacia PayPhone.
     *
     * @param configuracion configuracion de la empresa
     * @param path camino relativo
     * @param cuerpo cuerpo de la peticion
     * @return respuesta como JsonNode
     * @throws IllegalStateException si falla la comunicacion
     */
    public JsonNode ejecutarPost(ConfiguracionEmpresa configuracion, String path, JsonNode cuerpo) {
        try {
            RestTemplate restTemplate = construirRestTemplate(configuracion);
            HttpHeaders headers = construirHeaders(configuracion);
            HttpEntity<String> entidad = new HttpEntity<>(cuerpo.toString(), headers);
            String url = construirUrl(configuracion, path);
            ResponseEntity<String> respuesta = restTemplate.exchange(url, HttpMethod.POST, entidad, String.class);
            return objectMapper.readTree(respuesta.getBody());
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP PayPhone {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException(ex.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error inesperado al llamar PayPhone", e);
            throw new IllegalStateException("error de comunicacion PayPhone");
        }
    }

    /**
     * Ejecuta un GET hacia PayPhone.
     *
     * @param configuracion configuracion de la empresa
     * @param path camino relativo
     * @return respuesta como JsonNode
     */
    public JsonNode ejecutarGet(ConfiguracionEmpresa configuracion, String path) {
        try {
            RestTemplate restTemplate = construirRestTemplate(configuracion);
            HttpHeaders headers = construirHeaders(configuracion);
            HttpEntity<Void> entidad = new HttpEntity<>(headers);
            String url = construirUrl(configuracion, path);
            ResponseEntity<String> respuesta = restTemplate.exchange(url, HttpMethod.GET, entidad, String.class);
            return objectMapper.readTree(respuesta.getBody());
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP PayPhone {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException(ex.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error inesperado al consultar PayPhone", e);
            throw new IllegalStateException("error de comunicacion PayPhone");
        }
    }

    private RestTemplate construirRestTemplate(ConfiguracionEmpresa configuracion) {
        return restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(configuracion.getTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(configuracion.getTimeoutMs()))
            .build();
    }

    private HttpHeaders construirHeaders(ConfiguracionEmpresa configuracion) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + configuracion.getTokenPayphone());
        headers.set(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
        return headers;
    }

    private String construirUrl(ConfiguracionEmpresa configuracion, String path) {
        String base = configuracion.getUrlBaseApi();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }
}
