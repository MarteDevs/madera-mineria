# Observabilidad y Monitoreo

Para mantener el control sobre los 6+ microservicios corriendo simultáneamente, el sistema cuenta con una capa sólida de observabilidad (Métricas y Logs).

## 1. Métricas con Prometheus y Spring Boot Actuator
Cada microservicio incluye la dependencia `spring-boot-starter-actuator` y `micrometer-registry-prometheus`.
- **Actuator:** Expone endpoints internos (ej. `/actuator/health` y `/actuator/prometheus`) que muestran el estado de salud de la app, el uso de memoria, CPU, y el estado de los Circuit Breakers.
- **Prometheus:** Es una base de datos de series temporales (desplegada en Docker) que hace *pull* (jalado) de los endpoints `/actuator/prometheus` de cada microservicio cada 15 segundos.
- **El flujo:** El microservicio genera la métrica -> Prometheus la recolecta y almacena -> Grafana la visualiza.

## 2. Visualización con Grafana
**Grafana** se conecta a Prometheus como fuente de datos.
- Permite construir Dashboards visuales atractivos.
- Muestra gráficas en tiempo real: ¿Cuántas peticiones HTTP por segundo recibe `ms-inventario`? ¿Cuánta RAM consume `ms-auth`?
- Facilita la creación de alertas visuales si un servicio se sobrecarga.

## 3. Trazabilidad y Centralización de Logs (Stack ELK)
Dado que hay contenedores independientes, leer los logs con `docker logs <contenedor>` uno por uno es ineficiente en producción. Para eso se utiliza el **Stack ELK (Elasticsearch, Logstash, Kibana)**.

1. **Logstash:** Actúa como un recolector. Los microservicios de Spring Boot están configurados (vía Logback o envío directo) para escupir sus logs hacia Logstash.
2. **Elasticsearch:** Logstash procesa los logs, los indexa y los guarda en Elasticsearch, una base de datos ultrarrápida diseñada para búsquedas de texto completo.
3. **Kibana:** Es la interfaz gráfica conectada a Elasticsearch.
   - **Beneficio:** Si ocurre un error en el Gateway, Kibana permite rastrear el TraceID (identificador único de la petición) para ver por qué microservicios pasó el error (ej: Gateway -> Auth -> Proveedores) todo en una sola pantalla, sin tener que entrar a las consolas individuales de Docker.

## Tolerancia a Fallos Visible
Gracias a Actuator y Micrometer, los eventos del **Circuit Breaker** (estado ABIERTO, CERRADO, SEMI-ABIERTO) y los reintentos (Retries) de Resilience4j se exportan automáticamente a Prometheus, lo que permite visualizar en Grafana en qué momento un servicio dejó de responder y cuántas peticiones fueron salvadas por el método *fallback*.
