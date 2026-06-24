# 🪵 Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II  
> **Instituto:** CIBERTEC — Carrera de Computación e Informática  
> **Ciclo:** Sexto · Período 2026  
> **Arquitectura:** Microservicios con Spring Boot 3.5.14 · Java 21

---

## 📋 Descripción

Sistema de gestión de pedidos y entregas de madera para operaciones mineras. Permite a las minas solicitar madera (eucalipto, pino, roble) para sus operaciones de galería y entibado, gestionar el inventario del almacén, aprobar pedidos, y hacer seguimiento del despacho hasta la confirmación de recepción en la mina.

---

## 🏗️ Arquitectura del sistema

```
                        ┌─────────────────┐
                        │   api-gateway   │
                        │   puerto 8080   │
                        │  JWT · Routing  │
                        └────────┬────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                       │
    ┌─────▼──────┐        ┌──────▼─────┐        ┌──────▼──────┐
    │ ms-auth    │        │ms-inventario│        │ ms-pedidos  │
    │ puerto 8085│        │ puerto 8082 │        │ puerto 8083 │
    │ JWT·Roles  │        │ Stock·Madera│        │Feign·Rabbit │
    └────────────┘        └────────────┘        └──────┬──────┘
                                                        │
                                          ┌─────────────┼──────────────┐
                                          │                            │
                                   ┌──────▼──────┐             ┌──────▼──────┐
                                   │ms-notific.. │             │ ms-entrega  │
                                   │ puerto 8084 │             │ puerto 8086 │
                                   │RabbitMQ Lst │             │Feign·Rabbit │
                                   └─────────────┘             └─────────────┘

                    ┌─────────────────────────────────────────┐
                    │           OBSERVABILIDAD                 │
                    │  Prometheus:9090 · Grafana:3000          │
                    │  Elasticsearch:9200 · Kibana:5601        │
                    └─────────────────────────────────────────┘
```

---

## 🔧 Microservicios

| Microservicio | Puerto | Base de datos | Descripción |
|---|---|---|---|
| **api-gateway** | `8080` | — | Punto de entrada único, validación JWT y enrutamiento |
| **ms-inventario** | `8082` | `inventario_db` | Gestión de stock de madera por tipo y mina |
| **ms-pedidos** | `8083` | `pedidos_db` | Ciclo de vida completo de pedidos de madera |
| **ms-notificaciones** | `8084` | `notificaciones_db` | Notificaciones asíncronas vía RabbitMQ |
| **ms-auth** | `8085` | `auth_db` | Autenticación JWT y control de roles |
| **ms-entrega** | `8086` | `entrega_db` | Despacho y seguimiento de entregas a la mina |

---

## 🚀 Tecnologías utilizadas

### Backend
- **Java 21** + **Spring Boot 3.5.14**
- **Spring Cloud Gateway** — enrutamiento y seguridad centralizada
- **Spring Security** + **JWT (jjwt 0.11.5)** — autenticación y autorización
- **Spring Data JPA** + **PostgreSQL 15** — persistencia de datos
- **OpenFeign** — comunicación sincrónica entre microservicios
- **RabbitMQ** — mensajería asíncrona entre microservicios
- **Resilience4J** — Circuit Breaker y Retry para resiliencia
- **Spring Boot Actuator** + **Micrometer** — métricas y health checks

### Observabilidad
- **Prometheus** — recolección de métricas
- **Grafana** — dashboards visuales en tiempo real
- **ELK Stack** (Elasticsearch + Logstash + Kibana) — logs centralizados

### DevOps
- **Docker** + **Docker Compose** — contenedores y orquestación local
- **Maven** — gestión de dependencias

---

## 🔄 Flujo principal del sistema

```
1. 🔐 Login          POST /api/auth/login           → token JWT
2. 📦 Crear pedido   POST /api/pedidos              → verifica stock (Feign)
3. 🔔 Notificación   RabbitMQ → ms-notificaciones   → almacén alertado
4. ✅ Aprobar         PUT  /api/pedidos/{id}/aprobar → reduce stock (Feign)
5. 🚛 Entrega creada RabbitMQ → ms-entrega          → automático
6. 👷 Transportista  PUT  /api/entregas/{id}/asignar-transportista
7. 🛣️  En ruta        PUT  /api/entregas/{id}/en-ruta
8. 📬 Confirmar      PUT  /api/entregas/{id}/confirmar-recepcion
9. 🏁 Completado     Feign → ms-pedidos → ENTREGADO
```

---

## 👥 Roles del sistema

| Rol | Permisos |
|---|---|
| `ROLE_ADMIN` | Acceso total al sistema |
| `ROLE_ALMACEN` | Gestionar inventario, aprobar/rechazar pedidos |
| `ROLE_COMPRAS` | Crear pedidos, ver inventario de su mina |
| `ROLE_TRANSPORTE` | Ver pedidos aprobados, actualizar entregas |

---

## ⚙️ Requisitos previos

- **Java 21** instalado
- **Docker Desktop** instalado y corriendo
- **Maven 3.8+** instalado
- **Git** instalado

---

## 🐳 Ejecución con Docker (recomendado)

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/madera-mineria.git
cd madera-mineria
```

### 2. Levantar todo el sistema

```bash
docker-compose up --build
```

### 3. Verificar que todos los servicios están corriendo

```bash
docker-compose ps
```

Todos los servicios deben aparecer en estado `Up`.

### 4. Verificar health checks

```bash
# Gateway
curl http://localhost:8080/actuator/health

# Cada microservicio
curl http://localhost:8082/actuator/health  # ms-inventario
curl http://localhost:8083/actuator/health  # ms-pedidos
curl http://localhost:8084/actuator/health  # ms-notificaciones
curl http://localhost:8085/actuator/health  # ms-auth
curl http://localhost:8086/actuator/health  # ms-entrega
```

---

## 🔑 Primeros pasos — usuarios de prueba

### Registrar usuarios

```bash
# Administrador
POST http://localhost:8080/api/auth/register
{
  "nombre": "Carlos",
  "apellido": "Mamani",
  "email": "admin@madera.com",
  "password": "admin123",
  "rol": "ROLE_ADMIN"
}

# Almacén
POST http://localhost:8080/api/auth/register
{
  "nombre": "Rosa",
  "apellido": "Garcia",
  "email": "almacen@madera.com",
  "password": "almacen123",
  "rol": "ROLE_ALMACEN"
}

# Compras (Cerro Verde)
POST http://localhost:8080/api/auth/register
{
  "nombre": "Juan",
  "apellido": "Perez",
  "email": "compras@cerroverde.com",
  "password": "compras123",
  "rol": "ROLE_COMPRAS",
  "mina": "Cerro Verde"
}

# Transporte
POST http://localhost:8080/api/auth/register
{
  "nombre": "Pedro",
  "apellido": "Quispe",
  "email": "transporte@madera.com",
  "password": "trans123",
  "rol": "ROLE_TRANSPORTE"
}
```

### Login y obtener token

```bash
POST http://localhost:8080/api/auth/login
{
  "email": "admin@madera.com",
  "password": "admin123"
}
```

Usar el token en todas las requests siguientes:
```
Authorization: Bearer <token>
```

---

## 📡 Endpoints principales

### ms-inventario (via gateway)
```
GET    /api/inventario                    Lista toda la madera
GET    /api/inventario/mina/{mina}        Madera por mina
GET    /api/inventario/{id}/stock         Consultar stock
POST   /api/inventario                    Registrar madera
PUT    /api/inventario/{id}/reducir       Reducir stock
```

### ms-pedidos (via gateway)
```
GET    /api/pedidos                       Lista todos los pedidos
POST   /api/pedidos                       Crear pedido
PUT    /api/pedidos/{id}/aprobar          Aprobar pedido
PUT    /api/pedidos/{id}/rechazar         Rechazar pedido
GET    /api/pedidos/mina/{mina}           Pedidos por mina
GET    /api/pedidos/estado/{estado}       Pedidos por estado
```

### ms-entrega (via gateway)
```
GET    /api/entregas                      Lista todas las entregas
GET    /api/entregas/en-transito          Entregas activas
PUT    /api/entregas/{id}/asignar-transportista
PUT    /api/entregas/{id}/en-ruta
PUT    /api/entregas/{id}/confirmar-recepcion
```

### ms-notificaciones (via gateway)
```
GET    /api/notificaciones/pendientes     Notificaciones sin leer
GET    /api/notificaciones/pendientes/count
PUT    /api/notificaciones/{id}/leer      Marcar como leída
```

---

## 📊 Observabilidad

### Prometheus
```
URL: http://localhost:9090
```
Ver todos los targets activos: `http://localhost:9090/targets`

### Grafana
```
URL:      http://localhost:3000
Usuario:  admin
Password: admin123
```
Dashboard predefinido importado con ID `12900`.

### RabbitMQ Management
```
URL:      http://localhost:15672
Usuario:  guest
Password: guest
```

### Kibana (logs)
```
URL: http://localhost:5601
Index pattern: madera-logs-*
```

---

## 🛡️ Resiliencia

El sistema implementa **Resilience4J Circuit Breaker** en los puntos críticos:

- **ms-pedidos → ms-inventario:** Si el inventario no responde, el Circuit Breaker se abre y devuelve un mensaje claro al usuario en vez de un error 500.
- **ms-entrega → ms-pedidos:** Si ms-pedidos no responde al confirmar una entrega, la entrega se guarda localmente y se reintenta después.

Configuración del Circuit Breaker:
- Ventana de análisis: 5 llamadas
- Umbral de fallo: 50%
- Tiempo de espera: 10 segundos
- Reintentos: 3 intentos con 2 segundos entre cada uno

---

## 🗂️ Estructura del proyecto

```
madera-mineria/
├── api-gateway/                  ← Enrutamiento y seguridad JWT
│   ├── src/
│   └── Dockerfile
├── ms-auth/                      ← Autenticación y usuarios
│   ├── src/
│   └── Dockerfile
├── ms-inventario/                ← Stock de madera
│   ├── src/
│   └── Dockerfile
├── ms-pedidos/                   ← Gestión de pedidos
│   ├── src/
│   └── Dockerfile
├── ms-notificaciones/            ← Notificaciones RabbitMQ
│   ├── src/
│   └── Dockerfile
├── ms-entrega/                   ← Despacho y entregas
│   ├── src/
│   └── Dockerfile
├── observabilidad/
│   ├── prometheus.yml            ← Configuración de Prometheus
│   └── logstash/
│       └── logstash.conf         ← Pipeline de logs
├── docker-compose.yml            ← Orquestación completa
├── init-db.sql                   ← Creación de bases de datos
└── README.md
```

---

## 📚 Cobertura del sílabo

| Unidad | Tema | Implementado en |
|---|---|---|
| 1 | Feign Client — comunicación sincrónica | ms-pedidos ↔ ms-inventario |
| 1 | Mensajería asíncrona con RabbitMQ | ms-pedidos → ms-notificaciones/ms-entrega |
| 1 | Spring Cloud y configuración centralizada | api-gateway |
| 2 | JWT + OAuth2 para autenticación | ms-auth + api-gateway |
| 2 | Spring Security | ms-auth |
| 2 | Resilience4J Circuit Breaker | ms-pedidos, ms-entrega |
| 2 | Docker y contenedores | Dockerfile + docker-compose.yml |
| 3 | Spring Boot Actuator | Todos los microservicios |
| 3 | Prometheus + Grafana | observabilidad/ |
| 3 | ELK Stack — logs centralizados | observabilidad/logstash/ |

---

## 🛑 Detener el sistema

```bash
# Detener sin borrar datos
docker-compose stop

# Detener y borrar contenedores (conserva datos)
docker-compose down

# Detener y borrar todo incluyendo datos
docker-compose down -v
```

---

## 👨‍💻 Autor
**Nombre:** Marco Rodrigo Polo Silva
**Colaborador:** Ismael Iparraguirre

**Desarrollado como proyecto final del curso **Desarrollo de Aplicaciones Web II**  
**CIBERTEC** — Facultad de Tecnologías de la Información  
Período 2026

---

*Sistema desarrollado con Spring Boot 3.5.14 · Java 21 · Docker · RabbitMQ · PostgreSQL*
