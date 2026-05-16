# Plan Final — Observabilidad (Unidad 3)
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC
> **Unidad:** 3 — Observabilidad de Microservicios
> **Objetivo:** Monitoreo con Prometheus + Grafana y logs centralizados con ELK Stack
> **Tiempo estimado total:** 2 días

---

## ¿Qué se implementa?

| Herramienta | Puerto | Para qué |
|---|---|---|
| **Prometheus** | `9090` | Recolecta métricas de todos los microservicios vía Actuator |
| **Grafana** | `3000` | Visualiza las métricas en dashboards con gráficas |
| **Elasticsearch** | `9200` | Almacena y indexa todos los logs del sistema |
| **Logstash** | `5044` | Recibe y procesa logs de cada microservicio |
| **Kibana** | `5601` | Visualiza y busca logs en tiempo real |

---

## Arquitectura de observabilidad

```
Cada microservicio
      │
      │  /actuator/prometheus
      ▼
  Prometheus (9090)
  raspa métricas cada 15s
      │
      ▼
  Grafana (3000)
  muestra dashboards
  con gráficas en tiempo real


Cada microservicio
      │
      │  logs en formato JSON
      ▼
  Logstash (5044)
  procesa y transforma
      │
      ▼
  Elasticsearch (9200)
  almacena e indexa
      │
      ▼
  Kibana (5601)
  busca y visualiza logs
```

---

# PASO 1 — Prometheus + Grafana

## 1.1 Verificar que Actuator ya expone métricas

Antes de configurar Prometheus, verifica que cada microservicio expone el endpoint:

```
GET http://localhost:8082/actuator/prometheus
GET http://localhost:8083/actuator/prometheus
GET http://localhost:8084/actuator/prometheus
GET http://localhost:8085/actuator/prometheus
GET http://localhost:8086/actuator/prometheus
```

**Respuesta esperada:** texto con métricas en formato Prometheus:
```
# HELP jvm_memory_used_bytes ...
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",...} 1.234567E8
...
```

Si no responde, verifica que tienes en cada `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

---

## 1.2 Crear archivo de configuración de Prometheus

Crea la carpeta `observabilidad/` en la raíz del proyecto y dentro el archivo `prometheus.yml`:

```
madera-mineria/
├── docker-compose.yml
├── init-db.sql
├── observabilidad/
│   ├── prometheus.yml          ← crear aquí
│   └── grafana/
│       └── dashboards/         ← carpeta para dashboards
└── ...microservicios
```

### `observabilidad/prometheus.yml`

```yaml
global:
  scrape_interval: 15s          # raspa métricas cada 15 segundos
  evaluation_interval: 15s

scrape_configs:

  # Prometheus se monitorea a sí mismo
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # ms-inventario
  - job_name: 'ms-inventario'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ms-inventario:8082']
    labels:
      application: 'ms-inventario'
      environment: 'desarrollo'

  # ms-pedidos
  - job_name: 'ms-pedidos'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ms-pedidos:8083']
    labels:
      application: 'ms-pedidos'
      environment: 'desarrollo'

  # ms-notificaciones
  - job_name: 'ms-notificaciones'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ms-notificaciones:8084']
    labels:
      application: 'ms-notificaciones'
      environment: 'desarrollo'

  # ms-auth
  - job_name: 'ms-auth'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ms-auth:8085']
    labels:
      application: 'ms-auth'
      environment: 'desarrollo'

  # ms-entrega
  - job_name: 'ms-entrega'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ms-entrega:8086']
    labels:
      application: 'ms-entrega'
      environment: 'desarrollo'

  # api-gateway
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
    labels:
      application: 'api-gateway'
      environment: 'desarrollo'
```

---

## 1.3 Actualizar `docker-compose.yml`

Agregar Prometheus y Grafana al final del archivo existente:

```yaml
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
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    networks:
      - madera-network
    depends_on:
      - ms-inventario
      - ms-pedidos
      - ms-notificaciones
      - ms-auth
      - ms-entrega

  grafana:
    image: grafana/grafana:latest
    container_name: grafana-madera
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_USERS_ALLOW_SIGN_UP: false
    volumes:
      - grafana_data:/var/lib/grafana
    networks:
      - madera-network
    depends_on:
      - prometheus
```

### Agregar volúmenes al final del `docker-compose.yml`

```yaml
volumes:
  postgres_data:
  prometheus_data:      # agregar esta línea
  grafana_data:         # agregar esta línea
```

---

## 1.4 Configurar Grafana

### Acceder a Grafana

```
URL:      http://localhost:3000
Usuario:  admin
Password: admin123
```

### Conectar Prometheus como fuente de datos

1. Ir a **Configuration → Data Sources**
2. Clic en **Add data source**
3. Seleccionar **Prometheus**
4. En URL escribir: `http://prometheus:9090`
5. Clic en **Save & Test** — debe aparecer "Data source is working"

### Crear dashboard básico

1. Ir a **Dashboards → New Dashboard**
2. Clic en **Add new panel**
3. Agregar estos paneles con las queries de Prometheus:

#### Panel 1 — Requests HTTP por microservicio
```promql
sum(rate(http_server_requests_seconds_count[1m])) by (application)
```

#### Panel 2 — Errores HTTP (status 4xx y 5xx)
```promql
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[1m])) by (application, status)
```

#### Panel 3 — Tiempo de respuesta promedio
```promql
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])
```

#### Panel 4 — Uso de memoria JVM por microservicio
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

#### Panel 5 — Estado del Circuit Breaker
```promql
resilience4j_circuitbreaker_state{name="inventario"}
```

#### Panel 6 — Pedidos creados (métrica de negocio)
```promql
sum(increase(http_server_requests_seconds_count{uri="/api/pedidos", method="POST", status="201"}[5m]))
```

4. Guardar el dashboard con nombre **"Sistema Madera Minería"**

---

## 1.5 Importar dashboard predefinido de Spring Boot

Para tener un dashboard profesional sin configurarlo manualmente:

1. Ir a **Dashboards → Import**
2. En "Import via grafana.com" escribir: `12900`
3. Clic en **Load**
4. En "Prometheus" seleccionar la fuente de datos configurada
5. Clic en **Import**

Este dashboard muestra automáticamente JVM, HTTP requests, errores y más.

---

# PASO 2 — ELK Stack (Logs centralizados)

## 2.1 Agregar dependencia de Logstash en cada microservicio

### Dependencia en `pom.xml` (los 5 microservicios)

```xml
<!-- Logstash encoder para logs en formato JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

## 2.2 Configurar Logback en cada microservicio

Crear el archivo `src/main/resources/logback-spring.xml` en cada microservicio:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Appender para consola (desarrollo) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Appender para Logstash — envía logs en formato JSON -->
    <appender name="LOGSTASH"
              class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5044</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Campos adicionales para identificar el microservicio -->
            <customFields>
                {"aplicacion":"${spring.application.name}",
                 "entorno":"desarrollo"}
            </customFields>
        </encoder>
    </appender>

    <!-- Root logger — envía a consola Y a Logstash -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>

    <!-- Logger específico para el paquete del proyecto -->
    <logger name="com.madera" level="DEBUG"/>

</configuration>
```

## 2.3 Crear configuración de Logstash

Crear `observabilidad/logstash/logstash.conf`:

```
madera-mineria/
├── observabilidad/
│   ├── prometheus.yml
│   ├── logstash/
│   │   └── logstash.conf       ← crear aquí
│   └── grafana/
```

### `observabilidad/logstash/logstash.conf`

```
input {
  # Recibe logs de todos los microservicios
  tcp {
    port => 5044
    codec => json_lines
  }
}

filter {
  # Agregar timestamp si no existe
  if ![timestamp] {
    mutate {
      add_field => { "timestamp" => "%{@timestamp}" }
    }
  }

  # Parsear el nivel de log
  if [level] {
    mutate {
      uppercase => [ "level" ]
    }
  }

  # Identificar errores para alertas
  if [level] == "ERROR" {
    mutate {
      add_tag => [ "error" ]
    }
  }
}

output {
  # Enviar a Elasticsearch
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "madera-logs-%{+YYYY.MM.dd}"
  }

  # También imprimir en consola de Logstash (para debug)
  stdout {
    codec => rubydebug
  }
}
```

## 2.4 Agregar ELK Stack al `docker-compose.yml`

```yaml
  # ── ELK STACK ────────────────────────────────────────────────────────────────

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
```

### Agregar volumen de Elasticsearch

```yaml
volumes:
  postgres_data:
  prometheus_data:
  grafana_data:
  elasticsearch_data:     # agregar esta línea
```

---

## 2.5 Configurar Kibana

### Acceder a Kibana

```
URL: http://localhost:5601
```

### Crear índice de logs

1. Ir a **Management → Stack Management**
2. Clic en **Index Patterns** (o Data Views en versiones nuevas)
3. Clic en **Create index pattern**
4. En nombre escribir: `madera-logs-*`
5. En Time field seleccionar: `@timestamp`
6. Clic en **Create index pattern**

### Ver logs en tiempo real

1. Ir a **Analytics → Discover**
2. Seleccionar el índice `madera-logs-*`
3. Los logs de todos los microservicios aparecen en tiempo real

### Búsquedas útiles en Kibana

```
# Ver solo errores
level: ERROR

# Ver logs de un microservicio
aplicacion: "ms-pedidos"

# Ver errores de un microservicio específico
aplicacion: "ms-pedidos" AND level: ERROR

# Buscar logs de un pedido específico
message: "pedido #1"

# Ver todos los Circuit Breaker activados
message: "Circuit Breaker activo"
```

---

## `docker-compose.yml` final completo

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
    depends_on:
      - ms-inventario
      - ms-pedidos
      - ms-notificaciones
      - ms-auth
      - ms-entrega

  grafana:
    image: grafana/grafana:latest
    container_name: grafana-madera
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin123
      GF_USERS_ALLOW_SIGN_UP: false
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

## Pasos de implementación

### Día 1 — Prometheus + Grafana

#### Mañana (2 horas)
1. Verificar que `/actuator/prometheus` responde en los 5 microservicios
2. Crear carpeta `observabilidad/`
3. Crear `observabilidad/prometheus.yml`
4. Actualizar `docker-compose.yml` con Prometheus y Grafana
5. Agregar volúmenes `prometheus_data` y `grafana_data`
6. Ejecutar `docker-compose up --build`

#### Tarde (1.5 horas)
1. Abrir Grafana en `http://localhost:3000`
2. Conectar Prometheus como fuente de datos
3. Importar dashboard ID `12900`
4. Crear 2 paneles personalizados con las queries del plan
5. Verificar que las métricas aparecen en tiempo real

---

### Día 2 — ELK Stack

#### Mañana (2 horas)
1. Agregar dependencia `logstash-logback-encoder` en los 5 microservicios
2. Crear `logback-spring.xml` en cada microservicio
3. Crear `observabilidad/logstash/logstash.conf`
4. Actualizar `docker-compose.yml` con Elasticsearch, Logstash y Kibana
5. Agregar volumen `elasticsearch_data`
6. Ejecutar `docker-compose up --build`

#### Tarde (1.5 horas)
1. Abrir Kibana en `http://localhost:5601`
2. Crear index pattern `madera-logs-*`
3. Ir a Discover y verificar que llegan logs
4. Hacer algunas requests y buscar los logs en Kibana
5. Probar las búsquedas de errores y Circuit Breaker

---

## Puertos del sistema completo

| Servicio | Puerto | URL |
|---|---|---|
| api-gateway | `8080` | `http://localhost:8080` |
| ms-inventario | `8082` | `http://localhost:8082` |
| ms-pedidos | `8083` | `http://localhost:8083` |
| ms-notificaciones | `8084` | `http://localhost:8084` |
| ms-auth | `8085` | `http://localhost:8085` |
| ms-entrega | `8086` | `http://localhost:8086` |
| RabbitMQ panel | `15672` | `http://localhost:15672` |
| Prometheus | `9090` | `http://localhost:9090` |
| Grafana | `3000` | `http://localhost:3000` |
| Elasticsearch | `9200` | `http://localhost:9200` |
| Kibana | `5601` | `http://localhost:5601` |

---

## Checklist final — Sistema completo

### Prometheus
- [ ] `http://localhost:9090/targets` muestra todos los microservicios en estado UP
- [ ] Las métricas de cada microservicio aparecen en Prometheus
- [ ] Query `http_server_requests_seconds_count` devuelve datos

### Grafana
- [ ] Login en `http://localhost:3000` con admin/admin123
- [ ] Prometheus conectado como fuente de datos
- [ ] Dashboard importado ID 12900 muestra métricas
- [ ] Paneles personalizados con requests, errores y memoria
- [ ] Panel de Circuit Breaker muestra estado

### ELK Stack
- [ ] Elasticsearch responde en `http://localhost:9200`
- [ ] Logstash recibe logs de los microservicios
- [ ] Kibana accesible en `http://localhost:5601`
- [ ] Index pattern `madera-logs-*` creado
- [ ] Logs de todos los microservicios visibles en Discover
- [ ] Búsqueda por nivel ERROR funciona
- [ ] Búsqueda por aplicación funciona

### Sistema final
- [ ] `docker-compose ps` muestra todos los servicios en `Up`
- [ ] Flujo completo de pedido funciona a través del gateway
- [ ] Métricas del flujo visibles en Grafana en tiempo real
- [ ] Logs del flujo visibles en Kibana en tiempo real

---

## Cobertura total del sílabo

| Unidad | Tema | Estado |
|---|---|---|
| **1** | Feign Client — comunicación sincrónica | ✅ |
| **1** | RabbitMQ — mensajería asíncrona | ✅ |
| **1** | Spring Cloud Gateway | ✅ |
| **2** | JWT + OAuth2 + Spring Security | ✅ |
| **2** | Resilience4J Circuit Breaker | ✅ |
| **2** | Docker + docker-compose | ✅ |
| **3** | Spring Boot Actuator | ✅ |
| **3** | **Prometheus — monitoreo de métricas** | ✅ Este plan |
| **3** | **Grafana — dashboards visuales** | ✅ Este plan |
| **3** | **ELK Stack — logs centralizados** | ✅ Este plan |
| **3** | Optimización y escalabilidad | ✅ Docker + K8s ready |

---

## Tip para la presentación final (EF)

Para demostrar todo el sistema funcionando en la presentación:

1. Abrir en paralelo: **Grafana**, **Kibana** y **Postman**
2. Hacer el flujo completo desde Postman: login → crear pedido → aprobar → entregar
3. Mostrar en Grafana cómo suben las métricas de requests en tiempo real
4. Mostrar en Kibana los logs de cada paso del flujo
5. Apagar ms-inventario y mostrar el Circuit Breaker activándose
6. Mostrar el error en Kibana con `level: ERROR`

Eso cubre visualmente todas las unidades del sílabo en una sola demostración.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
