# Plan Técnico — ms-pedidos
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-pedidos · Puerto `8083`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`pedidos_db`)  
> **Depende de:** ms-inventario (puerto `8082`)

---

## ¿Qué hace este microservicio?

Es el **núcleo del sistema**. Gestiona todo el ciclo de vida de un pedido de madera: desde que una mina lo solicita hasta que es aprobado y despachado. Se comunica con `ms-inventario` de forma sincrónica (Feign) para verificar stock, y publica eventos de forma asíncrona (RabbitMQ) para notificar al almacén.

| Responsabilidad | Descripción |
|---|---|
| Crear pedidos | Una mina solicita madera indicando tipo, cantidad y destino |
| Verificar stock | Llama a ms-inventario vía Feign antes de aceptar el pedido |
| Gestionar estados | El pedido pasa por: PENDIENTE → APROBADO → EN_PREPARACION → DESPACHADO → ENTREGADO |
| Publicar eventos | Al crear un pedido, publica un evento en RabbitMQ para notificar al almacén |
| Historial | Registra cada cambio de estado con fecha y responsable |

---

## Flujo principal del pedido

```
Mina solicita madera
        │
        ▼
ms-pedidos recibe la solicitud
        │
        ▼
Feign llama a ms-inventario → ¿Hay stock suficiente?
        │
   ┌────┴────┐
   NO        SÍ
   │         │
   ▼         ▼
Rechaza   Crea pedido en estado PENDIENTE
pedido         │
               ▼
        Publica evento en RabbitMQ
        (ms-notificaciones lo recibe)
               │
               ▼
        Almacén aprueba → APROBADO
               │
               ▼
        Feign reduce stock en ms-inventario
               │
               ▼
        EN_PREPARACION → DESPACHADO → ENTREGADO
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST del ciclo de pedidos |
| Spring Data JPA | Persistencia en PostgreSQL |
| PostgreSQL Driver | Conexión a la base de datos |
| Lombok | Getters, setters y constructores automáticos |
| Validation | Validar datos de entrada (`@NotNull`, `@Min`) |
| DevTools | Recarga automática |
| **OpenFeign** | Llamar a ms-inventario para consultar y reducir stock |
| **RabbitMQ** | Publicar evento cuando se crea o aprueba un pedido |

---

## Estructura de archivos

```
ms-pedidos/src/main/java/com/madera/pedidos/
│
├── MsPedidosApplication.java              ← @EnableFeignClients aquí
│
├── model/
│   ├── Pedido.java                        ← entidad principal (crear PRIMERO)
│   ├── EstadoPedido.java                  ← enum de estados
│   └── HistorialEstado.java              ← registro de cambios de estado
│
├── dto/
│   ├── PedidoRequest.java                 ← datos para crear un pedido
│   ├── PedidoResponse.java               ← datos que devuelve la API
│   ├── CambioEstadoRequest.java          ← para aprobar/rechazar
│   └── PedidoEvent.java                  ← evento que se publica en RabbitMQ
│
├── repository/
│   └── PedidoRepository.java
│
├── client/
│   ├── InventarioClient.java             ← interfaz Feign (llamadas a ms-inventario)
│   └── InventarioClientFallback.java     ← respuesta si ms-inventario no responde
│
├── config/
│   └── RabbitMQConfig.java               ← exchange, queue y binding
│
├── service/
│   ├── PedidoService.java                ← interfaz
│   └── PedidoServiceImpl.java            ← lógica de negocio
│
├── controller/
│   └── PedidoController.java             ← endpoints REST
│
└── exception/
    ├── PedidoNotFoundException.java
    ├── StockInsuficienteException.java
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → client → config → service → controller → exception

---

## Entidades de la base de datos

### Enum: `EstadoPedido`

```java
public enum EstadoPedido {
    PENDIENTE,        // recién creado, esperando aprobación
    APROBADO,         // almacén aprobó, se descuenta stock
    EN_PREPARACION,   // el almacén está alistando la madera
    DESPACHADO,       // salió hacia la mina
    ENTREGADO,        // confirmación de recepción en la mina
    RECHAZADO         // sin stock o rechazado por el almacén
}
```

### Entidad: `Pedido`

```java
@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos de la madera solicitada
    private Long maderaId;              // referencia al ms-inventario
    private String tipoMadera;          // "eucalipto", "pino", etc.
    private String usoMadera;           // "soporte_galeria", "entibado"

    @Min(1)
    private Integer cantidadSolicitada;

    private String unidad;              // "m3", "metro_lineal", "unidad"

    // Datos de la solicitud
    @NotBlank
    private String mina;                // "Cerro Verde", "Antamina"

    private String areaSolicitante;     // "Operaciones", "Mantenimiento"
    private String solicitadoPor;       // username del comprador

    // Estado y fechas
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    @CreationTimestamp
    private LocalDateTime fechaPedido;

    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;

    // Control
    private String aprobadoPor;         // quien aprobó en almacén
    private String motivoRechazo;       // si fue rechazado
    private String observaciones;       // notas adicionales

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
```

### Entidad: `HistorialEstado`

```java
@Entity
@Table(name = "historial_estado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoNuevo;

    private String responsable;         // quien hizo el cambio
    private String motivo;              // razón del cambio

    @CreationTimestamp
    private LocalDateTime fecha;
}
```

### DTO: `PedidoEvent` (mensaje RabbitMQ)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String estado;              // "CREADO", "APROBADO", "RECHAZADO"
    private LocalDateTime fecha;
}
```

---

## Feign Client — comunicación con ms-inventario

### `InventarioClient.java`

```java
@FeignClient(
    name = "ms-inventario",
    url = "http://localhost:8082",
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {

    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable Long id);

    @PutMapping("/api/inventario/{id}/reducir")
    void reducirStock(@PathVariable Long id,
                      @RequestParam Integer cantidad);
}
```

### `InventarioClientFallback.java`

```java
@Component
public class InventarioClientFallback implements InventarioClient {

    @Override
    public Integer consultarStock(Long id) {
        // Si ms-inventario no responde, devuelve -1
        // El service interpretará -1 como "no se pudo consultar"
        return -1;
    }

    @Override
    public void reducirStock(Long id, Integer cantidad) {
        // Loguear el error — el stock no se pudo reducir
        System.err.println("ms-inventario no disponible. Stock no reducido para madera: " + id);
    }
}
```

> El fallback es el mecanismo de resiliencia básico. En la Fase 2 del curso se reemplaza con Resilience4J Circuit Breaker completo.

---

## Configuración RabbitMQ

### `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "madera.exchange";
    public static final String QUEUE       = "cola.pedidos.creados";
    public static final String ROUTING_KEY = "pedido.creado";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaPedidosCreados() {
        // durable=true: la cola sobrevive si RabbitMQ se reinicia
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
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
```

---

## Repositorio

### `PedidoRepository.java`

```java
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByMina(String mina);
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findBySolicitadoPor(String usuario);
    List<Pedido> findByMinaAndEstado(String mina, EstadoPedido estado);

    // Para reportes: pedidos en un rango de fechas
    List<Pedido> findByFechaPedidoBetween(
        LocalDateTime inicio, LocalDateTime fin);

    // Cuántos pedidos pendientes tiene una mina
    long countByMinaAndEstado(String mina, EstadoPedido estado);
}
```

---

## Lógica de negocio

### `PedidoServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository     pedidoRepo;
    private final InventarioClient      inventarioClient;
    private final AmqpTemplate          amqpTemplate;

    // ─── CREAR PEDIDO ────────────────────────────────────────────────────────
    @Transactional
    public PedidoResponse crearPedido(PedidoRequest request) {

        // 1. Consultar stock en ms-inventario vía Feign
        Integer stock = inventarioClient.consultarStock(request.getMaderaId());

        if (stock == -1) {
            throw new RuntimeException(
                "No se pudo conectar con el servicio de inventario. Intente más tarde.");
        }

        if (stock < request.getCantidadSolicitada()) {
            throw new StockInsuficienteException(
                "Stock disponible: " + stock +
                ", solicitado: " + request.getCantidadSolicitada());
        }

        // 2. Crear y guardar el pedido
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

        // 3. Publicar evento en RabbitMQ (asíncrono)
        PedidoEvent evento = new PedidoEvent(
            pedido.getId(),
            pedido.getTipoMadera(),
            pedido.getCantidadSolicitada(),
            pedido.getMina(),
            "CREADO",
            LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            evento
        );

        return mapToResponse(pedido);
    }

    // ─── APROBAR PEDIDO ──────────────────────────────────────────────────────
    @Transactional
    public PedidoResponse aprobarPedido(Long id, String aprobadoPor) {

        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        // Reducir stock en ms-inventario vía Feign
        inventarioClient.reducirStock(
            pedido.getMaderaId(),
            pedido.getCantidadSolicitada()
        );

        pedido.setEstado(EstadoPedido.APROBADO);
        pedido.setAprobadoPor(aprobadoPor);
        pedido.setFechaAprobacion(LocalDateTime.now());
        pedidoRepo.save(pedido);

        return mapToResponse(pedido);
    }

    // ─── RECHAZAR PEDIDO ─────────────────────────────────────────────────────
    @Transactional
    public PedidoResponse rechazarPedido(Long id, String motivo) {

        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        pedido.setEstado(EstadoPedido.RECHAZADO);
        pedido.setMotivoRechazo(motivo);
        pedidoRepo.save(pedido);

        return mapToResponse(pedido);
    }

    // ─── CAMBIAR ESTADO ──────────────────────────────────────────────────────
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {

        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        pedido.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoPedido.ENTREGADO) {
            pedido.setFechaEntregaReal(LocalDateTime.now());
        }

        pedidoRepo.save(pedido);
        return mapToResponse(pedido);
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    public List<PedidoResponse> listarTodos() {
        return pedidoRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public List<PedidoResponse> listarPorMina(String mina) {
        return pedidoRepo.findByMina(mina).stream()
            .map(this::mapToResponse).toList();
    }

    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado).stream()
            .map(this::mapToResponse).toList();
    }

    public PedidoResponse obtenerPorId(Long id) {
        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));
        return mapToResponse(pedido);
    }

    // ─── MAPPER ──────────────────────────────────────────────────────────────
    private PedidoResponse mapToResponse(Pedido p) {
        PedidoResponse r = new PedidoResponse();
        r.setId(p.getId());
        r.setTipoMadera(p.getTipoMadera());
        r.setCantidadSolicitada(p.getCantidadSolicitada());
        r.setMina(p.getMina());
        r.setEstado(p.getEstado().name());
        r.setFechaPedido(p.getFechaPedido());
        r.setFechaEntregaEstimada(p.getFechaEntregaEstimada());
        return r;
    }
}
```

---

## Endpoints REST

URL base: `http://localhost:8083/api/pedidos`

### Pedidos

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/pedidos` | Lista todos los pedidos | Admin / Almacén |
| `GET` | `/api/pedidos/{id}` | Detalle de un pedido | Todos |
| `GET` | `/api/pedidos/mina/{mina}` | Pedidos de una mina | Compras / Admin |
| `GET` | `/api/pedidos/estado/{estado}` | Filtrar por estado | Almacén |
| `POST` | `/api/pedidos` | Crear nuevo pedido | Compras |
| `PUT` | `/api/pedidos/{id}/aprobar` | Aprobar pedido | Almacén |
| `PUT` | `/api/pedidos/{id}/rechazar` | Rechazar con motivo | Almacén |
| `PUT` | `/api/pedidos/{id}/estado` | Cambiar estado | Almacén / Transporte |

### `PedidoController.java`

```java
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<PedidoResponse>> porMina(@PathVariable String mina) {
        return ResponseEntity.ok(pedidoService.listarPorMina(mina));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponse>> porEstado(
            @PathVariable EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.listarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(201).body(pedidoService.crearPedido(request));
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PedidoResponse> aprobar(
            @PathVariable Long id,
            @RequestParam String aprobadoPor) {
        return ResponseEntity.ok(pedidoService.aprobarPedido(id, aprobadoPor));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<PedidoResponse> rechazar(
            @PathVariable Long id,
            @RequestParam String motivo) {
        return ResponseEntity.ok(pedidoService.rechazarPedido(id, motivo));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido nuevoEstado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, nuevoEstado));
    }
}
```

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleStock(StockInsuficienteException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Stock insuficiente",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(PedidoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PedidoNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "Pedido no encontrado",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleEstado(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Cambio de estado inválido",
            "mensaje", ex.getMessage()
        ));
    }
}
```

---

## Pasos de implementación

### Paso 1 — Crear `EstadoPedido.java` ⏱ 5 min

Crear el enum en `model/`. Solo define los 6 estados. Sin lógica.

**Verificación:** Compila sin errores.

---

### Paso 2 — Crear `Pedido.java` ⏱ 20 min

Crear la entidad principal en `model/`. Agregar todas las anotaciones JPA y Lombok.

**Verificación:** Al correr la app, JPA crea la tabla `pedido` en `pedidos_db`. Verificar en pgAdmin.

---

### Paso 3 — Crear `HistorialEstado.java` ⏱ 10 min

Crear en `model/`. Tiene relación `@ManyToOne` con `Pedido`.

**Verificación:** JPA crea la tabla `historial_estado` con columna `pedido_id`.

---

### Paso 4 — Crear los DTOs ⏱ 20 min

Crear en `dto/`:
- `PedidoRequest` con validaciones `@NotNull` y `@Min(1)` en cantidad
- `PedidoResponse` con los campos que devuelve la API
- `CambioEstadoRequest` con el nuevo estado y el responsable
- `PedidoEvent` con los datos del evento RabbitMQ

**Verificación:** Compilan sin errores.

---

### Paso 5 — Crear `PedidoRepository.java` ⏱ 10 min

Extender `JpaRepository`. Agregar los métodos de consulta por mina, estado y fechas.

**Verificación:** Compila sin errores.

---

### Paso 6 — Crear `InventarioClient.java` y su fallback ⏱ 15 min

Crear en `client/`. La interfaz Feign apunta a `http://localhost:8082`. El fallback devuelve `-1` en `consultarStock` para que el service sepa que hubo un error de conexión.

**Verificación:** La app arranca. Feign no falla al iniciar si ms-inventario está apagado.

---

### Paso 7 — Crear `RabbitMQConfig.java` ⏱ 15 min

Crear en `config/`. Define el exchange tipo Topic, la cola durable y el binding con el routing key `pedido.creado`.

**Verificación:** Al arrancar la app con RabbitMQ corriendo, aparece la cola `cola.pedidos.creados` en el panel de RabbitMQ (`http://localhost:15672`).

---

### Paso 8 — Crear `PedidoService` y `PedidoServiceImpl` ⏱ 40 min

Crear en `service/`. La implementación contiene toda la lógica descrita arriba: verificar stock, crear pedido, publicar evento, aprobar con reducción de stock, rechazar y cambiar estados.

**Verificación:** Compila sin errores.

---

### Paso 9 — Crear `PedidoController.java` ⏱ 20 min

Crear en `controller/`. Mapear los 8 endpoints con sus anotaciones correctas.

**Verificación:** `GET http://localhost:8083/api/pedidos` devuelve `[]` en el navegador.

---

### Paso 10 — Crear excepciones y `GlobalExceptionHandler` ⏱ 15 min

Crear `PedidoNotFoundException`, `StockInsuficienteException` y el handler global.

**Verificación:** Al pedir un pedido inexistente devuelve `404` con JSON de error claro.

---

## Pruebas con Postman

> Antes de probar, asegúrate de que **ms-inventario está corriendo** en el puerto `8082` y tiene al menos una madera registrada con stock disponible.

### Prueba 1 — Crear un pedido con stock suficiente

```
POST http://localhost:8083/api/pedidos
Content-Type: application/json
```

```json
{
  "maderaId": 1,
  "tipoMadera": "eucalipto",
  "usoMadera": "soporte_galeria",
  "cantidadSolicitada": 10,
  "unidad": "m3",
  "mina": "Cerro Verde",
  "areaSolicitante": "Operaciones",
  "solicitadoPor": "juan.perez"
}
```

**Respuesta esperada:** `201 Created` con estado `PENDIENTE`.  
**Verificar en RabbitMQ:** El panel `http://localhost:15672` debe mostrar 1 mensaje en la cola `cola.pedidos.creados`.

---

### Prueba 2 — Crear un pedido sin stock suficiente

```
POST http://localhost:8083/api/pedidos
Content-Type: application/json
```

```json
{
  "maderaId": 1,
  "tipoMadera": "eucalipto",
  "cantidadSolicitada": 9999,
  "mina": "Cerro Verde",
  "solicitadoPor": "juan.perez"
}
```

**Respuesta esperada:** `400 Bad Request` con mensaje de stock insuficiente.

---

### Prueba 3 — Listar pedidos

```
GET http://localhost:8083/api/pedidos
```

**Respuesta esperada:** `200 OK` con el pedido de la Prueba 1 en estado `PENDIENTE`.

---

### Prueba 4 — Aprobar el pedido

```
PUT http://localhost:8083/api/pedidos/1/aprobar?aprobadoPor=almacen.garcia
```

**Respuesta esperada:** `200 OK` con estado `APROBADO`.  
**Verificar en ms-inventario:** `GET http://localhost:8082/api/inventario/1/stock` debe mostrar el stock reducido en 10.

---

### Prueba 5 — Rechazar un pedido

```
PUT http://localhost:8083/api/pedidos/1/rechazar?motivo=Tipo de madera no disponible para esa galería
```

**Respuesta esperada:** `200 OK` con estado `RECHAZADO` y el motivo registrado.

---

### Prueba 6 — Avanzar estado a DESPACHADO

```
PUT http://localhost:8083/api/pedidos/1/estado?nuevoEstado=DESPACHADO
```

**Respuesta esperada:** `200 OK` con estado `DESPACHADO`.

---

### Prueba 7 — Filtrar pedidos por mina

```
GET http://localhost:8083/api/pedidos/mina/Cerro Verde
```

**Respuesta esperada:** `200 OK` con todos los pedidos de Cerro Verde.

---

## Checklist final antes de avanzar a ms-notificaciones

- [ ] La aplicación arranca en el puerto `8083` sin errores
- [ ] Las tablas `pedido` e `historial_estado` existen en `pedidos_db`
- [ ] `POST /api/pedidos` con stock suficiente devuelve `201` en estado `PENDIENTE`
- [ ] `POST /api/pedidos` sin stock devuelve `400` con mensaje claro
- [ ] El evento aparece en la cola `cola.pedidos.creados` en RabbitMQ (`localhost:15672`)
- [ ] `PUT /api/pedidos/{id}/aprobar` cambia estado y reduce stock en ms-inventario
- [ ] `PUT /api/pedidos/{id}/rechazar` registra el motivo
- [ ] `GET /api/pedidos/estado/PENDIENTE` filtra correctamente
- [ ] Pedido inexistente devuelve `404` con JSON de error

---

## Qué viene después

Una vez completado `ms-pedidos`, el siguiente paso es **ms-notificaciones** (Unidad 1 — Evaluación T1).

Este microservicio:
1. Escucha la cola `cola.pedidos.creados` en RabbitMQ con `@RabbitListener`
2. Procesa el evento `PedidoEvent` publicado por ms-pedidos
3. Registra la notificación en su propia base de datos
4. En versión avanzada: envía email con `JavaMailSender`

Con ms-inventario + ms-pedidos + ms-notificaciones completados, tienes la **Evaluación T1 cubierta** (comunicación sincrónica con Feign y asíncrona con RabbitMQ).

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
