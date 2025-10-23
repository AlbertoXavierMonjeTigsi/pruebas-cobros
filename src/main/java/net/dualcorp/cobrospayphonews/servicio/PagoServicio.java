package net.dualcorp.cobrospayphonews.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dualcorp.cobrospayphonews.cliente.PayphoneClient;
import net.dualcorp.cobrospayphonews.dao.ConfiguracionEmpresaDao;
import net.dualcorp.cobrospayphonews.dao.TransaccionPagoDao;
import net.dualcorp.cobrospayphonews.modelado.ConfiguracionEmpresa;
import net.dualcorp.cobrospayphonews.modelado.TransaccionPago;
import net.dualcorp.cobrospayphonews.modelado.dtos.AccionTransaccionResponseDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.CobroRequestDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.CobroResponseDTO;
import net.dualcorp.cobrospayphonews.modelado.dtos.EstadoTransaccionDTO;
import net.dualcorp.cobrospayphonews.modelado.enums.EstadoTransaccionEnum;
import net.dualcorp.cobrospayphonews.util.AxeCodigoProveedorUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;



/**
 * Servicio que orquesta el flujo de cobros PayPhone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicio {

    private static final int LIMITE_CONSULTAS_MINUTO = 30;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConfiguracionEmpresaDao configuracionEmpresaDao;
    private final TransaccionPagoDao transaccionPagoDao;
    private final PayphoneClient payphoneClient;
    private final ObjectMapper objectMapper;

    /**
     * Genera un cobro en PayPhone.
     *
     * @param dto datos del cobro
     * @return respuesta con identificador interno
     */
    @Transactional
    public CobroResponseDTO generarCobro(CobroRequestDTO dto) {
        TransaccionPago transaccion = null;
        try {
            Long axeCodigo = AxeCodigoProveedorUtil.obtenerAxeCodigo();
            ConfiguracionEmpresa configuracion = obtenerConfiguracion(axeCodigo);
            String clientTransactionId = UUID.randomUUID().toString();
            int service = dto.getService() == null ? 0 : dto.getService();
            int tip = dto.getTip() == null ? 0 : dto.getTip();
            int montoCalculado = dto.getAmountWithoutTax() + dto.getAmountWithTax() + dto.getTax() + service + tip;
            int monto = dto.getAmount() == null ? montoCalculado : dto.getAmount();
            if (monto != montoCalculado) {
                log.info("Ajuste de amount recibido {} a calculado {}", dto.getAmount(), montoCalculado);
                monto = montoCalculado;
            }

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("phoneNumber", dto.getPhoneNumber());
            requestNode.put("countryCode", dto.getCountryCode());
            requestNode.put("amount", monto);
            requestNode.put("amountWithoutTax", dto.getAmountWithoutTax());
            requestNode.put("amountWithTax", dto.getAmountWithTax());
            requestNode.put("tax", dto.getTax());
            requestNode.put("service", service);
            requestNode.put("tip", tip);
            requestNode.put("reference", dto.getReference());
            requestNode.put("clientTransactionId", clientTransactionId);
            requestNode.put("storeId", configuracion.getStoreId());
            requestNode.set("order", objectMapper.valueToTree(dto.getOrder()));
            requestNode.put("responseUrl", dto.getResponseUrl());

            transaccion = construirTransaccionPendiente(dto, axeCodigo, clientTransactionId, configuracion, monto, service, tip, requestNode);
            log.info("Registrando venta PayPhone clientTransactionId {} axeCodigo {}", clientTransactionId, axeCodigo);
            JsonNode respuestaPayphone = payphoneClient.ejecutarPost(configuracion, "/api/Sale", requestNode);
            actualizarTransaccionConRespuesta(transaccion, respuestaPayphone, EstadoTransaccionEnum.PENDIENTE);
            transaccionPagoDao.save(transaccion);
            String transactionId = respuestaPayphone.path("transactionId").asText(null);
            return new CobroResponseDTO(
                clientTransactionId,
                EstadoTransaccionEnum.PENDIENTE,
                "cobro registrado",
                transactionId,
                transaccion.getIdTransaccion(),
                transaccion.getNumeroDocumento(),
                transaccion.getIdCobro()
            );
        } catch (Exception e) {
            log.error("Error al generar cobro", e);
            if (transaccion != null) {
                transaccion.setEstado(EstadoTransaccionEnum.ERROR);
                transaccion.setMensajeRespuesta(e.getMessage());
                transaccion.setFechaActualizacion(LocalDateTime.now());
                transaccionPagoDao.save(transaccion);
            }
            throw new IllegalStateException("no fue posible generar cobro");
        }
    }

    /**
     * Consulta el estado de una transaccion.
     *
     * @param clientTransactionId identificador interno
     * @return dto con estado actualizado
     */
    @Transactional
    public EstadoTransaccionDTO consultarEstado(String clientTransactionId) {
        try {
            Long axeCodigo = AxeCodigoProveedorUtil.obtenerAxeCodigo();
            TransaccionPago transaccion = transaccionPagoDao.findByClientTransactionIdAndAxeCodigo(clientTransactionId, axeCodigo)
                .orElseThrow(() -> new IllegalArgumentException("transaccion no encontrada"));

            if (esEstadoFinal(transaccion.getEstado())) {
                return mapearEstado(transaccion);
            }

            LocalDateTime minutoAtras = LocalDateTime.now().minusMinutes(1);
            long llamadasRecientes = transaccionPagoDao.contarConsultasRecientes(axeCodigo, minutoAtras);
            if (llamadasRecientes >= LIMITE_CONSULTAS_MINUTO) {
                throw new IllegalStateException("limite de consultas alcanzado");
            }

            ConfiguracionEmpresa configuracion = obtenerConfiguracion(axeCodigo);
            String path = construirPathConsulta(transaccion);
            JsonNode respuestaPayphone = payphoneClient.ejecutarGet(configuracion, path);
            EstadoTransaccionEnum estado = mapearEstadoDesdePayphone(respuestaPayphone.path("statusCode").asInt());
            actualizarTransaccionConRespuesta(transaccion, respuestaPayphone, estado);
            transaccionPagoDao.save(transaccion);
            return mapearEstado(transaccion);
        } catch (IllegalArgumentException e) {
            log.warn("Consulta estado invalida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error al consultar estado", e);
            throw new IllegalStateException("no fue posible consultar estado");
        }
    }

    /**
     * Cancela una transaccion pendiente.
     *
     * @param clientTransactionId identificador interno
     * @return resultado de la cancelacion
     */
    @Transactional
    public AccionTransaccionResponseDTO cancelar(String clientTransactionId) {
        try {
            Long axeCodigo = AxeCodigoProveedorUtil.obtenerAxeCodigo();
            TransaccionPago transaccion = transaccionPagoDao.findByClientTransactionIdAndAxeCodigo(clientTransactionId, axeCodigo)
                .orElseThrow(() -> new IllegalArgumentException("transaccion no encontrada"));

            if (transaccion.getEstado() != EstadoTransaccionEnum.PENDIENTE) {
                throw new IllegalArgumentException("transaccion no puede cancelarse");
            }

            ConfiguracionEmpresa configuracion = obtenerConfiguracion(axeCodigo);
            String transactionId = obtenerTransactionId(transaccion);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("transactionId", transactionId);
            JsonNode respuesta = payphoneClient.ejecutarPost(configuracion, "/api/Sale/" + transactionId + "/cancel", body);
            actualizarTransaccionConRespuesta(transaccion, respuesta, EstadoTransaccionEnum.CANCELADO);
            transaccionPagoDao.save(transaccion);
            log.info("Transaccion {} cancelada para axeCodigo {}", clientTransactionId, axeCodigo);
            return new AccionTransaccionResponseDTO(clientTransactionId, EstadoTransaccionEnum.CANCELADO, "transaccion cancelada");
        } catch (IllegalArgumentException e) {
            log.warn("Cancelacion invalida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error al cancelar transaccion", e);
            throw new IllegalStateException("no fue posible cancelar transaccion");
        }
    }

    /**
     * Reversa una transaccion aprobada.
     *
     * @param clientTransactionId identificador interno
     * @return resultado de reverso
     */
    @Transactional
    public AccionTransaccionResponseDTO reversar(String clientTransactionId) {
        try {
            Long axeCodigo = AxeCodigoProveedorUtil.obtenerAxeCodigo();
            TransaccionPago transaccion = transaccionPagoDao.findByClientTransactionIdAndAxeCodigo(clientTransactionId, axeCodigo)
                .orElseThrow(() -> new IllegalArgumentException("transaccion no encontrada"));

            if (transaccion.getEstado() != EstadoTransaccionEnum.APROBADO) {
                throw new IllegalArgumentException("transaccion no puede reversarse");
            }

            ConfiguracionEmpresa configuracion = obtenerConfiguracion(axeCodigo);
            String transactionId = obtenerTransactionId(transaccion);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("transactionId", transactionId);
            JsonNode respuesta = payphoneClient.ejecutarPost(configuracion, "/api/Sale/" + transactionId + "/reverse", body);
            actualizarTransaccionConRespuesta(transaccion, respuesta, EstadoTransaccionEnum.REVERSO);
            transaccionPagoDao.save(transaccion);
            log.info("Transaccion {} reversada para axeCodigo {}", clientTransactionId, axeCodigo);
            return new AccionTransaccionResponseDTO(clientTransactionId, EstadoTransaccionEnum.REVERSO, "transaccion reversada");
        } catch (IllegalArgumentException e) {
            log.warn("Reverso invalido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error al reversar transaccion", e);
            throw new IllegalStateException("no fue posible reversar transaccion");
        }
    }

    private ConfiguracionEmpresa obtenerConfiguracion(Long axeCodigo) {
        log.info("Obteniendo configuracion para axeCodigo {}", axeCodigo);
        return configuracionEmpresaDao.findByAxeCodigoAndHabilitadoTrue(axeCodigo)
            .orElseThrow(() -> new IllegalArgumentException("empresa sin configuracion activa"));
    }

    private TransaccionPago construirTransaccionPendiente(
        CobroRequestDTO dto,
        Long axeCodigo,
        String clientTransactionId,
        ConfiguracionEmpresa configuracion,
        int monto,
        int service,
        int tip,
        ObjectNode requestNode
    ) {
        TransaccionPago transaccion = new TransaccionPago();
        transaccion.setClientTransactionId(clientTransactionId);
        transaccion.setAxeCodigo(axeCodigo);
        transaccion.setPhoneNumber(dto.getPhoneNumber());
        transaccion.setCountryCode(dto.getCountryCode());
        transaccion.setAmount(monto);
        transaccion.setAmountWithoutTax(dto.getAmountWithoutTax());
        transaccion.setAmountWithTax(dto.getAmountWithTax());
        transaccion.setTax(dto.getTax());
        transaccion.setService(service);
        transaccion.setTip(tip);
        transaccion.setReference(dto.getReference());
        transaccion.setResponseUrl(dto.getResponseUrl());
        transaccion.setIdTransaccion(dto.getIdTransaccion());
        transaccion.setNumeroDocumento(dto.getNumeroDocumento());
        transaccion.setIdCobro(dto.getIdCobro());
        transaccion.setEstado(EstadoTransaccionEnum.PENDIENTE);
        transaccion.setStoreId(configuracion.getStoreId());
        transaccion.setDetallesRequest(requestNode.toString());
        transaccion.setFechaSolicitud(LocalDateTime.now());
        transaccion.setFechaActualizacion(LocalDateTime.now());
        return transaccionPagoDao.save(transaccion);
    }

    private void actualizarTransaccionConRespuesta(TransaccionPago transaccion, JsonNode respuesta, EstadoTransaccionEnum estado) {
        transaccion.setDetallesResponse(respuesta.toString());
        transaccion.setFechaActualizacion(LocalDateTime.now());
        transaccion.setEstado(estado);
        transaccion.setMensajeRespuesta(respuesta.path("message").asText(null));
        if (respuesta.hasNonNull("authorizationCode")) {
            transaccion.setAuthorizationCode(respuesta.path("authorizationCode").asText());
        }
        if (respuesta.hasNonNull("transactionId")) {
            transaccion.setTransactionIdPayphone(respuesta.path("transactionId").asText());
        } else if (respuesta.hasNonNull("transactionIdPayphone")) {
            transaccion.setTransactionIdPayphone(respuesta.path("transactionIdPayphone").asText());
        }
    }

    private boolean esEstadoFinal(EstadoTransaccionEnum estado) {
        return estado == EstadoTransaccionEnum.APROBADO
            || estado == EstadoTransaccionEnum.RECHAZADO
            || estado == EstadoTransaccionEnum.CANCELADO
            || estado == EstadoTransaccionEnum.REVERSO
            || estado == EstadoTransaccionEnum.ERROR;
    }

    private String construirPathConsulta(TransaccionPago transaccion) {
        if (transaccion.getTransactionIdPayphone() != null) {
            return "/api/Sale/" + transaccion.getTransactionIdPayphone();
        }
        return "/api/Sale/client/" + transaccion.getClientTransactionId();
    }

    private EstadoTransaccionEnum mapearEstadoDesdePayphone(int statusCode) {
        return switch (statusCode) {
            case 1 -> EstadoTransaccionEnum.PENDIENTE;
            case 2 -> EstadoTransaccionEnum.RECHAZADO;
            case 3 -> EstadoTransaccionEnum.APROBADO;
            default -> EstadoTransaccionEnum.ERROR;
        };
    }

    private EstadoTransaccionDTO mapearEstado(TransaccionPago transaccion) {
        return new EstadoTransaccionDTO(
            transaccion.getClientTransactionId(),
            transaccion.getTransactionIdPayphone(),
            transaccion.getEstado(),
            transaccion.getMensajeRespuesta(),
            transaccion.getAuthorizationCode(),
            transaccion.getIdTransaccion(),
            transaccion.getNumeroDocumento(),
            transaccion.getIdCobro(),
            transaccion.getFechaSolicitud(),
            transaccion.getFechaActualizacion()
        );
    }

    private String obtenerTransactionId(TransaccionPago transaccion) {
        if (transaccion.getTransactionIdPayphone() == null) {
            throw new IllegalArgumentException("transaccion sin identificador de payphone");
        }
        return transaccion.getTransactionIdPayphone();
    }


    public String crearLinkPago(Long axeCodigo) {
        String token = "rTOaurrKB2YMFymXLD9SANbA9aLuBjBocG0NAWBiAm1BInY7wfYNBZIcOcXLsKCwfWwvg1dlfqe87v20LXaNCQBzDLf2aBfGvpozcEl_fF2iQK5iaotmMRso1MdzVnuRASAfYUt2KfDCymEI5J15f8W4JJ4I5njldFl1hV-Dh53hxAZQKbKtCsNZn4bboPW4LzYtpwTjHb_6k9LFgkcsFs9MttQFy5C1ZBd2ks2tlBrnFa4rQRNqHDoWirymePED3RQfgufyNZw9UfWkehpstKAMDovp0wY8-ot2ZGk4UkB7RPR_g8FK5ktu7er7Sv0Jnu-4nQ";
        String url = "https://pay.payphonetodoesposible.com/api/Links";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        Map<String, Object> body = Map.of(
                "amount", 120,
                "amountWithTax", 100,
                "tax", 20,
                "clientTransactionId", "idpruebas-009",
                "currency", "USD",
                "storeId", "1639395f-5773-4769-a5c5-84ba94637fc8",
                "reference", "Pago con API Link",
                "callbackUrl", "https://distinguishingly-uncognizant-carolyn.ngrok-free.dev/cobros-payphone/api/v1/pagos/callback"
        );
    
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
    
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody(); // Aquí ya es el link directamente
            } else {
                throw new RuntimeException("Error al crear link de pago: " + response.getStatusCode()
                        + " - " + response.getBody());
            }
    
        } catch (Exception e) {
            throw new RuntimeException("Excepción al crear link de pago: " + e.getMessage(), e);
        }
    }
    
    }
