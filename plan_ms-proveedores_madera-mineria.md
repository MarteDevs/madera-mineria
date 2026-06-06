# Plan Técnico — ms-proveedores
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-proveedores · Puerto `8087`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`proveedores_db`)  
> **Depende de:** ms-inventario (Feign) · RabbitMQ (eventos)

---

## ¿Qué hace este microservicio?

Gestiona los proveedores que suministran madera al sistema. Cuando un proveedor entrega madera al almacén, este microservicio registra la entrega y notifica a `ms-inventario` vía Feign para aumentar el stock automáticamente.

| Responsabilidad | Descripción |
|---|---|
| Registrar proveedores | Empresas que suministran madera (razón social, RUC, contacto) |
| Gestionar contratos | Contrato vigente por proveedor con tipo de madera y precio pactado |
| Registrar entregas | Cuando llega madera del proveedor, registrar la entrada |
| Actualizar stock | Llama a ms-inventario vía Feign para aumentar el stock |
| Publicar eventos | Publica evento en RabbitMQ cuando llega nueva madera |
| Historial | Registro de todas las entregas por proveedor y período |

---

## Flujo principal

```
Proveedor llega con madera al almacén
            │
            ▼
POST /api/proveedores/{id}/entregas
            │
            ▼
ms-proveedores registra la entrega
            │
            ├──── Feign ────▶ ms-inventario
            │                 PUT /api/inventario/{id}/entrada
            │                 (aumenta el stock)
            │
            └──── RabbitMQ ─▶ ms-notificaciones
                              "Nueva madera disponible en almacén"
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST |
| Spring Data JPA | Persistencia en PostgreSQL |
| PostgreSQL Driver | Conexión a `proveedores_db` |
| Lombok | Getters, setters automáticos |
| Validation | Validar datos de entrada |
| DevTools | Recarga automática |
| **OpenFeign** | Llamar a ms-inventario para registrar entrada de stock |
| **Spring for RabbitMQ** | Publicar evento cuando llega nueva madera |
| **Eureka Client** | Registrarse en Eureka para descubrimiento de servicios |

---

## Estructura de archivos

```
ms-proveedores/src/main/java/com/madera/proveedores/
│
├── MsProveedoresApplication.java          ← @EnableFeignClients aquí
│
├── model/
│   ├── Proveedor.java                     ← entidad principal (crear PRIMERO)
│   ├── Contrato.java                      ← contrato vigente con el proveedor
│   ├── EntregaProveedor.java             ← registro de cada entrega de madera
│   └── EstadoContrato.java               ← enum: VIGENTE / VENCIDO / SUSPENDIDO
│
├── dto/
│   ├── ProveedorRequest.java
│   ├── ProveedorResponse.java
│   ├── ContratoRequest.java
│   ├── ContratoResponse.java
│   ├── EntregaProveedorRequest.java
│   ├── EntregaProveedorResponse.java
│   └── EntregaEvent.java                 ← evento para RabbitMQ
│
├── repository/
│   ├── ProveedorRepository.java
│   ├── ContratoRepository.java
│   └── EntregaProveedorRepository.java
│
├── client/
│   ├── InventarioClient.java             ← Feign hacia ms-inventario
│   └── InventarioClientFallback.java
│
├── config/
│   └── RabbitMQConfig.java
│
├── service/
│   ├── ProveedorService.java
│   ├── ProveedorServiceImpl.java
│   ├── ContratoService.java
│   └── ContratoServiceImpl.java
│
├── controller/
│   ├── ProveedorController.java
│   └── ContratoController.java
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → client → config → service → controller → exception

---

## Configuración

### `application.properties`

```properties
spring.application.name=ms-proveedores
server.port=8087

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/proveedores_db
spring.datasource.username=postgres
spring.datasource.password=postgres123

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Feign
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=5000

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
eureka.instance.lease-renewal-interval-in-seconds=10

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Resilience4J
resilience4j.circuitbreaker.instances.inventario.sliding-window-size=5
resilience4j.circuitbreaker.instances.inventario.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.inventario.wait-duration-in-open-state=10s
resilience4j.retry.instances.inventario.max-attempts=3
resilience4j.retry.instances.inventario.wait-duration=2s
```

### Crear base de datos

```sql
CREATE DATABASE proveedores_db;
```

### Clase principal

```java
@SpringBootApplication
@EnableFeignClients
public class MsProveedoresApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProveedoresApplication.class, args);
    }
}
```

---

## Entidades de la base de datos

### Enum: `EstadoContrato`

```java
public enum EstadoContrato {
    VIGENTE,      // contrato activo
    VENCIDO,      // plazo expirado
    SUSPENDIDO    // suspendido por incumplimiento
}
```

### Entidad: `Proveedor`

```java
@Entity
@Table(name = "proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String ruc;                    // RUC de la empresa

    @NotBlank
    private String razonSocial;            // nombre legal de la empresa

    private String nombreComercial;        // nombre comercial
    private String contactoNombre;         // nombre del representante
    private String contactoEmail;
    private String contactoTelefono;
    private String direccion;
    private String ciudad;                 // Lima, Arequipa, etc.

    private Boolean activo;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
```

### Entidad: `Contrato`

```java
@Entity
@Table(name = "contrato")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    private String tipoMadera;             // "eucalipto", "pino", "roble"
    private String unidad;                 // "m3", "metro_lineal"
    private Double precioPactado;          // precio por unidad en soles
    private Integer cantidadMinima;        // cantidad mínima por entrega
    private Integer cantidadMaxima;        // cantidad máxima por entrega

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoContrato estado;

    private String observaciones;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;
}
```

### Entidad: `EntregaProveedor`

```java
@Entity
@Table(name = "entrega_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    private Long maderaId;                 // ID en ms-inventario
    private String tipoMadera;
    private Integer cantidadEntregada;
    private String unidad;
    private Double precioUnitario;
    private Double montoTotal;             // calculado automáticamente

    private String guiaRemision;           // número de guía de remisión
    private String responsableRecepcion;   // quien recibió la madera

    private Boolean stockActualizado;      // si Feign fue exitoso

    @CreationTimestamp
    private LocalDateTime fechaEntrega;
}
```

---

## Feign Client

### `InventarioClient.java`

```java
@FeignClient(
    name = "ms-inventario",               // Eureka resuelve la URL
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {

    // Consultar si existe la madera
    @GetMapping("/api/inventario/{id}")
    Object obtenerMadera(@PathVariable Long id);

    // Registrar entrada de stock
    @PostMapping("/api/inventario/{id}/entrada")
    void registrarEntrada(
        @PathVariable Long id,
        @RequestParam Integer cantidad,
        @RequestParam String motivo
    );

    // Consultar stock actual
    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable Long id);
}
```

### `InventarioClientFallback.java`

```java
@Component
@Slf4j
public class InventarioClientFallback implements InventarioClient {

    @Override
    public Object obtenerMadera(Long id) {
        log.warn("ms-inventario no disponible. No se pudo obtener madera #{}", id);
        return null;
    }

    @Override
    public void registrarEntrada(Long id, Integer cantidad, String motivo) {
        log.error("ms-inventario no disponible. " +
            "Stock NO actualizado para madera #{}: {} unidades", id, cantidad);
    }

    @Override
    public Integer consultarStock(Long id) {
        return -1;
    }
}
```

---

## Configuración RabbitMQ

### `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

    // Reutiliza el exchange existente del sistema
    public static final String EXCHANGE         = "madera.exchange";
    public static final String COLA_PROVEEDORES = "cola.entregas.proveedor";
    public static final String ROUTING_PROVEEDOR= "entrega.proveedor";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaEntregasProveedor() {
        return new Queue(COLA_PROVEEDORES, true);
    }

    @Bean
    public Binding bindingProveedor(
            Queue colaEntregasProveedor,
            TopicExchange maderaExchange) {
        return BindingBuilder
            .bind(colaEntregasProveedor)
            .to(maderaExchange)
            .with(ROUTING_PROVEEDOR);
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

## Repositorios

### `ProveedorRepository.java`

```java
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
    List<Proveedor> findByActivo(Boolean activo);
    List<Proveedor> findByRazonSocialContainingIgnoreCase(String nombre);
}
```

### `ContratoRepository.java`

```java
@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByProveedorId(Long proveedorId);
    List<Contrato> findByEstado(EstadoContrato estado);
    List<Contrato> findByTipoMadera(String tipoMadera);
    List<Contrato> findByProveedorIdAndEstado(Long proveedorId, EstadoContrato estado);

    // Verificar contratos próximos a vencer (en los próximos 30 días)
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'VIGENTE' " +
           "AND c.fechaFin BETWEEN :hoy AND :limite")
    List<Contrato> findContratosProximosAVencer(
        @Param("hoy") LocalDate hoy,
        @Param("limite") LocalDate limite);
}
```

### `EntregaProveedorRepository.java`

```java
@Repository
public interface EntregaProveedorRepository
        extends JpaRepository<EntregaProveedor, Long> {

    List<EntregaProveedor> findByProveedorId(Long proveedorId);
    List<EntregaProveedor> findByTipoMadera(String tipoMadera);
    List<EntregaProveedor> findByFechaEntregaBetween(
        LocalDateTime inicio, LocalDateTime fin);
    List<EntregaProveedor> findByStockActualizado(Boolean actualizado);

    // Total entregado por proveedor
    @Query("SELECT SUM(e.cantidadEntregada) FROM EntregaProveedor e " +
           "WHERE e.proveedor.id = :proveedorId")
    Integer totalEntregadoPorProveedor(@Param("proveedorId") Long proveedorId);
}
```

---

## Lógica de negocio

### `ProveedorServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository         proveedorRepo;
    private final ContratoRepository          contratoRepo;
    private final EntregaProveedorRepository  entregaRepo;
    private final InventarioClient            inventarioClient;
    private final AmqpTemplate                amqpTemplate;

    // ─── CREAR PROVEEDOR ─────────────────────────────────────────────────────
    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest request) {

        if (proveedorRepo.existsByRuc(request.getRuc())) {
            throw new RuntimeException(
                "Ya existe un proveedor con el RUC: " + request.getRuc());
        }

        Proveedor proveedor = Proveedor.builder()
            .ruc(request.getRuc())
            .razonSocial(request.getRazonSocial())
            .nombreComercial(request.getNombreComercial())
            .contactoNombre(request.getContactoNombre())
            .contactoEmail(request.getContactoEmail())
            .contactoTelefono(request.getContactoTelefono())
            .direccion(request.getDireccion())
            .ciudad(request.getCiudad())
            .activo(true)
            .build();

        proveedorRepo.save(proveedor);
        return mapToResponse(proveedor);
    }

    // ─── REGISTRAR ENTREGA ───────────────────────────────────────────────────
    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackRegistrarEntrega")
    @Retry(name = "inventario")
    public EntregaProveedorResponse registrarEntrega(
            Long proveedorId, EntregaProveedorRequest request) {

        // 1. Verificar que el proveedor existe
        Proveedor proveedor = proveedorRepo.findById(proveedorId)
            .orElseThrow(() -> new RuntimeException(
                "Proveedor no encontrado: " + proveedorId));

        // 2. Verificar contrato vigente
        Contrato contrato = contratoRepo
            .findByProveedorIdAndEstado(proveedorId, EstadoContrato.VIGENTE)
            .stream()
            .filter(c -> c.getTipoMadera().equals(request.getTipoMadera()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No hay contrato vigente para el tipo de madera: " +
                request.getTipoMadera()));

        // 3. Calcular monto total
        double montoTotal = contrato.getPrecioPactado()
            * request.getCantidadEntregada();

        // 4. Crear registro de entrega
        EntregaProveedor entrega = EntregaProveedor.builder()
            .proveedor(proveedor)
            .contrato(contrato)
            .maderaId(request.getMaderaId())
            .tipoMadera(request.getTipoMadera())
            .cantidadEntregada(request.getCantidadEntregada())
            .unidad(request.getUnidad())
            .precioUnitario(contrato.getPrecioPactado())
            .montoTotal(montoTotal)
            .guiaRemision(request.getGuiaRemision())
            .responsableRecepcion(request.getResponsableRecepcion())
            .stockActualizado(false)
            .build();

        entregaRepo.save(entrega);

        // 5. Actualizar stock en ms-inventario vía Feign
        inventarioClient.registrarEntrada(
            request.getMaderaId(),
            request.getCantidadEntregada(),
            "Entrega proveedor: " + proveedor.getRazonSocial() +
            " | Guía: " + request.getGuiaRemision()
        );

        entrega.setStockActualizado(true);
        entregaRepo.save(entrega);

        // 6. Publicar evento en RabbitMQ
        EntregaEvent evento = new EntregaEvent(
            entrega.getId(),
            proveedor.getRazonSocial(),
            request.getTipoMadera(),
            request.getCantidadEntregada(),
            LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_PROVEEDOR,
            evento
        );

        log.info("Entrega registrada: {} unidades de {} del proveedor {}",
            request.getCantidadEntregada(),
            request.getTipoMadera(),
            proveedor.getRazonSocial());

        return mapEntregaToResponse(entrega);
    }

    // ─── FALLBACK ────────────────────────────────────────────────────────────
    public EntregaProveedorResponse fallbackRegistrarEntrega(
            Long proveedorId, EntregaProveedorRequest request, Exception ex) {

        log.error("Circuit Breaker activo. ms-inventario no disponible. " +
            "Entrega registrada pero stock NO actualizado.");

        // La entrega se guarda igual, pero stock no se actualiza
        // Se puede reprocesar después
        throw new RuntimeException(
            "La entrega fue registrada pero el stock no pudo actualizarse. " +
            "Contacte al administrador.");
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    public List<ProveedorResponse> listarTodos() {
        return proveedorRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public List<ProveedorResponse> listarActivos() {
        return proveedorRepo.findByActivo(true).stream()
            .map(this::mapToResponse).toList();
    }

    public ProveedorResponse obtenerPorId(Long id) {
        return mapToResponse(proveedorRepo.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Proveedor no encontrado: " + id)));
    }

    public List<EntregaProveedorResponse> entregasPorProveedor(Long id) {
        return entregaRepo.findByProveedorId(id).stream()
            .map(this::mapEntregaToResponse).toList();
    }

    public List<ContratoResponse> contratosPorProveedor(Long id) {
        return contratoRepo.findByProveedorId(id).stream()
            .map(this::mapContratoToResponse).toList();
    }

    public List<ContratoResponse> contratosProximosAVencer() {
        return contratoRepo.findContratosProximosAVencer(
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        ).stream().map(this::mapContratoToResponse).toList();
    }

    // ─── MAPPERS ─────────────────────────────────────────────────────────────
    private ProveedorResponse mapToResponse(Proveedor p) {
        ProveedorResponse r = new ProveedorResponse();
        r.setId(p.getId());
        r.setRuc(p.getRuc());
        r.setRazonSocial(p.getRazonSocial());
        r.setNombreComercial(p.getNombreComercial());
        r.setContactoNombre(p.getContactoNombre());
        r.setContactoEmail(p.getContactoEmail());
        r.setContactoTelefono(p.getContactoTelefono());
        r.setCiudad(p.getCiudad());
        r.setActivo(p.getActivo());
        return r;
    }

    private EntregaProveedorResponse mapEntregaToResponse(EntregaProveedor e) {
        EntregaProveedorResponse r = new EntregaProveedorResponse();
        r.setId(e.getId());
        r.setProveedorNombre(e.getProveedor().getRazonSocial());
        r.setTipoMadera(e.getTipoMadera());
        r.setCantidadEntregada(e.getCantidadEntregada());
        r.setUnidad(e.getUnidad());
        r.setPrecioUnitario(e.getPrecioUnitario());
        r.setMontoTotal(e.getMontoTotal());
        r.setGuiaRemision(e.getGuiaRemision());
        r.setStockActualizado(e.getStockActualizado());
        r.setFechaEntrega(e.getFechaEntrega());
        return r;
    }

    private ContratoResponse mapContratoToResponse(Contrato c) {
        ContratoResponse r = new ContratoResponse();
        r.setId(c.getId());
        r.setProveedorNombre(c.getProveedor().getRazonSocial());
        r.setTipoMadera(c.getTipoMadera());
        r.setPrecioPactado(c.getPrecioPactado());
        r.setCantidadMinima(c.getCantidadMinima());
        r.setCantidadMaxima(c.getCantidadMaxima());
        r.setFechaInicio(c.getFechaInicio());
        r.setFechaFin(c.getFechaFin());
        r.setEstado(c.getEstado().name());
        return r;
    }
}
```

---

## Endpoints REST

### `ProveedorController.java`

```java
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listarTodos() {
        return ResponseEntity.ok(proveedorService.listarTodos());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(
            @RequestBody @Valid ProveedorRequest request) {
        return ResponseEntity.status(201)
            .body(proveedorService.crearProveedor(request));
    }

    @GetMapping("/{id}/entregas")
    public ResponseEntity<List<EntregaProveedorResponse>> entregas(
            @PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.entregasPorProveedor(id));
    }

    @PostMapping("/{id}/entregas")
    public ResponseEntity<EntregaProveedorResponse> registrarEntrega(
            @PathVariable Long id,
            @RequestBody @Valid EntregaProveedorRequest request) {
        return ResponseEntity.status(201)
            .body(proveedorService.registrarEntrega(id, request));
    }

    @GetMapping("/{id}/contratos")
    public ResponseEntity<List<ContratoResponse>> contratos(
            @PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.contratosPorProveedor(id));
    }

    @GetMapping("/contratos/proximos-a-vencer")
    public ResponseEntity<List<ContratoResponse>> proximosAVencer() {
        return ResponseEntity.ok(proveedorService.contratosProximosAVencer());
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/proveedores` | Lista todos los proveedores | Admin, Almacén |
| `GET` | `/api/proveedores/activos` | Solo proveedores activos | Admin, Almacén |
| `GET` | `/api/proveedores/{id}` | Detalle de un proveedor | Admin, Almacén |
| `POST` | `/api/proveedores` | Registrar nuevo proveedor | Admin |
| `GET` | `/api/proveedores/{id}/entregas` | Historial de entregas | Admin, Almacén |
| `POST` | `/api/proveedores/{id}/entregas` | Registrar entrega de madera | Almacén |
| `GET` | `/api/proveedores/{id}/contratos` | Contratos del proveedor | Admin |
| `GET` | `/api/proveedores/contratos/proximos-a-vencer` | Alertas de contratos | Admin |

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Error en la solicitud",
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
            "error", "Error interno",
            "mensaje", ex.getMessage()
        ));
    }
}
```

---

## Pasos de implementación

### Paso 1 — Configurar Spring Initializr ⏱ 10 min

Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation, DevTools, OpenFeign, RabbitMQ, Eureka Client, Actuator.

**Verificación:** App arranca en puerto `8087`.

---

### Paso 2 — Crear `EstadoContrato.java` ⏱ 5 min

Enum con 3 valores: `VIGENTE`, `VENCIDO`, `SUSPENDIDO`.

**Verificación:** Compila sin errores.

---

### Paso 3 — Crear entidades ⏱ 25 min

Crear en orden: `Proveedor` → `Contrato` → `EntregaProveedor`.

**Verificación:** JPA crea las tablas `proveedor`, `contrato` y `entrega_proveedor` en `proveedores_db`.

---

### Paso 4 — Crear los DTOs ⏱ 20 min

Crear todos los Request, Response y el `EntregaEvent` para RabbitMQ.

**Verificación:** Compilan sin errores.

---

### Paso 5 — Crear repositorios ⏱ 15 min

Los 3 repositorios con sus métodos de consulta. El `ContratoRepository` incluye la query JPQL para contratos próximos a vencer.

**Verificación:** Compilan sin errores.

---

### Paso 6 — Crear `InventarioClient` y fallback ⏱ 15 min

Feign apuntando a `ms-inventario` por nombre (Eureka resuelve). El fallback loguea el error.

**Verificación:** App arranca sin errores aunque ms-inventario esté apagado.

---

### Paso 7 — Crear `RabbitMQConfig.java` ⏱ 10 min

Nueva cola `cola.entregas.proveedor` con routing key `entrega.proveedor` en el mismo exchange del sistema.

**Verificación:** Cola visible en `http://localhost:15672`.

---

### Paso 8 — Crear `ProveedorService` y `ProveedorServiceImpl` ⏱ 45 min

Lógica completa: crear proveedor, registrar entrega con Circuit Breaker, llamar a Feign, publicar evento RabbitMQ.

**Verificación:** Compila sin errores.

---

### Paso 9 — Crear `ProveedorController` y `ContratoController` ⏱ 20 min

Los 8 endpoints REST.

**Verificación:** `GET http://localhost:8087/api/proveedores` devuelve `[]`.

---

### Paso 10 — Crear `GlobalExceptionHandler` ⏱ 10 min

**Verificación:** RUC duplicado devuelve `400` con mensaje claro.

---

### Paso 11 — Agregar al api-gateway ⏱ 5 min

Agregar ruta en `application.properties` del gateway:

```properties
spring.cloud.gateway.routes[5].id=ms-proveedores
spring.cloud.gateway.routes[5].uri=lb://ms-proveedores
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/proveedores/**
```

**Verificación:** `GET http://localhost:8080/api/proveedores` con token JWT funciona.

---

### Paso 12 — Agregar Dockerfile ⏱ 5 min

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Paso 13 — Agregar al docker-compose.yml ⏱ 5 min

```yaml
  ms-proveedores:
    build: ./ms-proveedores
    container_name: ms-proveedores
    ports:
      - "8087:8087"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/proveedores_db
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
```

También agregar `proveedores_db` al `init-db.sql`:

```sql
CREATE DATABASE proveedores_db;
```

---

## Pruebas con Postman

> Asegúrate de tener ms-inventario (8082) y RabbitMQ corriendo.

### Prueba 1 — Registrar proveedor

```
POST http://localhost:8080/api/proveedores
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "ruc": "20123456789",
  "razonSocial": "Maderería del Sur SAC",
  "nombreComercial": "MaderSur",
  "contactoNombre": "Carlos Huanca",
  "contactoEmail": "carlos@madersur.pe",
  "contactoTelefono": "987654321",
  "direccion": "Av. Industrial 450",
  "ciudad": "Arequipa"
}
```

**Respuesta esperada:** `201 Created` con el proveedor creado.

---

### Prueba 2 — Registrar entrega de madera

```
POST http://localhost:8080/api/proveedores/1/entregas
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "maderaId": 1,
  "tipoMadera": "eucalipto",
  "cantidadEntregada": 50,
  "unidad": "m3",
  "guiaRemision": "GR-001-00123",
  "responsableRecepcion": "Rosa Garcia"
}
```

**Respuesta esperada:** `201 Created` con la entrega registrada.
**Verificar en ms-inventario:** `GET /api/inventario/1/stock` debe aumentar en 50.
**Verificar en RabbitMQ:** Cola `cola.entregas.proveedor` recibió el mensaje.

---

### Prueba 3 — Listar proveedores activos

```
GET http://localhost:8080/api/proveedores/activos
Authorization: Bearer <token>
```

**Respuesta esperada:** `200 OK` con lista de proveedores.

---

### Prueba 4 — Ver contratos próximos a vencer

```
GET http://localhost:8080/api/proveedores/contratos/proximos-a-vencer
Authorization: Bearer <token>
```

**Respuesta esperada:** `200 OK` con contratos que vencen en los próximos 30 días.

---

### Prueba 5 — RUC duplicado

```
POST http://localhost:8080/api/proveedores
Authorization: Bearer <token>
```

Con el mismo RUC de la Prueba 1.

**Respuesta esperada:** `400 Bad Request` con mensaje "Ya existe un proveedor con el RUC: 20123456789".

---

## Checklist final

- [ ] App arranca en puerto `8087`
- [ ] Tablas `proveedor`, `contrato` y `entrega_proveedor` creadas en `proveedores_db`
- [ ] `POST /api/proveedores` crea proveedor correctamente
- [ ] RUC duplicado devuelve `400`
- [ ] `POST /api/proveedores/{id}/entregas` registra entrega
- [ ] Al registrar entrega, stock en ms-inventario aumenta vía Feign
- [ ] Evento publicado en RabbitMQ al registrar entrega
- [ ] `GET /api/proveedores/contratos/proximos-a-vencer` funciona
- [ ] `MS-PROVEEDORES` aparece en dashboard de Eureka `http://localhost:8761`
- [ ] Requests a través del gateway con token funcionan
- [ ] Circuit Breaker actúa si ms-inventario está apagado

---

## Qué viene después

El siguiente microservicio es **ms-reportes** (puerto `8088`). Consulta datos de múltiples microservicios vía Feign y genera resúmenes consolidados — ideal para demostrar comunicación entre muchos servicios en la presentación de la EF.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
