package com.madera.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.debug("Request entrante: {} {}", request.getMethod(), path);

        // Dejar pasar rutas públicas sin validar token
        if (jwtUtil.esRutaPublica(path)) {
            log.debug("Ruta pública, sin validación: {}", path);
            return chain.filter(exchange);
        }

        // Verificar que existe el header Authorization
        String authHeader = request.getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Request sin token: {}", path);
            return respuestaNoAutorizado(exchange,
                "Token requerido. Incluir: Authorization: Bearer <token>");
        }

        // Extraer y validar el token
        String token = authHeader.substring(7);

        if (!jwtUtil.esTokenValido(token)) {
            log.warn("Token inválido o expirado para ruta: {}", path);
            return respuestaNoAutorizado(exchange,
                "Token inválido o expirado");
        }

        // Token válido — agregar datos del usuario al header
        // para que los microservicios puedan leerlos
        String username = jwtUtil.extraerUsername(token);
        String rol      = jwtUtil.extraerRol(token);

        log.debug("Token válido. Usuario: {}, Rol: {}, Ruta: {}",
            username, rol, path);

        ServerHttpRequest requestModificado = request.mutate()
            .header("X-Usuario", username)
            .header("X-Rol", rol)
            .build();

        return chain.filter(
            exchange.mutate().request(requestModificado).build());
    }

    @Override
    public int getOrder() {
        return -1; // Ejecutar antes que cualquier otro filtro
    }

    private Mono<Void> respuestaNoAutorizado(
            ServerWebExchange exchange, String mensaje) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(
            HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format(
            "{\"error\":\"No autorizado\",\"mensaje\":\"%s\"}", mensaje);

        DataBuffer buffer = response.bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
