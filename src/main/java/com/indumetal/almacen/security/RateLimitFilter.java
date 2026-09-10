package com.indumetal.almacen.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indumetal.almacen.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de peticiones (rate limiting) SOLO para los endpoints sensibles de
 * autenticacion (login y el flujo de recuperacion de contrasena), para
 * dificultar ataques de fuerza bruta / enumeracion de correos, incluso
 * contra distintas cuentas desde la misma IP.
 *
 * No es un rate limit global de toda la API (eso normalmente se resuelve a
 * nivel de infraestructura / API Gateway en produccion); aqui se protegen
 * puntualmente las rutas mas atacadas.
 *
 * Implementacion "ventana fija" en memoria (sin Redis ni librerias externas):
 * para cada combinacion IP + ruta se cuenta cuantas peticiones llegaron
 * dentro de la ventana de tiempo configurada; al superar el limite, se
 * responde 429 hasta que la ventana se reinicia. Suficiente para una sola
 * instancia (como este proyecto); si en el futuro se despliega con varias
 * instancias, este contador tendria que moverse a un almacen compartido
 * (ej. Redis) porque cada instancia tendria su propio mapa en memoria.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final int capacidad;
    private final Duration ventana;
    private final List<String> rutasProtegidas;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ConcurrentHashMap<String, Contador> contadores = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.capacidad:10}") int capacidad,
            @Value("${app.rate-limit.ventana-segundos:60}") long ventanaSegundos,
            @Value("${app.rate-limit.rutas:/api/auth/login,/api/auth/forgot-password,/api/auth/verify-reset-code,/api/auth/reset-password}")
            List<String> rutasProtegidas) {
        this.capacidad = capacidad;
        this.ventana = Duration.ofSeconds(ventanaSegundos);
        this.rutasProtegidas = rutasProtegidas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return rutasProtegidas.stream().noneMatch(patron -> pathMatcher.match(patron, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clave = obtenerIp(request) + ":" + request.getRequestURI();
        Contador contador = contadores.computeIfAbsent(clave, k -> new Contador());

        if (contador.excedioLimite(capacidad, ventana)) {
            log.warn("Rate limit excedido para {} en {}", obtenerIp(request), request.getRequestURI());
            responderDemasiadasPeticiones(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private String obtenerIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void responderDemasiadasPeticiones(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("Demasiados intentos. Por favor espera unos minutos antes de volver a intentarlo.")));
    }

    /** Contador de ventana fija: se reinicia solo apenas pasa el tiempo de la ventana. */
    private static class Contador {
        private volatile Instant inicioVentana = Instant.now();
        private final AtomicInteger cantidad = new AtomicInteger(0);

        synchronized boolean excedioLimite(int capacidad, Duration ventana) {
            if (Instant.now().isAfter(inicioVentana.plus(ventana))) {
                inicioVentana = Instant.now();
                cantidad.set(0);
            }
            return cantidad.incrementAndGet() > capacidad;
        }
    }
}