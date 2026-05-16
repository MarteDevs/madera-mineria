# Plan Opción A — Completar para Evaluación Final (EF)
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Unidades:** 2 y 3 del sílabo  
> **Objetivo:** Sistema completo dockerizado con resiliencia y observabilidad  
> **Tiempo estimado total:** 3 días

---

## Resumen de cambios

| Paso | Qué se implementa | Microservicios afectados | Tiempo |
|---|---|---|---|
| 1 | Resilience4J Circuit Breaker | ms-pedidos, ms-entrega | 1 día |
| 2 | Spring Boot Actuator | Los 5 microservicios | 2 horas |
| 3 | Dockerfile | Los 6 microservicios | 3 horas |
| 4 | docker-compose.yml | Proyecto raíz | 2 horas |

---

# PASO 1 — Resilience4J Circuit Breaker

## ¿Qué es y para qué sirve?

Resilience4J protege el sistema cuando un microservicio falla. Sin él, si ms-inventario se cae, ms-pedidos también falla en cascada. Con el Circuit Breaker, ms-pedidos detecta el fallo y activa un **fallback** — una respuesta alternativa que mantiene el sistema funcionando.

```
ms-pedidos llama a ms-inventario
        │
        ▼
¿ms-inventario responde?
        │
   ┌────┴────┐
   NO        SÍ
   │         │
   ▼         ▼
Circuit    Respuesta
Breaker    normal
ABIERTO
   │
   ▼
Fallback: "Servicio de inventario
no disponible temporalmente"
```

### Estados del Circuit Breaker

```
CLOSED (normal)  →  muchos fallos  →  OPEN (bloquea llamadas)
                                           │
                                    tiempo de espera
                                           │
                                           ▼
                                    HALF_OPEN (prueba)
                                           │
                                    ┌──────┴──────┐
                                  FALLA          ÉXITO
                                    │              │
                                    ▼              ▼
                                  OPEN          CLOSED
```

---

## 1.1 Cambios en ms-pedidos

### Agregar dependencia en `pom.xml`

```xml
<!-- Resilience4J -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- AOP — requerido por Resilience4J -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Agregar configuración en `application.properties`

```properties
# ── RESILIENCE4J — Circuit Breaker ───────────────────────────────────────────

# Configuración para la llamada a ms-inventario
resilience4j.circuitbreaker.instances.inventario.sliding-window-size=5
resilience4j.circuitbreaker.instances.inventario.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.inventario.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.inventario.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.inventario.automatic-transition-from-open-to-half-open-enabled=true

# Configuración del Retry
resilience4j.retry.instances.inventario.max-attempts=3
resilience4j.retry.instances.inventario.wait-duration=2s

# Configuración del TimeLimiter (timeout)
resilience4j.timelimiter.instances.inventario.timeout-duration=3s
```

> **Explicación de los valores:**
> - `sliding-window-size=5` → analiza las últimas 5 llamadas
> - `failure-rate-threshold=50` → si el 50% fallan, abre el circuito
> - `wait-duration-in-open-state=10s` → espera 10 segundos antes de probar de nuevo
> - `max-attempts=3` → reintenta 3 veces antes de fallar

### Modificar `InventarioClient.java`

Reemplazar el fallback básico por uno con Resilience4J completo:

```java
@FeignClient(
    name = "ms-inventario",
    url = "http://localhost:8082"
    // Quitar el fallback del @FeignClient
    // Resilience4J lo maneja con @CircuitBreaker
)
public interface InventarioClient {

    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable Long id);

    @PutMapping("/api/inventario/{id}/reducir")
    void reducirStock(@PathVariable Long id,
                      @RequestParam Integer cantidad);
}
```

### Modificar `PedidoServiceImpl.java`

Agregar `@CircuitBreaker` en los métodos que llaman a ms-inventario:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository    pedidoRepo;
    private final InventarioClient    inventarioClient;
    private final AmqpTemplate        amqpTemplate;

    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackCrearPedido")
    @Retry(name = "inventario")
    public PedidoResponse crearPedido(PedidoRequest request) {

        Integer stock = inventarioClient.consultarStock(request.getMaderaId());

        if (stock < request.getCantidadSolicitada()) {
            throw new StockInsuficienteException(
                "Stock disponible: " + stock +
                ", solicitado: " + request.getCantidadSolicitada());
        }

        Pedido pedido = new Pedido();
        pedido.setMaderaId(request.getMaderaId());
        pedido.setTipoMadera(request.getTipoMadera());
        pedido.setCantidadSolicitada(request.getCantidadSolicitada());
        pedido.setMina(request.getMina());
        pedido.setAreaSolicitante(request.getAreaSolicitante());
        pedido.setSolicitadoPor(request.getSolicitadoPor());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFechaEntregaEstimada(LocalDateTime.now().plusDays(3));
        pedidoRepo.save(pedido);

        PedidoEvent evento = new PedidoEvent(
            pedido.getId(), pedido.getTipoMadera(),
            pedido.getCantidadSolicitada(), pedido.getMina(),
            "CREADO", LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            evento);

        return mapToResponse(pedido);
    }

    // ─── FALLBACK — se ejecuta cuando el Circuit Breaker está ABIERTO ────────
    public PedidoResponse fallbackCrearPedido(
            PedidoRequest request, Exception ex) {

        log.error("Circuit Breaker activo para ms-inventario. " +
            "Causa: {}", ex.getMessage());

        throw new RuntimeException(
            "El servicio de inventario no está disponible temporalmente. " +
            "Por favor intente en unos minutos.");
    }

    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackAprobarPedido")
    public PedidoResponse aprobarPedido(Long id, String aprobadoPor) {

        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException(
                "Pedido no encontrado: " + id));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        inventarioClient.reducirStock(
            pedido.getMaderaId(),
            pedido.getCantidadSolicitada());

        pedido.setEstado(EstadoPedido.APROBADO);
        pedido.setAprobadoPor(aprobadoPor);
        pedido.setFechaAprobacion(LocalDateTime.now());
        pedidoRepo.save(pedido);

        return mapToResponse(pedido);
    }

    // ─── FALLBACK para aprobar ───────────────────────────────────────────────
    public PedidoResponse fallbackAprobarPedido(Long id,
            String aprobadoPor, Exception ex) {

        log.error("No se pudo reducir stock. Circuit Breaker activo. " +
            "Pedido #{}", id);

        throw new RuntimeException(
            "No se pudo comunicar con el servicio de inventario. " +
            "El pedido no fue aprobado.");
    }
}
```

---

## 1.2 Cambios en ms-entrega

### Agregar dependencia en `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Agregar configuración en `application.properties`

```properties
# ── RESILIENCE4J ─────────────────────────────────────────────────────────────
resilience4j.circuitbreaker.instances.pedidos.sliding-window-size=5
resilience4j.circuitbreaker.instances.pedidos.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.pedidos.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.pedidos.automatic-transition-from-open-to-half-open-enabled=true

resilience4j.retry.instances.pedidos.max-attempts=3
resilience4j.retry.instances.pedidos.wait-duration=2s
```

### Modificar `EntregaServiceImpl.java`

Agregar `@CircuitBreaker` en el método `confirmarRecepcion`:

```java
@Transactional
@CircuitBreaker(name = "pedidos", fallbackMethod = "fallbackConfirmarRecepcion")
public EntregaResponse confirmarRecepcion(Long id,
        ConfirmarRecepcionRequest request) {

    Entrega entrega = obtenerEntidadPorId(id);

    if (entrega.getEstado() != EstadoEntrega.EN_RUTA) {
        throw new IllegalStateException(
            "Solo se puede confirmar recepción de entregas EN_RUTA");
    }

    entrega.setEstado(EstadoEntrega.ENTREGADO);
    entrega.setFechaEntregaReal(LocalDateTime.now());
    entrega.setRecibidoPor(request.getRecibidoPor());
    entrega.setConformidad(request.getConformidad());
    entrega.setObservaciones(request.getObservaciones());
    entregaRepo.save(entrega);

    // Notificar a ms-pedidos (protegido por Circuit Breaker)
    pedidosClient.actualizarEstadoPedido(
        entrega.getPedidoId(), "ENTREGADO");

    return mapToResponse(entrega);
}

// ─── FALLBACK ────────────────────────────────────────────────────────────────
public EntregaResponse fallbackConfirmarRecepcion(Long id,
        ConfirmarRecepcionRequest request, Exception ex) {

    log.error("No se pudo notificar a ms-pedidos. " +
        "Entrega #{} marcada como entregada localmente.", id);

    // La entrega SÍ se guarda como entregada
    // ms-pedidos se actualizará manualmente o en un retry posterior
    Entrega entrega = obtenerEntidadPorId(id);
    return mapToResponse(entrega);
}
```

---

# PASO 2 — Spring Boot Actuator en todos los microservicios

Actuator expone endpoints de salud y métricas que Prometheus usará para el monitoreo.

## Agregar en cada microservicio

### Dependencia en `pom.xml` (los 5 microservicios)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer — expone métricas en formato Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Agregar en `application.properties` de cada microservicio

```properties
# ── ACTUATOR ─────────────────────────────────────────────────────────────────
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.info.env.enabled=true

# Info del microservicio
info.app.name=${spring.application.name}
info.app.version=1.0.0
info.app.description=Microservicio del sistema de madera para mineria
```

### Endpoints disponibles por microservicio

| Endpoint | Descripción |
|---|---|
| `/actuator/health` | Estado del servicio y BD |
| `/actuator/info` | Información del microservicio |
| `/actuator/metrics` | Lista de métricas disponibles |
| `/actuator/prometheus` | Métricas en formato Prometheus |

### Verificación por microservicio

```
GET http://localhost:8082/actuator/health  → ms-inventario
GET http://localhost:8083/actuator/health  → ms-pedidos
GET http://localhost:8084/actuator/health  → ms-notificaciones
GET http://localhost:8085/actuator/health  → ms-auth
GET http://localhost:8086/actuator/health  → ms-entrega
```

**Respuesta esperada en cada uno:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

# PASO 3 — Dockerfile para cada microservicio

Cada microservicio necesita su propio `Dockerfile` en la raíz de su carpeta.

## Dockerfile estándar (igual para todos)

```dockerfile
# ── Etapa 1: Compilar ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos de Maven
COPY pom.xml .
COPY src ./src

# Compilar sin ejecutar tests
RUN ./mvnw package -DskipTests

# ── Etapa 2: Imagen final ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Puerto del microservicio
EXPOSE 8082

# Comando de inicio
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Dockerfile por microservicio

Crea el archivo `Dockerfile` en la raíz de cada carpeta. Solo cambia el `EXPOSE`:

### ms-inventario/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ms-pedidos/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ms-notificaciones/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ms-auth/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ms-entrega/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### api-gateway/Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# PASO 4 — docker-compose.yml

Crea este archivo en la **raíz del proyecto** (fuera de todas las carpetas de microservicios).

## Estructura esperada del proyecto

```
madera-mineria/
├── docker-compose.yml          ← aquí
├── ms-inventario/
│   └── Dockerfile
├── ms-pedidos/
│   └── Dockerfile
├── ms-notificaciones/
│   └── Dockerfile
├── ms-auth/
│   └── Dockerfile
├── ms-entrega/
│   └── Dockerfile
└── api-gateway/
    └── Dockerfile
```

## `docker-compose.yml` completo

```yaml
version: '3.8'

services:

  # ── INFRAESTRUCTURA ─────────────────────────────────────────────────────────

  postgres:
    image: postgres:15-alpine
    container_name: postgres-madera
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    networks:
      - madera-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: rabbitmq-madera
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    networks:
      - madera-network
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ── MICROSERVICIOS ───────────────────────────────────────────────────────────

  ms-auth:
    build: ./ms-auth
    container_name: ms-auth
    ports:
      - "8085:8085"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      JWT_SECRET: madera-mineria-clave-secreta-2026-cibertec
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - madera-network

  ms-inventario:
    build: ./ms-inventario
    container_name: ms-inventario
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/inventario_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - madera-network

  ms-pedidos:
    build: ./ms-pedidos
    container_name: ms-pedidos
    ports:
      - "8083:8083"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pedidos_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      SPRING_RABBITMQ_HOST: rabbitmq
      INVENTARIO_URL: http://ms-inventario:8082
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      ms-inventario:
        condition: service_started
    networks:
      - madera-network

  ms-notificaciones:
    build: ./ms-notificaciones
    container_name: ms-notificaciones
    ports:
      - "8084:8084"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notificaciones_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - madera-network

  ms-entrega:
    build: ./ms-entrega
    container_name: ms-entrega
    ports:
      - "8086:8086"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/entrega_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      SPRING_RABBITMQ_HOST: rabbitmq
      PEDIDOS_URL: http://ms-pedidos:8083
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      ms-pedidos:
        condition: service_started
    networks:
      - madera-network

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: madera-mineria-clave-secreta-2026-cibertec
      MS_AUTH_URL: http://ms-auth:8085
      MS_INVENTARIO_URL: http://ms-inventario:8082
      MS_PEDIDOS_URL: http://ms-pedidos:8083
      MS_NOTIFICACIONES_URL: http://ms-notificaciones:8084
      MS_ENTREGA_URL: http://ms-entrega:8086
    depends_on:
      - ms-auth
      - ms-inventario
      - ms-pedidos
      - ms-notificaciones
      - ms-entrega
    networks:
      - madera-network

# ── VOLÚMENES Y REDES ─────────────────────────────────────────────────────────

volumes:
  postgres_data:

networks:
  madera-network:
    driver: bridge
```

## `init-db.sql` — crear todas las bases de datos automáticamente

Crea este archivo en la raíz del proyecto junto al `docker-compose.yml`:

```sql
-- Crear todas las bases de datos del sistema
CREATE DATABASE auth_db;
CREATE DATABASE inventario_db;
CREATE DATABASE pedidos_db;
CREATE DATABASE notificaciones_db;
CREATE DATABASE entrega_db;
```

---

## Ajuste en `application.properties` de cada microservicio para Docker

Cuando corren en Docker, los microservicios no se llaman por `localhost` sino por el **nombre del contenedor**. Agrega estas variables en cada `application.properties`:

### ms-pedidos — cambiar URL de Feign

```properties
# En local usa localhost, en Docker usa el nombre del contenedor
inventario.url=http://localhost:8082
```

Y en `InventarioClient.java` usar la variable:

```java
@FeignClient(name = "ms-inventario", url = "${inventario.url}")
public interface InventarioClient { ... }
```

### ms-entrega — cambiar URL de Feign

```properties
pedidos.url=http://localhost:8083
```

```java
@FeignClient(name = "ms-pedidos", url = "${pedidos.url}")
public interface PedidosClient { ... }
```

### api-gateway — cambiar URLs de rutas

```properties
# En Docker las URLs cambian a nombres de contenedor
ms.auth.url=http://localhost:8085
ms.inventario.url=http://localhost:8082
ms.pedidos.url=http://localhost:8083
ms.notificaciones.url=http://localhost:8084
ms.entrega.url=http://localhost:8086
```

Y en `application.properties` del gateway usar las variables:

```properties
spring.cloud.gateway.routes[0].uri=${ms.auth.url}
spring.cloud.gateway.routes[1].uri=${ms.inventario.url}
spring.cloud.gateway.routes[2].uri=${ms.pedidos.url}
spring.cloud.gateway.routes[3].uri=${ms.notificaciones.url}
spring.cloud.gateway.routes[4].uri=${ms.entrega.url}
```

---

## Comandos Docker esenciales

```bash
# Construir e iniciar todo el sistema
docker-compose up --build

# Iniciar en segundo plano
docker-compose up --build -d

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f ms-pedidos

# Ver estado de todos los contenedores
docker-compose ps

# Parar todo sin borrar datos
docker-compose stop

# Parar y borrar contenedores (conserva volúmenes)
docker-compose down

# Parar, borrar contenedores Y borrar datos
docker-compose down -v

# Reconstruir solo un microservicio
docker-compose up --build ms-pedidos

# Ver métricas de recursos de los contenedores
docker stats
```

---

## Pasos de implementación del plan completo

### Día 1 — Resilience4J

#### Mañana — ms-pedidos (2 horas)
1. Agregar dependencias al `pom.xml`
2. Agregar configuración en `application.properties`
3. Modificar `InventarioClient` — quitar fallback del `@FeignClient`
4. Modificar `PedidoServiceImpl` — agregar `@CircuitBreaker` y `@Retry`
5. Probar: apagar ms-inventario y crear un pedido → debe devolver mensaje claro de error

#### Tarde — ms-entrega (1.5 horas)
1. Agregar dependencias al `pom.xml`
2. Agregar configuración en `application.properties`
3. Modificar `EntregaServiceImpl` — agregar `@CircuitBreaker` en confirmarRecepcion
4. Probar: apagar ms-pedidos y confirmar recepción → debe guardar entrega localmente

---

### Día 2 — Actuator + Dockerfiles

#### Mañana — Actuator en los 5 microservicios (2 horas)
Para cada microservicio (ms-inventario, ms-pedidos, ms-notificaciones, ms-auth, ms-entrega):
1. Agregar dependencias `actuator` y `micrometer-registry-prometheus`
2. Agregar configuración en `application.properties`
3. Verificar `GET /actuator/health` devuelve `{"status":"UP"}`

#### Tarde — Dockerfiles (3 horas)
1. Crear `Dockerfile` en cada carpeta de microservicio
2. Verificar que el `mvnw` tiene permisos de ejecución
3. Probar que cada Dockerfile compila: `docker build -t ms-inventario ./ms-inventario`

---

### Día 3 — docker-compose

#### Mañana (3 horas)
1. Crear `init-db.sql` en la raíz del proyecto
2. Crear `docker-compose.yml` completo
3. Ajustar URLs de Feign y gateway para usar variables de entorno
4. Ejecutar `docker-compose up --build`
5. Verificar que todos los contenedores quedan en estado `Up`

#### Tarde — pruebas finales (2 horas)
1. Login desde Postman usando `http://localhost:8080/api/auth/login`
2. Crear pedido completo a través del gateway
3. Aprobar, despachar y confirmar entrega
4. Verificar `docker-compose ps` — todos en `Up`

---

## Checklist final — Opción A completa

### Resilience4J
- [ ] ms-pedidos tiene `@CircuitBreaker` en `crearPedido` y `aprobarPedido`
- [ ] ms-pedidos tiene `@Retry` configurado
- [ ] ms-entrega tiene `@CircuitBreaker` en `confirmarRecepcion`
- [ ] Al apagar ms-inventario, ms-pedidos responde con mensaje claro (no error 500)
- [ ] Al apagar ms-pedidos, ms-entrega guarda la entrega localmente

### Actuator
- [ ] `GET /actuator/health` responde `UP` en los 5 microservicios
- [ ] `GET /actuator/metrics` lista las métricas disponibles
- [ ] `GET /actuator/prometheus` devuelve métricas en formato Prometheus

### Docker
- [ ] Dockerfile existe en cada microservicio
- [ ] `init-db.sql` existe en la raíz con las 5 bases de datos
- [ ] `docker-compose.yml` existe en la raíz
- [ ] `docker-compose up --build` levanta todos los contenedores sin errores
- [ ] `docker-compose ps` muestra todos los servicios en estado `Up`
- [ ] Login funciona a través del gateway dockerizado
- [ ] Flujo completo de pedido funciona con Docker

---

## Cobertura del sílabo con Opción A

| Tema del sílabo | Estado |
|---|---|
| Feign Client | ✅ Implementado |
| RabbitMQ / Kafka | ✅ Implementado |
| Spring Cloud Gateway | ✅ Implementado |
| JWT + OAuth2 + Spring Security | ✅ Implementado |
| **Resilience4J Circuit Breaker** | ✅ Opción A |
| **Docker** | ✅ Opción A |
| **Spring Boot Actuator** | ✅ Opción A |
| Kubernetes | ⬜ Opcional |
| Prometheus + Grafana | ⬜ Opcional |
| ELK Stack | ⬜ Opcional |

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
