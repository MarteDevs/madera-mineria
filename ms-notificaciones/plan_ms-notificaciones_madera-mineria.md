# Plan Técnico — ms-notificaciones
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-notificaciones · Puerto `8084`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`notificaciones_db`)  
> **Depende de:** ms-pedidos (consume eventos RabbitMQ)

---

## ¿Qué hace este microservicio?

Es el **consumidor de eventos** del sistema. No llama a nadie (sin Feign), solo escucha la cola RabbitMQ que `ms-pedidos` alimenta. Cada vez que se crea, aprueba o rechaza un pedido, este servicio recibe el evento, lo registra en su base de datos y genera una notificación para el área correspondiente.

| Responsabilidad | Descripción |
|---|---|
| Escuchar eventos | Consume mensajes de la cola `cola.pedidos.creados` via `@RabbitListener` |
| Registrar notificaciones | Guarda cada evento recibido en `notificaciones_db` con estado y fecha |
| Marcar como leída | El almacén puede marcar una notificación como leída |
| Exponer historial | Endpoints REST para consultar notificaciones por mina o estado |

---

## Cómo encaja en la arquitectura

```
ms-pedidos
    │
    │  publica evento "pedido.creado"
    ▼
RabbitMQ (cola.pedidos.creados)
    │
    │  @RabbitListener consume el mensaje
    ▼
ms-notificaciones
    │
    ├── Guarda notificación en notificaciones_db
    └── Marca como PENDIENTE (esperando que almacén la lea)
```

> Este flujo es completamente asíncrono. Si ms-notificaciones está apagado, los mensajes se acumulan en la cola y se procesan cuando vuelve a arrancar. Eso es la ventaja de RabbitMQ.

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST para consultar notificaciones |
| Spring Data JPA | Persistencia en PostgreSQL |
| PostgreSQL Driver | Conexión a `notificaciones_db` |
| Lombok | Getters, setters y constructores automáticos |
| Validation | Validar datos de entrada |
| DevTools | Recarga automática |
| **Spring for RabbitMQ** | Escuchar la cola `cola.pedidos.creados` con `@RabbitListener` |

---

## Estructura de archivos

```
ms-notificaciones/src/main/java/com/madera/notificaciones/
│
├── MsNotificacionesApplication.java       ← clase principal
│
├── model/
│   ├── Notificacion.java                  ← entidad principal (crear PRIMERO)
│   └── EstadoNotificacion.java            ← enum: PENDIENTE / LEIDA
│
├── dto/
│   ├── PedidoEvent.java                   ← objeto que llega desde RabbitMQ
│   └── NotificacionResponse.java         ← lo que devuelven los endpoints
│
├── repository/
│   └── NotificacionRepository.java
│
├── config/
│   └── RabbitMQConfig.java               ← misma cola que en ms-pedidos
│
├── listener/
│   └── PedidoEventListener.java          ← @RabbitListener aquí (lo más importante)
│
├── service/
│   ├── NotificacionService.java          ← interfaz
│   └── NotificacionServiceImpl.java      ← lógica de negocio
│
├── controller/
│   └── NotificacionController.java       ← endpoints REST
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → config → listener → service → controller

---

## Entidades de la base de datos

### Enum: `EstadoNotificacion`

```java
public enum EstadoNotificacion {
    PENDIENTE,    // recién llegó, el almacén aún no la ve
    LEIDA,        // el almacén la marcó como leída
    PROCESADA     // ya se tomó acción sobre ella
}
```

### Entidad: `Notificacion`

```java
@Entity
@Table(name = "notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del pedido que generó la notificación
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String areaSolicitante;

    // Tipo de evento recibido
    private String tipoEvento;         // "CREADO", "APROBADO", "RECHAZADO"

    // Mensaje legible para el almacén
    private String mensaje;

    // Estado de la notificación
    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estado;

    // Destinatario
    private String destinatario;       // "ALMACEN", "COMPRAS", "TRANSPORTE"

    @CreationTimestamp
    private LocalDateTime fechaRecibida;

    private LocalDateTime fechaLeida;
}
```

### DTO: `PedidoEvent` (mensaje que llega desde RabbitMQ)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String estado;             // "CREADO", "APROBADO", "RECHAZADO"
    private LocalDateTime fecha;
}
```

> Este DTO debe tener exactamente los mismos campos que el `PedidoEvent` de `ms-pedidos`. RabbitMQ deserializa el JSON automáticamente.

### DTO: `NotificacionResponse`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {
    private Long id;
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String tipoEvento;
    private String mensaje;
    private String estado;
    private String destinatario;
    private LocalDateTime fechaRecibida;
    private LocalDateTime fechaLeida;
}
```

---

## Configuración RabbitMQ

### `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

    // Mismos valores que en ms-pedidos — deben coincidir exactamente
    public static final String EXCHANGE    = "madera.exchange";
    public static final String QUEUE       = "cola.pedidos.creados";
    public static final String ROUTING_KEY = "pedido.creado";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaPedidosCreados() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue cola, TopicExchange exchange) {
        return BindingBuilder
            .bind(cola)
            .to(exchange)
            .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
            new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
```

---

## Listener — el corazón del microservicio

### `PedidoEventListener.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoEventListener {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void procesarEvento(PedidoEvent evento) {

        log.info("Evento recibido desde RabbitMQ: Pedido #{} - {} - {}",
            evento.getPedidoId(),
            evento.getTipoEvento(),
            evento.getMina());

        try {
            notificacionService.procesarEvento(evento);
            log.info("Notificación creada correctamente para pedido #{}",
                evento.getPedidoId());

        } catch (Exception e) {
            log.error("Error procesando evento del pedido #{}: {}",
                evento.getPedidoId(), e.getMessage());
            // RabbitMQ reintentará el mensaje si lanza excepción
            throw e;
        }
    }
}
```

> `@RabbitListener` escucha continuamente la cola. Cada vez que `ms-pedidos` publica un evento, este método se ejecuta automáticamente en un hilo separado.

---

## Repositorio

### `NotificacionRepository.java`

```java
@Repository
public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByMina(String mina);
    List<Notificacion> findByEstado(EstadoNotificacion estado);
    List<Notificacion> findByDestinatario(String destinatario);
    List<Notificacion> findByDestinatarioAndEstado(
        String destinatario, EstadoNotificacion estado);
    List<Notificacion> findByPedidoId(Long pedidoId);

    // Cuántas notificaciones pendientes hay para el almacén
    long countByDestinatarioAndEstado(
        String destinatario, EstadoNotificacion estado);
}
```

---

## Lógica de negocio

### `NotificacionServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepo;

    // ─── PROCESAR EVENTO RABBITMQ ────────────────────────────────────────────
    @Transactional
    public void procesarEvento(PedidoEvent evento) {

        Notificacion notificacion = new Notificacion();
        notificacion.setPedidoId(evento.getPedidoId());
        notificacion.setTipoMadera(evento.getTipoMadera());
        notificacion.setCantidad(evento.getCantidad());
        notificacion.setMina(evento.getMina());
        notificacion.setTipoEvento(evento.getEstado());
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);

        // Generar mensaje legible según el tipo de evento
        String mensaje = generarMensaje(evento);
        notificacion.setMensaje(mensaje);

        // Determinar destinatario según el tipo de evento
        String destinatario = determinarDestinatario(evento.getEstado());
        notificacion.setDestinatario(destinatario);

        notificacionRepo.save(notificacion);
    }

    // ─── MARCAR COMO LEÍDA ───────────────────────────────────────────────────
    @Transactional
    public NotificacionResponse marcarLeida(Long id) {

        Notificacion notificacion = notificacionRepo.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Notificación no encontrada: " + id));

        notificacion.setEstado(EstadoNotificacion.LEIDA);
        notificacion.setFechaLeida(LocalDateTime.now());
        notificacionRepo.save(notificacion);

        return mapToResponse(notificacion);
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    public List<NotificacionResponse> listarTodas() {
        return notificacionRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public List<NotificacionResponse> listarPendientes() {
        return notificacionRepo
            .findByEstado(EstadoNotificacion.PENDIENTE).stream()
            .map(this::mapToResponse).toList();
    }

    public List<NotificacionResponse> listarPorMina(String mina) {
        return notificacionRepo.findByMina(mina).stream()
            .map(this::mapToResponse).toList();
    }

    public List<NotificacionResponse> listarPorDestinatario(String dest) {
        return notificacionRepo.findByDestinatario(dest).stream()
            .map(this::mapToResponse).toList();
    }

    public long contarPendientesAlmacen() {
        return notificacionRepo.countByDestinatarioAndEstado(
            "ALMACEN", EstadoNotificacion.PENDIENTE);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    private String generarMensaje(PedidoEvent evento) {
        return switch (evento.getEstado()) {
            case "CREADO" -> String.format(
                "Nuevo pedido #%d: %d unidades de %s solicitadas por la mina %s. Requiere aprobación.",
                evento.getPedidoId(),
                evento.getCantidad(),
                evento.getTipoMadera(),
                evento.getMina());
            case "APROBADO" -> String.format(
                "Pedido #%d aprobado. Preparar %d unidades de %s para despacho a %s.",
                evento.getPedidoId(),
                evento.getCantidad(),
                evento.getTipoMadera(),
                evento.getMina());
            case "RECHAZADO" -> String.format(
                "Pedido #%d rechazado. Notificar a la mina %s.",
                evento.getPedidoId(),
                evento.getMina());
            default -> String.format(
                "Evento en pedido #%d: %s",
                evento.getPedidoId(),
                evento.getEstado());
        };
    }

    private String determinarDestinatario(String tipoEvento) {
        return switch (tipoEvento) {
            case "CREADO"   -> "ALMACEN";    // el almacén debe aprobar
            case "APROBADO" -> "ALMACEN";    // el almacén debe preparar
            case "RECHAZADO"-> "COMPRAS";    // compras debe informar a la mina
            default         -> "ADMIN";
        };
    }

    private NotificacionResponse mapToResponse(Notificacion n) {
        NotificacionResponse r = new NotificacionResponse();
        r.setId(n.getId());
        r.setPedidoId(n.getPedidoId());
        r.setTipoMadera(n.getTipoMadera());
        r.setCantidad(n.getCantidad());
        r.setMina(n.getMina());
        r.setTipoEvento(n.getTipoEvento());
        r.setMensaje(n.getMensaje());
        r.setEstado(n.getEstado().name());
        r.setDestinatario(n.getDestinatario());
        r.setFechaRecibida(n.getFechaRecibida());
        r.setFechaLeida(n.getFechaLeida());
        return r;
    }
}
```

---

## Endpoints REST

URL base: `http://localhost:8084/api/notificaciones`

### `NotificacionController.java`

```java
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listarTodas() {
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<NotificacionResponse>> listarPendientes() {
        return ResponseEntity.ok(notificacionService.listarPendientes());
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<NotificacionResponse>> porMina(
            @PathVariable String mina) {
        return ResponseEntity.ok(notificacionService.listarPorMina(mina));
    }

    @GetMapping("/destinatario/{destinatario}")
    public ResponseEntity<List<NotificacionResponse>> porDestinatario(
            @PathVariable String destinatario) {
        return ResponseEntity.ok(
            notificacionService.listarPorDestinatario(destinatario));
    }

    @GetMapping("/pendientes/count")
    public ResponseEntity<Long> contarPendientes() {
        return ResponseEntity.ok(notificacionService.contarPendientesAlmacen());
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/notificaciones` | Lista todas las notificaciones | Admin |
| `GET` | `/api/notificaciones/pendientes` | Solo las no leídas | Almacén |
| `GET` | `/api/notificaciones/mina/{mina}` | Notificaciones de una mina | Compras |
| `GET` | `/api/notificaciones/destinatario/{dest}` | Por destinatario (ALMACEN, COMPRAS) | Admin |
| `GET` | `/api/notificaciones/pendientes/count` | Cuántas pendientes hay | Almacén |
| `PUT` | `/api/notificaciones/{id}/leer` | Marcar como leída | Almacén |

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "Recurso no encontrado",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
            "error", "Error interno del servidor",
            "mensaje", ex.getMessage()
        ));
    }
}
```

---

## Pasos de implementación

### Paso 1 — Crear `EstadoNotificacion.java` ⏱ 5 min

Enum con 3 valores: `PENDIENTE`, `LEIDA`, `PROCESADA`.

**Verificación:** Compila sin errores.

---

### Paso 2 — Crear `Notificacion.java` ⏱ 15 min

Entidad principal en `model/`. Agregar todas las anotaciones JPA y Lombok.

**Verificación:** Al correr la app, JPA crea la tabla `notificacion` en `notificaciones_db`.

---

### Paso 3 — Crear los DTOs ⏱ 15 min

Crear en `dto/`:
- `PedidoEvent` — debe tener los mismos campos que en `ms-pedidos`
- `NotificacionResponse` — lo que devuelven los endpoints

**Verificación:** Compilan sin errores.

---

### Paso 4 — Crear `NotificacionRepository.java` ⏱ 10 min

Extender `JpaRepository`. Agregar los métodos de consulta por mina, estado y destinatario.

**Verificación:** Compila sin errores.

---

### Paso 5 — Crear `RabbitMQConfig.java` ⏱ 15 min

Mismos valores de exchange, cola y routing key que en `ms-pedidos`. Agregar el `MessageConverter` para deserializar el JSON automáticamente.

**Verificación:** Al arrancar la app con RabbitMQ corriendo, no hay errores de conexión.

---

### Paso 6 — Crear `NotificacionService` y `NotificacionServiceImpl` ⏱ 35 min

La implementación contiene `procesarEvento`, `marcarLeida` y todas las consultas. Los métodos `generarMensaje` y `determinarDestinatario` son helpers privados.

**Verificación:** Compila sin errores.

---

### Paso 7 — Crear `PedidoEventListener.java` ⏱ 15 min

Esta es la pieza más importante. El `@RabbitListener` conecta todo: recibe el mensaje de RabbitMQ y llama al service para guardarlo.

**Verificación:** Al crear un pedido en `ms-pedidos`, en la consola de `ms-notificaciones` aparece el log:
```
Evento recibido desde RabbitMQ: Pedido #1 - CREADO - Cerro Verde
Notificación creada correctamente para pedido #1
```

---

### Paso 8 — Crear `NotificacionController.java` ⏱ 15 min

Mapear los 6 endpoints. Todos son de solo lectura excepto el de marcar como leída.

**Verificación:** `GET http://localhost:8084/api/notificaciones` devuelve las notificaciones ya guardadas por el listener.

---

### Paso 9 — Crear `GlobalExceptionHandler.java` ⏱ 10 min

Capturar errores de recurso no encontrado y errores generales del servidor.

**Verificación:** Al consultar una notificación inexistente devuelve `404` con JSON de error.

---

## Pruebas con Postman

> Antes de probar, asegúrate de que **ms-inventario** (8082), **ms-pedidos** (8083) y **RabbitMQ** están corriendo.

### Prueba 1 — Verificar que el listener funciona

Con los 3 servicios corriendo, crea un pedido en ms-pedidos:

```
POST http://localhost:8083/api/pedidos
Content-Type: application/json
```

```json
{
  "maderaId": 1,
  "tipoMadera": "eucalipto",
  "cantidadSolicitada": 5,
  "mina": "Cerro Verde",
  "areaSolicitante": "Operaciones",
  "solicitadoPor": "juan.perez"
}
```

Luego inmediatamente consulta:

```
GET http://localhost:8084/api/notificaciones
```

**Respuesta esperada:** `200 OK` con la notificación ya creada automáticamente con mensaje:
> "Nuevo pedido #1: 5 unidades de eucalipto solicitadas por la mina Cerro Verde. Requiere aprobación."

---

### Prueba 2 — Listar notificaciones pendientes

```
GET http://localhost:8084/api/notificaciones/pendientes
```

**Respuesta esperada:** `200 OK` con lista de notificaciones en estado `PENDIENTE`.

---

### Prueba 3 — Contar pendientes del almacén

```
GET http://localhost:8084/api/notificaciones/pendientes/count
```

**Respuesta esperada:** `200 OK` con el número de notificaciones pendientes para el almacén. Ejemplo: `2`.

---

### Prueba 4 — Filtrar por mina

```
GET http://localhost:8084/api/notificaciones/mina/Cerro Verde
```

**Respuesta esperada:** `200 OK` con todas las notificaciones de esa mina.

---

### Prueba 5 — Filtrar por destinatario

```
GET http://localhost:8084/api/notificaciones/destinatario/ALMACEN
```

**Respuesta esperada:** `200 OK` con las notificaciones dirigidas al almacén.

---

### Prueba 6 — Marcar como leída

```
PUT http://localhost:8084/api/notificaciones/1/leer
```

**Respuesta esperada:** `200 OK` con la notificación en estado `LEIDA` y la `fechaLeida` registrada.

---

### Prueba 7 — Verificar que ya no aparece en pendientes

```
GET http://localhost:8084/api/notificaciones/pendientes
```

**Respuesta esperada:** La notificación marcada como leída ya no aparece en esta lista.

---

## Checklist final antes de avanzar a ms-auth

- [ ] La aplicación arranca en el puerto `8084` sin errores
- [ ] La tabla `notificacion` existe en `notificaciones_db`
- [ ] Al crear un pedido en ms-pedidos, el listener imprime el log en consola automáticamente
- [ ] `GET /api/notificaciones` devuelve las notificaciones creadas por el listener
- [ ] `GET /api/notificaciones/pendientes` filtra correctamente por estado
- [ ] `GET /api/notificaciones/destinatario/ALMACEN` filtra por destinatario
- [ ] `PUT /api/notificaciones/{id}/leer` cambia estado a `LEIDA` y registra la fecha
- [ ] `GET /api/notificaciones/pendientes/count` devuelve el conteo correcto
- [ ] Notificación inexistente devuelve `404` con JSON de error

---

## Evaluación T1 — Lo que ya tienes cubierto

Con los 3 microservicios completados, cubres toda la **Unidad 1** del sílabo:

| Tema del sílabo | Implementado en |
|---|---|
| Feign Client — comunicación sincrónica | ms-pedidos → ms-inventario |
| Consumo de APIs REST entre microservicios | ms-pedidos llama a ms-inventario |
| Mensajería asíncrona con RabbitMQ | ms-pedidos publica, ms-notificaciones consume |
| Principios básicos de mensajería | Exchange, Queue, Binding, Routing Key |

---

## Qué viene después

El siguiente microservicio es **ms-auth** (puerto `8085`). Es el más importante de la **Unidad 2** y cubre:

1. Registro y login de usuarios con roles (`ADMIN`, `ALMACEN`, `COMPRAS`, `TRANSPORTE`)
2. Generación de tokens **JWT** al hacer login
3. Validación de tokens en cada request
4. **Spring Security** con `OAuth2 Resource Server`

Con ms-auth listo, todos los endpoints de ms-inventario, ms-pedidos y ms-notificaciones quedarán protegidos y solo accesibles con un token válido.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
