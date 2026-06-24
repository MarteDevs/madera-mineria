# Documentación Técnica - Madera & Minería

Bienvenido a la documentación técnica del **Sistema de Gestión de Pedidos y Entregas de Madera para Minería**. Este directorio contiene toda la información necesaria para comprender la arquitectura, los componentes y los patrones de comunicación del proyecto.

## Índice de Documentos

1. **[Arquitectura General](arquitectura_general.md)**
   - Visión global del sistema.
   - API Gateway y Eureka Service Discovery.
   - Frontend SPA (Vue 3).

2. **[Microservicios](microservicios.md)**
   - Detalle de cada microservicio implementado (Auth, Inventario, Proveedores, Pedidos, Entregas, Mantenimiento).
   - Tecnologías y bases de datos.

3. **[Patrones de Comunicación](comunicacion.md)**
   - Comunicación Síncrona (OpenFeign).
   - Comunicación Asíncrona (RabbitMQ).
   - Tolerancia a Fallos (Resilience4j Circuit Breaker).

4. **[Observabilidad y Monitoreo](observabilidad.md)**
   - Spring Boot Actuator.
   - Prometheus y Grafana.
   - Stack ELK (Elasticsearch, Logstash, Kibana) para centralización de logs.

## Stack Tecnológico Principal

- **Backend:** Java 17, Spring Boot 3.2.x, Spring Cloud (Eureka, Gateway, OpenFeign).
- **Frontend:** Vue 3 (Composition API), Pinia, Tailwind CSS, Vite.
- **Base de Datos:** PostgreSQL (Bases de datos independientes por servicio).
- **Mensajería:** RabbitMQ.
- **Infraestructura:** Docker & Docker Compose.
