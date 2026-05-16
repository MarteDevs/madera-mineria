package com.madera.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // Rutas que NO requieren token
    private static final List<String> RUTAS_PUBLICAS = List.of(
        "/api/auth/login",
        "/api/auth/register"
    );

    public boolean esRutaPublica(String path) {
        return RUTAS_PUBLICAS.stream()
            .anyMatch(path::startsWith);
    }

    public boolean esTokenValido(String token) {
        try {
            extraerTodosLosClaims(token);
            return !esTokenExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extraerUsername(String token) {
        return extraerTodosLosClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        Claims claims = extraerTodosLosClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?> lista && !lista.isEmpty()) {
            return lista.get(0).toString();
        }
        return "";
    }

    private boolean esTokenExpirado(String token) {
        return extraerTodosLosClaims(token)
            .getExpiration()
            .before(new Date());
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(
                Keys.hmacShaKeyFor(
                    Base64.getEncoder()
                        .encodeToString(secret.getBytes())
                        .getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
