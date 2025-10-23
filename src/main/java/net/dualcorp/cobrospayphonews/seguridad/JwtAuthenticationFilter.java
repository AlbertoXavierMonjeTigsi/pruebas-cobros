package net.dualcorp.cobrospayphonews.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que valida token y expone axeCodigo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (cabecera == null || cabecera.isBlank() || !cabecera.startsWith(PREFIJO)) {
                lanzarNoAutorizado(response, "token ausente");
                return;
            }
            String token = cabecera.substring(PREFIJO.length());
            Long axeCodigo = jwtTokenService.extraerAxeCodigo(token);
            if (axeCodigo == null) {
                lanzarNoAutorizado(response, "token sin axe_codigo");
                return;
            }
            AxeCodigoContext.establecer(axeCodigo);
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(axeCodigo, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            lanzarNoAutorizado(response, e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
            AxeCodigoContext.limpiar();
        }
    }

    private void lanzarNoAutorizado(HttpServletResponse response, String detalle) throws IOException {
        log.warn("Acceso no autorizado: {}", detalle);
        if (!response.isCommitted()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, String> error = Map.of(
                "codigo", "401",
                "mensaje", "autenticacion requerida",
                "detalle", detalle
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }
}
