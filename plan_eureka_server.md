# Plan Técnico — Eureka Server
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC
> **Microservicio:** eureka-server · Puerto `8761`
> **Spring Boot:** 3.5.14 · Java 21 · Maven
> **Unidad del sílabo:** Unidad 1 — Spring Cloud

---

## ¿Qué es Eureka y para qué sirve?

Eureka es un **servidor de descubrimiento de servicios** de Netflix, integrado en Spring Cloud. Permite que los microservicios se registren automáticamente y se encuentren entre sí por nombre, sin necesidad de conocer IPs ni puertos fijos.

```
SIN EUREKA (actual)                    CON EUREKA
─────────────────────                  ─────────────────────────────
ms-pedidos llama a:                    ms-pedidos llama a:
http://localhost:8082/api/inventario   http://MS-INVENTARIO/api/inventario
                                                │
                                       Eureka resuelve la IP
                                       automáticamente
```

---

## Cómo funciona el registro

```
1. ms-inventario arranca
        │
        ▼
   Se registra en Eureka (8761)
   "Hola soy MS-INVENTARIO en el puerto 8082"
        │
        ▼
2. ms-pedidos arranca
        │
        ▼
   Se registra en Eureka
   "Hola soy MS-PEDIDOS en el puerto 8083"
        │
        ▼
3. ms-pedidos quiere llamar a ms-inventario
        │
        ▼
   Pregunta a Eureka: "¿Dónde está MS-INVENTARIO?"
        │
        ▼
   Eureka responde: "En localhost:8082"
        │
        ▼
   ms-pedidos hace la llamada correctamente
```

---

## Resumen de cambios por microservicio

| Microservicio | Puerto | Cambios necesarios |
|---|---|---|
| **eureka-server** | `8761` | Crear nuevo microservicio |
| **ms-inventario** | `8082` | Agregar Eureka Client |
| **ms-pedidos** | `8083` | Agregar Eureka Client + cambiar Feign |
| **ms-notificaciones** | `8084` | Agregar Eureka Client |
| **ms-auth** | `8085` | Agregar Eureka Client |
| **ms-entrega** | `8086` | Agregar Eureka Client + cambiar Feign |
| **api-gateway** | `8080` | Agregar Eureka Client + cambiar rutas |
| **docker-compose.yml** | — | Agregar eureka-server |

---

# PARTE 1 — eureka-server (nuevo microservicio)

## Estructura de archivos

```
eureka-server/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/madera/eureka/
    │   └── EurekaServerApplication.java
    └── resources/
        └── application.properties
```

## `EurekaServerApplication.java`

```java
package com.madera.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

## `application.properties`

```properties
spring.application.name=eureka-server
server.port=8761

# Eureka no se registra a sí mismo
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.instance.hostname=localhost

# Desactivar auto-preservación en desarrollo
eureka.server.enable-self-preservation=false
eureka.server.eviction-interval-timer-in-ms=5000

# Logging
logging.level.com.netflix.eureka=WARN
logging.level.com.netflix.discovery=WARN
```

## `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.14</version>
  </parent>

  <groupId>com.madera</groupId>
  <artifactId>eureka-server</artifactId>
  <version>1.0.0</version>

  <properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

## `Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Verificación

Abrir `http://localhost:8761` — debe mostrar el dashboard de Eureka:

```
Instances currently registered with Eureka
─────────────────────────────────────────
(vacío por ahora — se llena cuando arrancan los demás)
```

---

# PARTE 2 — Cambios en los microservicios existentes

## Dependencia a agregar en cada microservicio

Abrir el `pom.xml` de cada microservicio y agregar:

```xml
<!-- Eureka Client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Verificar que el BOM de Spring Cloud ya existe en el `pom.xml`

Si no existe, agregar el `dependencyManagement`:

```xml
<properties>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> Si ya tienes Spring Cloud instalado (Feign, Resilience4J), el BOM ya existe. Solo agrega la dependencia del cliente.

---

## Configuración en `application.properties` de cada microservicio

Agregar estas líneas al final de cada `application.properties`:

```properties
# ── EUREKA CLIENT ─────────────────────────────────────────────────────────────
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Tiempo de heartbeat — cada 10 segundos avisa que sigue vivo
eureka.instance.lease-renewal-interval-in-seconds=10
eureka.instance.lease-expiration-duration-in-seconds=30

# Mostrar IP en vez de hostname (importante para Docker)
eureka.instance.prefer-ip-address=true
```

### Aplicar a estos microservicios uno por uno

- `ms-inventario/src/main/resources/application.properties`
- `ms-pedidos/src/main/resources/application.properties`
- `ms-notificaciones/src/main/resources/application.properties`
- `ms-auth/src/main/resources/application.properties`
- `ms-entrega/src/main/resources/application.properties`
- `api-gateway/src/main/resources/application.properties`

---

# PARTE 3 — Cambios en Feign Clients

Con Eureka, los Feign Clients ya no necesitan la URL fija. Solo usan el nombre del servicio registrado en Eureka.

## ms-pedidos — `InventarioClient.java`

### Antes (sin Eureka)
```java
@FeignClient(
    name = "ms-inventario",
    url = "http://localhost:8082",       // ← quitar esto
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {
    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable Long id);

    @PutMapping("/api/inventario/{id}/reducir")
    void reducirStock(@PathVariable Long id, @RequestParam Integer cantidad);
}
```

### Después (con Eureka)
```java
@FeignClient(
    name = "ms-inventario",              // Eureka resuelve la URL por este nombre
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {
    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable Long id);

    @PutMapping("/api/inventario/{id}/reducir")
    void reducirStock(@PathVariable Long id, @RequestParam Integer cantidad);
}
```

> El `name` debe coincidir exactamente con el `spring.application.name` del microservicio destino.

---

## ms-entrega — `PedidosClient.java`

### Antes (sin Eureka)
```java
@FeignClient(
    name = "ms-pedidos",
    url = "http://localhost:8083",       // ← quitar esto
    fallback = PedidosClientFallback.class
)
public interface PedidosClient {
    @PutMapping("/api/pedidos/{id}/estado")
    void actualizarEstadoPedido(@PathVariable Long id,
                                @RequestParam String nuevoEstado);
}
```

### Después (con Eureka)
```java
@FeignClient(
    name = "ms-pedidos",                 // Eureka resuelve la URL
    fallback = PedidosClientFallback.class
)
public interface PedidosClient {
    @PutMapping("/api/pedidos/{id}/estado")
    void actualizarEstadoPedido(@PathVariable Long id,
                                @RequestParam String nuevoEstado);
}
```

---

# PARTE 4 — Cambios en api-gateway

## `application.properties` del gateway

### Antes (URLs fijas)
```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[3].uri=http://localhost:8084
spring.cloud.gateway.routes[4].uri=http://localhost:8086
```

### Después (con Eureka — usar lb:// para load balancing)
```properties
spring.application.name=api-gateway
server.port=8080

# ── RUTAS CON EUREKA ──────────────────────────────────────────────────────────
# lb:// indica "Load Balancer" — Eureka resuelve la dirección

spring.cloud.gateway.routes[0].id=ms-auth
spring.cloud.gateway.routes[0].uri=lb://ms-auth
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**

spring.cloud.gateway.routes[1].id=ms-inventario
spring.cloud.gateway.routes[1].uri=lb://ms-inventario
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/inventario/**

spring.cloud.gateway.routes[2].id=ms-pedidos
spring.cloud.gateway.routes[2].uri=lb://ms-pedidos
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/pedidos/**

spring.cloud.gateway.routes[3].id=ms-notificaciones
spring.cloud.gateway.routes[3].uri=lb://ms-notificaciones
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/notificaciones/**

spring.cloud.gateway.routes[4].id=ms-entrega
spring.cloud.gateway.routes[4].uri=lb://ms-entrega
spring.cloud.gateway.routes[4].predicates[0]=Path=/api/entregas/**

# ── EUREKA ────────────────────────────────────────────────────────────────────
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true

# ── JWT ───────────────────────────────────────────────────────────────────────
jwt.secret=madera-mineria-clave-secreta-2026-cibertec

# ── ACTUATOR ─────────────────────────────────────────────────────────────────
management.endpoints.web.exposure.include=health,info,gateway
management.endpoint.gateway.enabled=true

# ── CORS ─────────────────────────────────────────────────────────────────────
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins=*
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-headers=*
```

> `lb://` es el prefijo que le dice al gateway que use Eureka para resolver la URL del microservicio con balanceo de carga.

---

# PARTE 5 — Actualizar docker-compose.yml

## Agregar eureka-server como servicio

```yaml
  eureka-server:
    build: ./eureka-server
    container_name: eureka-server
    ports:
      - "8761:8761"
    networks:
      - madera-network
    healthcheck:
      test: ["CMD-SHELL",
             "curl -f http://localhost:8761/actuator/health || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5
```

## Actualizar cada microservicio para esperar a Eureka

Cada microservicio debe tener `depends_on` con eureka-server y la URL de Eureka debe apuntar al contenedor:

```yaml
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
        condition: service_healthy        # ← espera a que Eureka esté listo
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - madera-network

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: madera-mineria-clave-secreta-2026-cibertec
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
      ms-auth:
        condition: service_started
      ms-inventario:
        condition: service_started
      ms-pedidos:
        condition: service_started
      ms-notificaciones:
        condition: service_started
      ms-entrega:
        condition: service_started
    networks:
      - madera-network
```

---

# PARTE 6 — docker-compose.yml final completo con Eureka

```yaml
version: '3.8'

services:

  # ── INFRAESTRUCTURA ──────────────────────────────────────────────────────────

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

  # ── EUREKA SERVER ────────────────────────────────────────────────────────────

  eureka-server:
    build: ./eureka-server
    container_name: eureka-server
    ports:
      - "8761:8761"
    networks:
      - madera-network
    healthcheck:
      test: ["CMD-SHELL",
             "curl -f http://localhost:8761/actuator/health || exit 1"]
      interval: 15s
      timeout: 10s
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
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
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - madera-network

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: madera-mineria-clave-secreta-2026-cibertec
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
      ms-auth:
        condition: service_started
      ms-inventario:
        condition: service_started
      ms-pedidos:
        condition: service_started
      ms-notificaciones:
        condition: service_started
      ms-entrega:
        condition: service_started
    networks:
      - madera-network

  # ── OBSERVABILIDAD ───────────────────────────────────────────────────────────

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus-madera
    ports:
      - "9090:9090"
    volumes:
      - ./observabilidad/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    networks:
      - madera-network

  grafana:
    image: grafana/grafana:latest
    container_name: grafana-madera
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - madera-network
    depends_on:
      - prometheus

  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: elasticsearch-madera
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - madera-network
    healthcheck:
      test: ["CMD-SHELL",
             "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 20s
      timeout: 10s
      retries: 5

  logstash:
    image: logstash:8.11.0
    container_name: logstash-madera
    ports:
      - "5044:5044"
    volumes:
      - ./observabilidad/logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    environment:
      LS_JAVA_OPTS: "-Xmx256m -Xms256m"
    networks:
      - madera-network
    depends_on:
      elasticsearch:
        condition: service_healthy

  kibana:
    image: kibana:8.11.0
    container_name: kibana-madera
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    networks:
      - madera-network
    depends_on:
      elasticsearch:
        condition: service_healthy

  frontend:
    build: ./madera-frontend
    container_name: frontend-madera
    ports:
      - "80:80"
    depends_on:
      - api-gateway
    networks:
      - madera-network

# ── VOLÚMENES Y REDES ─────────────────────────────────────────────────────────

volumes:
  postgres_data:
  prometheus_data:
  grafana_data:
  elasticsearch_data:

networks:
  madera-network:
    driver: bridge
```

---

# Pasos de implementación

### Paso 1 — Crear `eureka-server` ⏱ 15 min

1. Generar en Spring Initializr con dependencia **Eureka Server**
2. Agregar `@EnableEurekaServer` en la clase principal
3. Configurar `application.properties`
4. Verificar que arranca en `http://localhost:8761`

**Verificación:** Dashboard de Eureka visible en el navegador.

---

### Paso 2 — Agregar Eureka Client a ms-inventario ⏱ 10 min

1. Agregar dependencia `spring-cloud-starter-netflix-eureka-client` en `pom.xml`
2. Agregar BOM de Spring Cloud si no existe
3. Agregar configuración de Eureka en `application.properties`
4. Reiniciar el microservicio

**Verificación:** En `http://localhost:8761` aparece `MS-INVENTARIO` registrado.

---

### Paso 3 — Agregar Eureka Client a ms-pedidos ⏱ 15 min

1. Agregar dependencia y configuración igual que ms-inventario
2. Modificar `InventarioClient.java` — quitar la propiedad `url`
3. Reiniciar el microservicio

**Verificación:** `MS-PEDIDOS` aparece en Eureka. Crear un pedido funciona correctamente.

---

### Paso 4 — Agregar Eureka Client a ms-notificaciones ⏱ 10 min

1. Agregar dependencia y configuración

**Verificación:** `MS-NOTIFICACIONES` aparece en Eureka.

---

### Paso 5 — Agregar Eureka Client a ms-auth ⏱ 10 min

1. Agregar dependencia y configuración

**Verificación:** `MS-AUTH` aparece en Eureka.

---

### Paso 6 — Agregar Eureka Client a ms-entrega ⏱ 15 min

1. Agregar dependencia y configuración
2. Modificar `PedidosClient.java` — quitar la propiedad `url`
3. Reiniciar el microservicio

**Verificación:** `MS-ENTREGA` aparece en Eureka. Confirmar recepción actualiza ms-pedidos correctamente.

---

### Paso 7 — Modificar api-gateway ⏱ 15 min

1. Agregar dependencia Eureka Client
2. Cambiar todas las URIs de `http://localhost:PORT` a `lb://nombre-servicio`
3. Agregar configuración de Eureka en `application.properties`
4. Reiniciar el gateway

**Verificación:** `API-GATEWAY` aparece en Eureka. Todas las requests al gateway funcionan correctamente.

---

### Paso 8 — Crear `Dockerfile` para eureka-server ⏱ 5 min

1. Crear el Dockerfile en `eureka-server/`
2. Actualizar `docker-compose.yml` con el servicio eureka-server
3. Agregar `depends_on: eureka-server` a todos los microservicios
4. Agregar variable de entorno `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` a cada servicio

**Verificación:** `docker-compose up --build` levanta todo correctamente.

---

### Paso 9 — Prueba final del sistema completo ⏱ 20 min

1. Ejecutar `docker-compose up --build`
2. Verificar `http://localhost:8761` — deben aparecer los 6 microservicios registrados
3. Hacer login desde Postman o frontend
4. Crear un pedido completo y verificar que el flujo funciona
5. Verificar en Eureka que las instancias están `UP`

---

## Puertos del sistema completo con Eureka

| Servicio | Puerto | URL |
|---|---|---|
| **Eureka Dashboard** | `8761` | `http://localhost:8761` |
| api-gateway | `8080` | `http://localhost:8080` |
| ms-inventario | `8082` | `http://localhost:8082` |
| ms-pedidos | `8083` | `http://localhost:8083` |
| ms-notificaciones | `8084` | `http://localhost:8084` |
| ms-auth | `8085` | `http://localhost:8085` |
| ms-entrega | `8086` | `http://localhost:8086` |
| Frontend | `80` | `http://localhost` |
| RabbitMQ panel | `15672` | `http://localhost:15672` |
| Prometheus | `9090` | `http://localhost:9090` |
| Grafana | `3000` | `http://localhost:3000` |
| Kibana | `5601` | `http://localhost:5601` |

---

## Checklist final

### eureka-server
- [ ] Arranca en el puerto `8761`
- [ ] Dashboard visible en `http://localhost:8761`
- [ ] No se registra a sí mismo

### Microservicios registrados
- [ ] `MS-AUTH` aparece en el dashboard de Eureka
- [ ] `MS-INVENTARIO` aparece en el dashboard de Eureka
- [ ] `MS-PEDIDOS` aparece en el dashboard de Eureka
- [ ] `MS-NOTIFICACIONES` aparece en el dashboard de Eureka
- [ ] `MS-ENTREGA` aparece en el dashboard de Eureka
- [ ] `API-GATEWAY` aparece en el dashboard de Eureka

### Feign con Eureka
- [ ] `InventarioClient` no tiene propiedad `url`
- [ ] `PedidosClient` no tiene propiedad `url`
- [ ] Crear pedido consulta stock correctamente (Feign via Eureka)
- [ ] Confirmar entrega actualiza pedido correctamente (Feign via Eureka)

### Gateway con Eureka
- [ ] Todas las rutas usan `lb://` en vez de `http://localhost`
- [ ] Login funciona a través del gateway
- [ ] Todas las rutas responden correctamente

### Docker
- [ ] `eureka-server` arranca antes que los microservicios
- [ ] Todos los microservicios se registran en Eureka al arrancar en Docker
- [ ] `docker-compose ps` muestra todos los servicios en `Up`
- [ ] Dashboard de Eureka muestra los 6 microservicios en `UP`

---

## Cobertura del sílabo — estado final

| Unidad | Tema | Estado |
|---|---|---|
| **1** | Feign Client — comunicación sincrónica | ✅ |
| **1** | RabbitMQ — mensajería asíncrona | ✅ |
| **1** | Spring Cloud Config | ✅ |
| **1** | **Eureka — Service Discovery** | ✅ Este plan |
| **2** | JWT + OAuth2 + Spring Security | ✅ |
| **2** | Resilience4J Circuit Breaker | ✅ |
| **2** | Docker + docker-compose | ✅ |
| **3** | Spring Boot Actuator | ✅ |
| **3** | Prometheus + Grafana | ✅ |
| **3** | ELK Stack | ✅ |

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
