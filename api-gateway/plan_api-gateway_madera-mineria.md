# Plan Técnico — api-gateway
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** api-gateway · Puerto `8080`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Sin base de datos** — solo enruta y valida tokens  
> **Unidad del sílabo:** Unidad 2 — Seguridad en Microservicios

---

## ¿Qué hace este microservicio?

Es la **puerta de entrada única** al sistema. Ningún cliente llama directamente a ms-inventario, ms-pedidos ni ningún otro servicio — todo pasa por el gateway. Su trabajo es validar el token JWT y enrutar la request al microservicio correcto.

| Responsabilidad | Descripción |
|---|---|
| Punto de entrada único | Todo el tráfico entra por el puerto `8080` |
| Validar token JWT | Si no hay token válido, devuelve `401` antes de tocar cualquier microservicio |
| Enrutar requests | Redirige cada path al microservicio correspondiente |
| Rutas públicas | `/api/auth/login` y `/api/auth/register` no requieren token |
| Logging | Registra cada request que pasa por el gateway |
| Actuator | Expone health checks del sistema |

---

## Cómo funciona el enrutamiento

```
Cliente hace request a http://localhost:8080/api/pedidos
                │
                ▼
        api-gateway recibe la request
                │
                ▼
        ¿Tiene token JWT válido?
                │
         ┌──────┴──────┐
         NO             SÍ
         │              │
         ▼              ▼
     401             Enruta a ms-pedidos
  Unauthorized      http://localhost:8083/api/pedidos
                         │
                         ▼
                  ms-pedidos responde
                         │
                         ▼
                  gateway devuelve
                  respuesta al cliente
```

---

## Mapa de rutas del sistema

| Request al gateway (8080) | Enruta a | Microservicio |
|---|---|---|
| `/api/auth/**` | `http://localhost:8085` | ms-auth |
| `/api/inventario/**` | `http://localhost:8082` | ms-inventario |
| `/api/pedidos/**` | `http://localhost:8083` | ms-pedidos |
| `/api/notificaciones/**` | `http://localhost:8084` | ms-notificaciones |
| `/api/entregas/**` | `http://localhost:8086` | ms-entrega |

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| **Spring Cloud Gateway** | Motor de enrutamiento de requests |
| **Spring Boot Actuator** | Health checks en `/actuator/health` |
| **Lombok** | Clases auxiliares |
| **jjwt 0.11.5** | Validar tokens JWT (misma librería que ms-auth) |

> El gateway usa servidor **Netty** (reactivo), no Tomcat. Por eso en consola verás "Netty started on port 8080" en vez de "Tomcat started".

---

## Estructura de archivos

```
api-gateway/src/main/java/com/madera/gateway/
│
├── ApiGatewayApplication.java
│
├── security/
│   ├── JwtUtil.java                     ← valida tokens JWT (crear PRIMERO)
│   └── JwtAuthenticationFilter.java     ← filtro global del gateway
│
├── config/
│   └── GatewayConfig.java               ← rutas programáticas (opcional)
│
└── exception/
    └── GatewayExceptionHandler.java     ← manejo de errores del gateway

src/main/resources/
└── application.properties               ← rutas declarativas (principal)
```

> Este microservicio tiene muy pocos archivos — su configuración principal está en `application.properties`.

---

## Configuración completa

### `application.properties`

```properties
spring.application.name=api-gateway
server.port=8080

# ── RUTAS ────────────────────────────────────────────────────────────────────

# ms-auth — rutas públicas (login y register no requieren token)
spring.cloud.gateway.routes[0].id=ms-auth
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**

# ms-inventario
spring.cloud.gateway.routes[1].id=ms-inventario
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/inventario/**

# ms-pedidos
spring.cloud.gateway.routes[2].id=ms-pedidos
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/pedidos/**

# ms-notificaciones
spring.cloud.gateway.routes[3].id=ms-notificaciones
spring.cloud.gateway.routes[3].uri=http://localhost:8084
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/notificaciones/**

# ms-entrega
spring.cloud.gateway.routes[4].id=ms-entrega
spring.cloud.gateway.routes[4].uri=http://localhost:8086
spring.cloud.gateway.routes[4].predicates[0]=Path=/api/entregas/**

# ── ACTUATOR ─────────────────────────────────────────────────────────────────
management.endpoints.web.exposure.include=health,info,gateway
management.endpoint.gateway.enabled=true
management.endpoint.health.show-details=always

# ── JWT ───────────────────────────────────────────────────────────────────────
# Debe ser la misma clave que en ms-auth
jwt.secret=madera-mineria-clave-secreta-2026-cibertec

# ── CORS ─────────────────────────────────────────────────────────────────────
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins=*
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-headers=*

# ── LOGGING ──────────────────────────────────────────────────────────────────
logging.level.org.springframework.cloud.gateway=DEBUG
```

---

## Dependencias JWT en `pom.xml`

Agregar manualmente igual que en ms-auth:

```xml
<!-- JWT — misma versión que ms-auth -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

---

## Seguridad JWT

### `JwtUtil.java`

```java
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
```

### `JwtAuthenticationFilter.java`

```java
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
```

---

## Manejo de errores del gateway

### `GatewayExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GatewayExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "error", "Error del gateway",
            "mensaje", ex.getReason() != null
                ? ex.getReason()
                : "Error procesando la request"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Error en gateway: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of(
            "error", "Error interno del gateway",
            "mensaje", ex.getMessage()
        ));
    }
}
```

---

## Pasos de implementación

### Paso 1 — Agregar dependencias JWT al `pom.xml` ⏱ 5 min

Las 3 dependencias jjwt igual que en ms-auth. Sin esto el filtro no compila.

**Verificación:** Maven descarga las dependencias sin errores.

---

### Paso 2 — Configurar `application.properties` ⏱ 10 min

Copiar la configuración completa de arriba. Verificar que los puertos coincidan con los de cada microservicio.

**Verificación:** La app arranca y en consola aparece `Netty started on port 8080`.

---

### Paso 3 — Crear `JwtUtil.java` ⏱ 20 min

Clase que valida tokens y extrae datos. La clave secreta debe ser idéntica a la de ms-auth — si difiere, todos los tokens serán inválidos.

**Verificación:** Compila sin errores.

---

### Paso 4 — Crear `JwtAuthenticationFilter.java` ⏱ 25 min

Filtro global que intercepta cada request. Implementa `GlobalFilter` y `Ordered`. El orden `-1` garantiza que se ejecuta primero que cualquier otro filtro del gateway.

**Verificación:** Al hacer cualquier request sin token a `http://localhost:8080/api/pedidos` devuelve `401`.

---

### Paso 5 — Crear `GatewayExceptionHandler.java` ⏱ 10 min

Manejo de errores del gateway para respuestas JSON consistentes.

**Verificación:** Compila sin errores.

---

### Paso 6 — Probar el enrutamiento completo ⏱ 20 min

Verificar que cada ruta llega al microservicio correcto usando el token obtenido desde ms-auth.

**Verificación:** Las 7 pruebas de Postman pasan correctamente.

---

## Pruebas con Postman

> Para estas pruebas necesitas todos los microservicios corriendo: 8082, 8083, 8084, 8085, 8086.

### Prueba 1 — Ruta pública sin token (debe funcionar)

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@madera.com",
  "password": "admin123"
}
```

**Respuesta esperada:** `200 OK` con token JWT. Esta ruta es pública — no necesita token.

> Copia el token de la respuesta para las siguientes pruebas.

---

### Prueba 2 — Request SIN token (debe rechazar)

```
GET http://localhost:8080/api/pedidos
```

**Respuesta esperada:** `401 Unauthorized`

```json
{
  "error": "No autorizado",
  "mensaje": "Token requerido. Incluir: Authorization: Bearer <token>"
}
```

---

### Prueba 3 — Request CON token válido

```
GET http://localhost:8080/api/pedidos
Authorization: Bearer <token-del-login>
```

**Respuesta esperada:** `200 OK` con la lista de pedidos desde ms-pedidos.

---

### Prueba 4 — Token expirado o inválido

```
GET http://localhost:8080/api/inventario
Authorization: Bearer tokeninvalidofalso123
```

**Respuesta esperada:** `401 Unauthorized`

```json
{
  "error": "No autorizado",
  "mensaje": "Token inválido o expirado"
}
```

---

### Prueba 5 — Enrutamiento a ms-inventario

```
GET http://localhost:8080/api/inventario
Authorization: Bearer <token>
```

**Respuesta esperada:** `200 OK` con lista de madera desde ms-inventario (puerto 8082).

---

### Prueba 6 — Enrutamiento a ms-notificaciones

```
GET http://localhost:8080/api/notificaciones/pendientes
Authorization: Bearer <token>
```

**Respuesta esperada:** `200 OK` con notificaciones pendientes desde ms-notificaciones (puerto 8084).

---

### Prueba 7 — Enrutamiento a ms-entrega

```
GET http://localhost:8080/api/entregas
Authorization: Bearer <token>
```

**Respuesta esperada:** `200 OK` con lista de entregas desde ms-entrega (puerto 8086).

---

### Prueba 8 — Health check del gateway

```
GET http://localhost:8080/actuator/health
```

**Respuesta esperada:** `200 OK` — no requiere token, es una ruta de sistema.

```json
{
  "status": "UP"
}
```

---

## Checklist final del sistema completo

### api-gateway
- [ ] Arranca en el puerto `8080` con Netty
- [ ] `POST /api/auth/login` sin token devuelve `200` con JWT
- [ ] `GET /api/pedidos` sin token devuelve `401`
- [ ] `GET /api/pedidos` con token válido devuelve `200` desde ms-pedidos
- [ ] Token inválido devuelve `401` con mensaje claro
- [ ] `GET /actuator/health` devuelve `{"status":"UP"}`
- [ ] Todas las rutas enrutan al microservicio correcto

### Sistema completo
- [ ] ms-inventario corriendo en `8082`
- [ ] ms-pedidos corriendo en `8083`
- [ ] ms-notificaciones corriendo en `8084`
- [ ] ms-auth corriendo en `8085`
- [ ] ms-entrega corriendo en `8086`
- [ ] api-gateway corriendo en `8080`
- [ ] RabbitMQ corriendo en `5672` (panel en `15672`)
- [ ] PostgreSQL con las 5 bases de datos creadas

---

## Flujo completo del sistema — de extremo a extremo

Con los 6 microservicios listos, el flujo completo pasa **todo por el gateway**:

```
PASO 1 — Login
POST http://localhost:8080/api/auth/login
→ gateway → ms-auth → devuelve token JWT

PASO 2 — Crear pedido
POST http://localhost:8080/api/pedidos
Authorization: Bearer <token>
→ gateway valida token → ms-pedidos
→ Feign consulta stock en ms-inventario
→ RabbitMQ notifica a ms-notificaciones

PASO 3 — Aprobar pedido
PUT http://localhost:8080/api/pedidos/1/aprobar
Authorization: Bearer <token>
→ gateway → ms-pedidos
→ Feign reduce stock en ms-inventario
→ RabbitMQ notifica a ms-entrega

PASO 4 — Asignar transportista
PUT http://localhost:8080/api/entregas/1/asignar-transportista
Authorization: Bearer <token>
→ gateway → ms-entrega

PASO 5 — Marcar en ruta
PUT http://localhost:8080/api/entregas/1/en-ruta
Authorization: Bearer <token>
→ gateway → ms-entrega

PASO 6 — Confirmar recepción
PUT http://localhost:8080/api/entregas/1/confirmar-recepcion
Authorization: Bearer <token>
→ gateway → ms-entrega
→ Feign actualiza pedido en ms-pedidos a ENTREGADO
```

---

## Resumen de puertos del sistema

| Microservicio | Puerto | Base de datos |
|---|---|---|
| api-gateway | `8080` | Sin BD |
| ms-inventario | `8082` | inventario_db |
| ms-pedidos | `8083` | pedidos_db |
| ms-notificaciones | `8084` | notificaciones_db |
| ms-auth | `8085` | auth_db |
| ms-entrega | `8086` | entrega_db |
| RabbitMQ | `5672` | Panel: `15672` |
| PostgreSQL | `5432` | — |

---

## Cobertura del sílabo — Evaluación Final (EF)

| Tema del sílabo | Implementado en |
|---|---|
| Feign Client — comunicación sincrónica | ms-pedidos ↔ ms-inventario, ms-entrega ↔ ms-pedidos |
| Mensajería asíncrona con RabbitMQ | ms-pedidos → ms-notificaciones, ms-pedidos → ms-entrega |
| Spring Cloud Gateway | api-gateway — enrutamiento y seguridad |
| JWT y OAuth2 — autenticación | ms-auth genera tokens, api-gateway los valida |
| Spring Security | ms-auth protege endpoints por rol |
| Resilience4J — fallback | InventarioClientFallback, PedidosClientFallback |
| Docker | Dockerfile por microservicio + docker-compose.yml |
| Spring Boot Actuator | api-gateway `/actuator/health` |

---

## Próximo paso — Docker (Unidad 3)

Con el sistema completo funcionando en local, el siguiente paso para la EF es **dockerizar** todos los microservicios con un `docker-compose.yml` que levante todo con un solo comando:

```bash
docker-compose up --build
```

Esto cubre la parte de **Docker y Kubernetes** de la Unidad 2 y la **observabilidad** con Prometheus + Grafana de la Unidad 3.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
