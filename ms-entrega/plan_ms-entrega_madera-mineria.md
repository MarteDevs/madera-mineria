# Plan Técnico — ms-entrega
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-entrega · Puerto `8086`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`entrega_db`)  
> **Depende de:** ms-pedidos (Feign + RabbitMQ)

---

## ¿Qué hace este microservicio?

Gestiona el **despacho físico** de madera desde el almacén hacia la mina. Una vez que ms-pedidos aprueba un pedido, ms-entrega toma el control: asigna transportista, registra la ruta y hace seguimiento hasta la confirmación de recepción en la mina.

| Responsabilidad | Descripción |
|---|---|
| Crear entrega | Al recibir evento de pedido aprobado, crea automáticamente una entrega |
| Asignar transportista | Vincula un transportista y vehículo a la entrega |
| Registrar ruta | Almacén de origen → mina de destino con distancia estimada |
| Seguimiento de estados | PENDIENTE → PREPARANDO → EN_RUTA → ENTREGADO |
| Confirmar recepción | La mina confirma que recibió la madera |
| Historial | Registro de cada cambio de estado con fecha y responsable |

---

## Flujo de la entrega

```
ms-pedidos aprueba un pedido
        │
        │  publica evento "pedido.aprobado" en RabbitMQ
        ▼
ms-entrega recibe el evento (listener)
        │
        ▼
Crea entrega en estado PENDIENTE
        │
        ▼
Almacén asigna transportista y vehículo
        │
        ▼
Estado: PREPARANDO (alistando la madera)
        │
        ▼
Transportista sale con la carga
        │
        ▼
Estado: EN_RUTA
        │
        ▼
Mina confirma recepción
        │
        ▼
Estado: ENTREGADO ✓
        │
        ▼
ms-entrega notifica a ms-pedidos (Feign)
para actualizar el estado del pedido a ENTREGADO
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST de gestión de entregas |
| Spring Data JPA | Persistencia en PostgreSQL |
| PostgreSQL Driver | Conexión a `entrega_db` |
| Lombok | Getters, setters y constructores automáticos |
| Validation | Validar datos de entrada |
| DevTools | Recarga automática |
| **OpenFeign** | Notificar a ms-pedidos cuando la entrega se completa |
| **Spring for RabbitMQ** | Escuchar eventos de pedidos aprobados |

---

## Estructura de archivos

```
ms-entrega/src/main/java/com/madera/entrega/
│
├── MsEntregaApplication.java             ← @EnableFeignClients aquí
│
├── model/
│   ├── Entrega.java                      ← entidad principal (crear PRIMERO)
│   ├── EstadoEntrega.java               ← enum de estados
│   └── HistorialEntrega.java            ← registro de cambios de estado
│
├── dto/
│   ├── PedidoAprobadoEvent.java         ← evento que llega de RabbitMQ
│   ├── AsignarTransportistaRequest.java ← datos del transportista
│   ├── EntregaResponse.java             ← lo que devuelven los endpoints
│   └── ConfirmarRecepcionRequest.java   ← datos de confirmación en mina
│
├── repository/
│   └── EntregaRepository.java
│
├── client/
│   ├── PedidosClient.java               ← Feign hacia ms-pedidos
│   └── PedidosClientFallback.java       ← fallback si ms-pedidos no responde
│
├── config/
│   └── RabbitMQConfig.java              ← nueva cola para pedidos aprobados
│
├── listener/
│   └── PedidoAprobadoListener.java      ← @RabbitListener aquí
│
├── service/
│   ├── EntregaService.java              ← interfaz
│   └── EntregaServiceImpl.java          ← lógica de negocio
│
├── controller/
│   └── EntregaController.java           ← endpoints REST
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → client → config → listener → service → controller → exception

---

## Entidades de la base de datos

### Enum: `EstadoEntrega`

```java
public enum EstadoEntrega {
    PENDIENTE,      // entrega creada, sin transportista asignado
    PREPARANDO,     // almacén está alistando la madera
    EN_RUTA,        // transportista salió hacia la mina
    ENTREGADO,      // mina confirmó recepción
    FALLIDO         // no se pudo completar la entrega
}
```

### Entidad: `Entrega`

```java
@Entity
@Table(name = "entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia al pedido en ms-pedidos
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String unidad;

    // Origen y destino
    private String almacenOrigen;        // "Almacén Central Lima"
    private String minaDestino;          // "Cerro Verde", "Antamina"
    private String direccionMina;        // dirección física de la mina
    private Double distanciaKm;          // distancia estimada en km

    // Transportista
    private String transportista;        // nombre del conductor
    private String vehiculo;             // placa del camión
    private String telefTransportista;   // contacto del transportista

    // Estado y fechas
    @Enumerated(EnumType.STRING)
    private EstadoEntrega estado;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaSalida;        // cuando salió del almacén
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;   // cuando llegó a la mina

    // Confirmación
    private String recibidoPor;          // nombre del responsable en la mina
    private String observaciones;        // notas de la entrega
    private Boolean conformidad;         // true = conforme, false = con observaciones

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
```

### Entidad: `HistorialEntrega`

```java
@Entity
@Table(name = "historial_entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "entrega_id")
    private Entrega entrega;

    @Enumerated(EnumType.STRING)
    private EstadoEntrega estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoEntrega estadoNuevo;

    private String responsable;
    private String motivo;

    @CreationTimestamp
    private LocalDateTime fecha;
}
```

---

## DTOs

### `PedidoAprobadoEvent.java` (llega desde RabbitMQ)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoAprobadoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String unidad;
    private String mina;
    private String solicitadoPor;
    private LocalDateTime fecha;
}
```

### `AsignarTransportistaRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarTransportistaRequest {

    @NotBlank(message = "El transportista es obligatorio")
    private String transportista;

    @NotBlank(message = "La placa del vehículo es obligatoria")
    private String vehiculo;

    private String telefTransportista;
    private String almacenOrigen;
    private String direccionMina;
    private Double distanciaKm;
    private LocalDateTime fechaEntregaEstimada;
}
```

### `ConfirmarRecepcionRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarRecepcionRequest {

    @NotBlank(message = "El nombre del receptor es obligatorio")
    private String recibidoPor;

    private Boolean conformidad;         // true = conforme
    private String observaciones;        // si hay observaciones
}
```

### `EntregaResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaResponse {
    private Long id;
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String minaDestino;
    private String transportista;
    private String vehiculo;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
    private String recibidoPor;
    private Boolean conformidad;
    private String observaciones;
}
```

---

## Feign Client — comunicación con ms-pedidos

### `PedidosClient.java`

```java
@FeignClient(
    name = "ms-pedidos",
    url = "http://localhost:8083",
    fallback = PedidosClientFallback.class
)
public interface PedidosClient {

    @PutMapping("/api/pedidos/{id}/estado")
    void actualizarEstadoPedido(
        @PathVariable Long id,
        @RequestParam String nuevoEstado);
}
```

### `PedidosClientFallback.java`

```java
@Component
@Slf4j
public class PedidosClientFallback implements PedidosClient {

    @Override
    public void actualizarEstadoPedido(Long id, String nuevoEstado) {
        log.warn("ms-pedidos no disponible. " +
            "No se pudo actualizar estado del pedido #{} a {}",
            id, nuevoEstado);
    }
}
```

---

## Configuración RabbitMQ

### `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

    // Cola existente (compartida con ms-notificaciones)
    public static final String EXCHANGE         = "madera.exchange";

    // Nueva cola exclusiva para ms-entrega
    public static final String COLA_APROBADOS   = "cola.pedidos.aprobados";
    public static final String ROUTING_APROBADO = "pedido.aprobado";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaPedidosAprobados() {
        return new Queue(COLA_APROBADOS, true);
    }

    @Bean
    public Binding bindingAprobados(
            Queue colaPedidosAprobados,
            TopicExchange maderaExchange) {
        return BindingBuilder
            .bind(colaPedidosAprobados)
            .to(maderaExchange)
            .with(ROUTING_APROBADO);
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

> **Importante:** en ms-pedidos debes agregar la publicación del evento `pedido.aprobado` cuando se aprueba un pedido, igual que publicas `pedido.creado`. ms-entrega escucha esa cola.

---

## Listener

### `PedidoAprobadoListener.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoAprobadoListener {

    private final EntregaService entregaService;

    @RabbitListener(queues = RabbitMQConfig.COLA_APROBADOS)
    public void procesarPedidoAprobado(PedidoAprobadoEvent evento) {

        log.info("Pedido aprobado recibido: #{} - {} unidades de {} para {}",
            evento.getPedidoId(),
            evento.getCantidad(),
            evento.getTipoMadera(),
            evento.getMina());

        try {
            entregaService.crearEntregaDesdeEvento(evento);
            log.info("Entrega creada para pedido #{}", evento.getPedidoId());
        } catch (Exception e) {
            log.error("Error creando entrega para pedido #{}: {}",
                evento.getPedidoId(), e.getMessage());
            throw e;
        }
    }
}
```

---

## Repositorio

### `EntregaRepository.java`

```java
@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    Optional<Entrega> findByPedidoId(Long pedidoId);
    List<Entrega> findByEstado(EstadoEntrega estado);
    List<Entrega> findByMinaDestino(String mina);
    List<Entrega> findByTransportista(String transportista);
    List<Entrega> findByMinaDestinoAndEstado(
        String mina, EstadoEntrega estado);

    // Entregas en tránsito (para monitoreo)
    List<Entrega> findByEstadoIn(List<EstadoEntrega> estados);
}
```

---

## Lógica de negocio

### `EntregaServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository  entregaRepo;
    private final PedidosClient      pedidosClient;

    // ─── CREAR ENTREGA DESDE EVENTO RABBITMQ ─────────────────────────────────
    @Transactional
    public void crearEntregaDesdeEvento(PedidoAprobadoEvent evento) {

        // Verificar que no existe ya una entrega para este pedido
        if (entregaRepo.findByPedidoId(evento.getPedidoId()).isPresent()) {
            log.warn("Ya existe una entrega para el pedido #{}",
                evento.getPedidoId());
            return;
        }

        Entrega entrega = Entrega.builder()
            .pedidoId(evento.getPedidoId())
            .tipoMadera(evento.getTipoMadera())
            .cantidad(evento.getCantidad())
            .unidad(evento.getUnidad())
            .minaDestino(evento.getMina())
            .almacenOrigen("Almacén Central")
            .estado(EstadoEntrega.PENDIENTE)
            .fechaEntregaEstimada(LocalDateTime.now().plusDays(2))
            .build();

        entregaRepo.save(entrega);
    }

    // ─── ASIGNAR TRANSPORTISTA ───────────────────────────────────────────────
    @Transactional
    public EntregaResponse asignarTransportista(
            Long id, AsignarTransportistaRequest request) {

        Entrega entrega = obtenerEntidadPorId(id);

        if (entrega.getEstado() != EstadoEntrega.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se puede asignar transportista a entregas PENDIENTES");
        }

        entrega.setTransportista(request.getTransportista());
        entrega.setVehiculo(request.getVehiculo());
        entrega.setTelefTransportista(request.getTelefTransportista());
        entrega.setAlmacenOrigen(request.getAlmacenOrigen());
        entrega.setDireccionMina(request.getDireccionMina());
        entrega.setDistanciaKm(request.getDistanciaKm());
        entrega.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        entrega.setEstado(EstadoEntrega.PREPARANDO);

        entregaRepo.save(entrega);
        return mapToResponse(entrega);
    }

    // ─── MARCAR EN RUTA ──────────────────────────────────────────────────────
    @Transactional
    public EntregaResponse marcarEnRuta(Long id) {

        Entrega entrega = obtenerEntidadPorId(id);

        if (entrega.getEstado() != EstadoEntrega.PREPARANDO) {
            throw new IllegalStateException(
                "La entrega debe estar en estado PREPARANDO para salir");
        }

        entrega.setEstado(EstadoEntrega.EN_RUTA);
        entrega.setFechaSalida(LocalDateTime.now());
        entregaRepo.save(entrega);

        return mapToResponse(entrega);
    }

    // ─── CONFIRMAR RECEPCIÓN ─────────────────────────────────────────────────
    @Transactional
    public EntregaResponse confirmarRecepcion(
            Long id, ConfirmarRecepcionRequest request) {

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

        // Notificar a ms-pedidos que el pedido fue entregado (Feign)
        pedidosClient.actualizarEstadoPedido(
            entrega.getPedidoId(), "ENTREGADO");

        return mapToResponse(entrega);
    }

    // ─── MARCAR COMO FALLIDO ─────────────────────────────────────────────────
    @Transactional
    public EntregaResponse marcarFallido(Long id, String motivo) {

        Entrega entrega = obtenerEntidadPorId(id);
        entrega.setEstado(EstadoEntrega.FALLIDO);
        entrega.setObservaciones(motivo);
        entregaRepo.save(entrega);

        return mapToResponse(entrega);
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    public List<EntregaResponse> listarTodas() {
        return entregaRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public EntregaResponse obtenerPorId(Long id) {
        return mapToResponse(obtenerEntidadPorId(id));
    }

    public EntregaResponse obtenerPorPedidoId(Long pedidoId) {
        Entrega entrega = entregaRepo.findByPedidoId(pedidoId)
            .orElseThrow(() -> new RuntimeException(
                "No hay entrega para el pedido #" + pedidoId));
        return mapToResponse(entrega);
    }

    public List<EntregaResponse> listarPorEstado(EstadoEntrega estado) {
        return entregaRepo.findByEstado(estado).stream()
            .map(this::mapToResponse).toList();
    }

    public List<EntregaResponse> listarPorMina(String mina) {
        return entregaRepo.findByMinaDestino(mina).stream()
            .map(this::mapToResponse).toList();
    }

    public List<EntregaResponse> listarEnTransito() {
        return entregaRepo.findByEstadoIn(
            List.of(EstadoEntrega.PREPARANDO, EstadoEntrega.EN_RUTA)
        ).stream().map(this::mapToResponse).toList();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    private Entrega obtenerEntidadPorId(Long id) {
        return entregaRepo.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Entrega no encontrada: " + id));
    }

    private EntregaResponse mapToResponse(Entrega e) {
        return EntregaResponse.builder()
            .id(e.getId())
            .pedidoId(e.getPedidoId())
            .tipoMadera(e.getTipoMadera())
            .cantidad(e.getCantidad())
            .minaDestino(e.getMinaDestino())
            .transportista(e.getTransportista())
            .vehiculo(e.getVehiculo())
            .estado(e.getEstado().name())
            .fechaCreacion(e.getFechaCreacion())
            .fechaSalida(e.getFechaSalida())
            .fechaEntregaEstimada(e.getFechaEntregaEstimada())
            .fechaEntregaReal(e.getFechaEntregaReal())
            .recibidoPor(e.getRecibidoPor())
            .conformidad(e.getConformidad())
            .observaciones(e.getObservaciones())
            .build();
    }
}
```

---

## Endpoints REST

URL base: `http://localhost:8086/api/entregas`

### `EntregaController.java`

```java
@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @GetMapping
    public ResponseEntity<List<EntregaResponse>> listarTodas() {
        return ResponseEntity.ok(entregaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponse> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.obtenerPorId(id));
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<EntregaResponse> obtenerPorPedido(
            @PathVariable Long pedidoId) {
        return ResponseEntity.ok(entregaService.obtenerPorPedidoId(pedidoId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EntregaResponse>> porEstado(
            @PathVariable EstadoEntrega estado) {
        return ResponseEntity.ok(entregaService.listarPorEstado(estado));
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<EntregaResponse>> porMina(
            @PathVariable String mina) {
        return ResponseEntity.ok(entregaService.listarPorMina(mina));
    }

    @GetMapping("/en-transito")
    public ResponseEntity<List<EntregaResponse>> enTransito() {
        return ResponseEntity.ok(entregaService.listarEnTransito());
    }

    @PutMapping("/{id}/asignar-transportista")
    public ResponseEntity<EntregaResponse> asignarTransportista(
            @PathVariable Long id,
            @RequestBody @Valid AsignarTransportistaRequest request) {
        return ResponseEntity.ok(
            entregaService.asignarTransportista(id, request));
    }

    @PutMapping("/{id}/en-ruta")
    public ResponseEntity<EntregaResponse> marcarEnRuta(
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.marcarEnRuta(id));
    }

    @PutMapping("/{id}/confirmar-recepcion")
    public ResponseEntity<EntregaResponse> confirmarRecepcion(
            @PathVariable Long id,
            @RequestBody @Valid ConfirmarRecepcionRequest request) {
        return ResponseEntity.ok(
            entregaService.confirmarRecepcion(id, request));
    }

    @PutMapping("/{id}/fallido")
    public ResponseEntity<EntregaResponse> marcarFallido(
            @PathVariable Long id,
            @RequestParam String motivo) {
        return ResponseEntity.ok(entregaService.marcarFallido(id, motivo));
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/entregas` | Lista todas las entregas | Admin |
| `GET` | `/api/entregas/{id}` | Detalle de una entrega | Todos |
| `GET` | `/api/entregas/pedido/{pedidoId}` | Entrega de un pedido | Todos |
| `GET` | `/api/entregas/estado/{estado}` | Filtrar por estado | Almacén |
| `GET` | `/api/entregas/mina/{mina}` | Entregas hacia una mina | Compras |
| `GET` | `/api/entregas/en-transito` | Entregas activas | Admin / Transporte |
| `PUT` | `/api/entregas/{id}/asignar-transportista` | Asignar conductor y vehículo | Almacén |
| `PUT` | `/api/entregas/{id}/en-ruta` | Marcar que salió del almacén | Transporte |
| `PUT` | `/api/entregas/{id}/confirmar-recepcion` | Mina confirma recepción | Compras |
| `PUT` | `/api/entregas/{id}/fallido` | Marcar entrega fallida | Admin |

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(
            RuntimeException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "Recurso no encontrado",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleEstado(
            IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Cambio de estado inválido",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Datos inválidos",
            "mensaje", mensaje
        ));
    }
}
```

---

## Ajuste en ms-pedidos — publicar evento al aprobar

Para que ms-entrega reciba el evento, debes agregar la publicación en `PedidoServiceImpl.java` dentro del método `aprobarPedido`:

```java
// Al final del método aprobarPedido en ms-pedidos
PedidoAprobadoEvent eventoAprobado = new PedidoAprobadoEvent(
    pedido.getId(),
    pedido.getTipoMadera(),
    pedido.getCantidadSolicitada(),
    pedido.getUnidad(),
    pedido.getMina(),
    pedido.getSolicitadoPor(),
    LocalDateTime.now()
);

amqpTemplate.convertAndSend(
    "madera.exchange",
    "pedido.aprobado",     // routing key diferente al de creación
    eventoAprobado
);
```

---

## Pasos de implementación

### Paso 1 — Crear `EstadoEntrega.java` ⏱ 5 min

Enum con 5 estados: `PENDIENTE`, `PREPARANDO`, `EN_RUTA`, `ENTREGADO`, `FALLIDO`.

**Verificación:** Compila sin errores.

---

### Paso 2 — Crear `Entrega.java` ⏱ 25 min

Entidad principal con todos los campos. Usar `@Builder` de Lombok para facilitar la creación en el service.

**Verificación:** JPA crea la tabla `entrega` en `entrega_db`. Verificar en pgAdmin.

---

### Paso 3 — Crear `HistorialEntrega.java` ⏱ 10 min

Relación `@ManyToOne` con `Entrega`.

**Verificación:** JPA crea la tabla `historial_entrega`.

---

### Paso 4 — Crear los DTOs ⏱ 20 min

Crear los 4 DTOs en `dto/`. El `PedidoAprobadoEvent` debe tener los mismos campos que el evento publicado por ms-pedidos.

**Verificación:** Compilan sin errores.

---

### Paso 5 — Crear `EntregaRepository.java` ⏱ 10 min

Incluir `findByEstadoIn` para la consulta de entregas en tránsito.

**Verificación:** Compila sin errores.

---

### Paso 6 — Crear `PedidosClient.java` y fallback ⏱ 15 min

Feign apuntando a `http://localhost:8083`. El fallback solo loguea el error.

**Verificación:** La app arranca sin errores aunque ms-pedidos esté apagado.

---

### Paso 7 — Crear `RabbitMQConfig.java` ⏱ 15 min

Nueva cola `cola.pedidos.aprobados` con routing key `pedido.aprobado`.

**Verificación:** La cola aparece en el panel de RabbitMQ `http://localhost:15672`.

---

### Paso 8 — Crear `PedidoAprobadoListener.java` ⏱ 15 min

`@RabbitListener` escuchando `cola.pedidos.aprobados`. Llama al service para crear la entrega automáticamente.

**Verificación:** Al aprobar un pedido en ms-pedidos, el log de ms-entrega muestra el evento recibido.

---

### Paso 9 — Crear `EntregaService` y `EntregaServiceImpl` ⏱ 45 min

Lógica de creación automática, asignación de transportista, estados y confirmación de recepción. Al confirmar recepción, llama a ms-pedidos vía Feign para actualizar el estado del pedido.

**Verificación:** Compila sin errores.

---

### Paso 10 — Crear `EntregaController.java` ⏱ 20 min

Los 10 endpoints. Verificar que los métodos PUT usan las anotaciones correctas.

**Verificación:** `GET http://localhost:8086/api/entregas` devuelve `[]`.

---

### Paso 11 — Actualizar ms-pedidos ⏱ 10 min

Agregar la publicación del evento `pedido.aprobado` en el método `aprobarPedido` de `PedidoServiceImpl`.

**Verificación:** Al aprobar un pedido, ms-entrega crea la entrega automáticamente.

---

### Paso 12 — Crear `GlobalExceptionHandler.java` ⏱ 10 min

Manejo de errores de estado inválido, recurso no encontrado y validaciones.

**Verificación:** Intentar marcar EN_RUTA una entrega PENDIENTE devuelve `400` con mensaje claro.

---

## Pruebas con Postman

> Asegúrate de que ms-inventario (8082), ms-pedidos (8083) y RabbitMQ están corriendo.

### Prueba 1 — Verificar creación automática de entrega

Primero aprobar un pedido en ms-pedidos:

```
PUT http://localhost:8083/api/pedidos/1/aprobar?aprobadoPor=almacen.garcia
```

Luego verificar que ms-entrega creó la entrega automáticamente:

```
GET http://localhost:8086/api/entregas
```

**Respuesta esperada:** `200 OK` con la entrega en estado `PENDIENTE` creada automáticamente por el listener.

---

### Prueba 2 — Asignar transportista

```
PUT http://localhost:8086/api/entregas/1/asignar-transportista
Content-Type: application/json
```

```json
{
  "transportista": "Pedro Quispe",
  "vehiculo": "ABC-123",
  "telefTransportista": "987654321",
  "almacenOrigen": "Almacén Central Lima",
  "direccionMina": "Carretera Arequipa km 48, Cerro Verde",
  "distanciaKm": 48.5,
  "fechaEntregaEstimada": "2026-05-20T08:00:00"
}
```

**Respuesta esperada:** `200 OK` con estado `PREPARANDO` y datos del transportista.

---

### Prueba 3 — Marcar en ruta

```
PUT http://localhost:8086/api/entregas/1/en-ruta
```

**Respuesta esperada:** `200 OK` con estado `EN_RUTA` y `fechaSalida` registrada.

---

### Prueba 4 — Confirmar recepción

```
PUT http://localhost:8086/api/entregas/1/confirmar-recepcion
Content-Type: application/json
```

```json
{
  "recibidoPor": "Juan Perez - Jefe de Operaciones",
  "conformidad": true,
  "observaciones": "Madera recibida en buen estado"
}
```

**Respuesta esperada:** `200 OK` con estado `ENTREGADO` y `fechaEntregaReal` registrada.  
**Verificar en ms-pedidos:** `GET http://localhost:8083/api/pedidos/1` debe mostrar estado `ENTREGADO`.

---

### Prueba 5 — Listar entregas en tránsito

```
GET http://localhost:8086/api/entregas/en-transito
```

**Respuesta esperada:** Lista de entregas en estado `PREPARANDO` o `EN_RUTA`.

---

### Prueba 6 — Probar cambio de estado inválido

```
PUT http://localhost:8086/api/entregas/1/en-ruta
```

(Con la entrega ya en ENTREGADO)

**Respuesta esperada:** `400 Bad Request` con mensaje "La entrega debe estar en estado PREPARANDO para salir".

---

### Prueba 7 — Filtrar por mina

```
GET http://localhost:8086/api/entregas/mina/Cerro Verde
```

**Respuesta esperada:** `200 OK` con todas las entregas hacia Cerro Verde.

---

## Checklist final antes de avanzar a api-gateway

- [ ] La aplicación arranca en el puerto `8086` sin errores
- [ ] Las tablas `entrega` e `historial_entrega` existen en `entrega_db`
- [ ] Al aprobar un pedido en ms-pedidos, ms-entrega crea la entrega automáticamente
- [ ] `PUT /asignar-transportista` cambia estado a `PREPARANDO`
- [ ] `PUT /en-ruta` cambia estado a `EN_RUTA` y registra `fechaSalida`
- [ ] `PUT /confirmar-recepcion` cambia estado a `ENTREGADO` y registra `fechaEntregaReal`
- [ ] Al confirmar recepción, ms-pedidos actualiza el pedido a `ENTREGADO` vía Feign
- [ ] Cambio de estado inválido devuelve `400` con mensaje claro
- [ ] `GET /en-transito` muestra solo entregas `PREPARANDO` y `EN_RUTA`

---

## Flujo completo del sistema hasta este punto

Con los 5 microservicios listos, el flujo de extremo a extremo es:

```
1. Compras crea pedido    → ms-pedidos (POST /api/pedidos)
2. Feign verifica stock   → ms-inventario (GET /{id}/stock)
3. RabbitMQ notifica      → ms-notificaciones (cola.pedidos.creados)
4. Almacén aprueba        → ms-pedidos (PUT /{id}/aprobar)
5. Feign reduce stock     → ms-inventario (PUT /{id}/reducir)
6. RabbitMQ notifica      → ms-entrega (cola.pedidos.aprobados)
7. Entrega creada         → ms-entrega (automático por listener)
8. Transportista asignado → ms-entrega (PUT /asignar-transportista)
9. Sale del almacén       → ms-entrega (PUT /en-ruta)
10. Mina confirma         → ms-entrega (PUT /confirmar-recepcion)
11. Feign actualiza       → ms-pedidos (PUT /{id}/estado → ENTREGADO)
```

---

## Qué viene después

El último microservicio es el **api-gateway** (puerto `8080`). Es el punto de entrada único al sistema:

1. **Enruta** todas las requests al microservicio correcto
2. **Valida tokens JWT** antes de dejar pasar cualquier request
3. **Rate limiting** para evitar abuso de la API
4. **Logging centralizado** de todas las requests

Con el api-gateway completo, el sistema está listo para la **Evaluación Final (EF)**.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
