# Plan Técnico — ms-mantenimiento
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-mantenimiento · Puerto `8089`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`mantenimiento_db`)  
> **Depende de:** ms-entrega (RabbitMQ listener) · Eureka Server

---

## ¿Qué hace este microservicio?

Gestiona los vehículos de transporte que despachan madera hacia las minas. Cuando una entrega se completa, ms-entrega publica un evento en RabbitMQ y ms-mantenimiento lo recibe automáticamente para actualizar el kilometraje del vehículo. Si el vehículo supera el límite de kilometraje, genera una alerta de mantenimiento preventivo.

| Responsabilidad | Descripción |
|---|---|
| Registrar vehículos | Camiones y transportistas del sistema de entregas |
| Actualizar kilometraje | Recibe evento de entrega completada vía RabbitMQ |
| Registrar mantenimientos | Historial de servicios de cada vehículo |
| Generar alertas | Vehículos que necesitan mantenimiento por km o por fecha |
| Programar mantenimientos | Agendar próximo servicio preventivo |

---

## Flujo principal

```
ms-entrega confirma entrega
            │
            │  publica evento "entrega.completada"
            │  con placa del vehículo y km recorridos
            ▼
RabbitMQ (cola.entregas.completadas)
            │
            │  @RabbitListener
            ▼
ms-mantenimiento recibe el evento
            │
            ├── Actualiza kilometraje del vehículo
            │
            └── ¿Km acumulados > límite de mantenimiento?
                        │
                   ┌────┴────┐
                   NO        SÍ
                   │         │
                   │         ▼
                   │   Crea alerta de
                   │   mantenimiento
                   │   preventivo
                   ▼
            Registro guardado
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST |
| Spring Data JPA | Persistencia en PostgreSQL |
| PostgreSQL Driver | Conexión a `mantenimiento_db` |
| Lombok | Getters, setters automáticos |
| Validation | Validar datos de entrada |
| DevTools | Recarga automática |
| **Spring for RabbitMQ** | Listener de eventos de entrega completada |
| **Eureka Client** | Registro en Eureka para descubrimiento |
| **Actuator** | Health checks y métricas |

---

## Estructura de archivos

```
ms-mantenimiento/src/main/java/com/madera/mantenimiento/
│
├── MsMantenimientoApplication.java
│
├── model/
│   ├── Vehiculo.java                      ← entidad principal (crear PRIMERO)
│   ├── RegistroMantenimiento.java         ← historial de servicios
│   ├── AlertaMantenimiento.java           ← alertas generadas
│   ├── EstadoVehiculo.java               ← enum: OPERATIVO / EN_MANTENIMIENTO / INACTIVO
│   └── TipoMantenimiento.java            ← enum: PREVENTIVO / CORRECTIVO / REVISION
│
├── dto/
│   ├── EntregaCompletadaEvent.java        ← evento que llega de RabbitMQ
│   ├── VehiculoRequest.java
│   ├── VehiculoResponse.java
│   ├── RegistroMantenimientoRequest.java
│   ├── RegistroMantenimientoResponse.java
│   └── AlertaMantenimientoResponse.java
│
├── repository/
│   ├── VehiculoRepository.java
│   ├── RegistroMantenimientoRepository.java
│   └── AlertaMantenimientoRepository.java
│
├── config/
│   └── RabbitMQConfig.java
│
├── listener/
│   └── EntregaCompletadaListener.java     ← @RabbitListener aquí
│
├── service/
│   ├── MantenimientoService.java
│   └── MantenimientoServiceImpl.java
│
├── controller/
│   └── MantenimientoController.java
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → config → listener → service → controller → exception

---

## Configuración

### `application.properties`

```properties
spring.application.name=ms-mantenimiento
server.port=8089

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/mantenimiento_db
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

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
eureka.instance.lease-renewal-interval-in-seconds=10

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Límite de km para mantenimiento preventivo
mantenimiento.km-limite=5000
mantenimiento.dias-limite=90
```

### Crear base de datos

```sql
CREATE DATABASE mantenimiento_db;
```

### Clase principal

```java
@SpringBootApplication
public class MsMantenimientoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsMantenimientoApplication.class, args);
    }
}
```

---

## Entidades de la base de datos

### Enums

```java
public enum EstadoVehiculo {
    OPERATIVO,          // disponible para entregas
    EN_MANTENIMIENTO,   // en taller
    INACTIVO            // fuera de servicio
}

public enum TipoMantenimiento {
    PREVENTIVO,         // programado por km o tiempo
    CORRECTIVO,         // por falla o avería
    REVISION            // inspección técnica
}
```

### Entidad: `Vehiculo`

```java
@Entity
@Table(name = "vehiculo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String placa;                  // placa del vehículo (ej: ABC-123)

    private String marca;                  // Volvo, Scania, Mercedes
    private String modelo;
    private Integer anio;
    private String tipo;                   // "camion", "camioneta", "furgon"
    private Double capacidadToneladasM3;   // capacidad de carga en m3

    // Conductor asignado
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    // Kilometraje
    private Double kmActual;               // kilometraje actual
    private Double kmUltimoMantenimiento;  // km en el último mantenimiento
    private Double kmProximoMantenimiento; // km programado para próximo servicio

    // Fechas
    private LocalDate fechaUltimoMantenimiento;
    private LocalDate fechaProximoMantenimiento;
    private LocalDate vencimientoSoat;
    private LocalDate vencimientoRevisionTecnica;

    @Enumerated(EnumType.STRING)
    private EstadoVehiculo estado;

    private Integer totalEntregasRealizadas;
    private Boolean requiereMantenimiento;  // alerta activa

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
```

### Entidad: `RegistroMantenimiento`

```java
@Entity
@Table(name = "registro_mantenimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @Enumerated(EnumType.STRING)
    private TipoMantenimiento tipo;

    private String descripcion;            // qué se hizo
    private String taller;                 // dónde se hizo
    private Double kmAlServicio;           // km cuando entró al taller
    private Double costo;                  // costo del servicio en soles

    private String tecnicoResponsable;
    private String observaciones;

    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;
}
```

### Entidad: `AlertaMantenimiento`

```java
@Entity
@Table(name = "alerta_mantenimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    private String tipoAlerta;             // "KM_EXCEDIDO", "FECHA_VENCIDA", "SOAT", "REVISION"
    private String mensaje;
    private String prioridad;              // "ALTA", "MEDIA", "BAJA"
    private Boolean resuelta;

    @CreationTimestamp
    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;
}
```

---

## DTOs

### `EntregaCompletadaEvent.java` — llega desde RabbitMQ

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaCompletadaEvent {
    private Long entregaId;
    private Long pedidoId;
    private String placaVehiculo;          // placa del camión que hizo la entrega
    private String conductorNombre;
    private Double distanciaKm;            // km recorridos en la entrega
    private String minaDestino;
    private LocalDateTime fechaEntrega;
}
```

### `VehiculoRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoRequest {

    @NotBlank(message = "La placa es obligatoria")
    private String placa;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    private String modelo;
    private Integer anio;
    private String tipo;
    private Double capacidadToneladasM3;
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Double kmActual;

    private LocalDate vencimientoSoat;
    private LocalDate vencimientoRevisionTecnica;
}
```

### `VehiculoResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoResponse {
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private String tipo;
    private String conductorNombre;
    private String conductorTelefono;
    private Double kmActual;
    private Double kmProximoMantenimiento;
    private Double kmRestantesParaMantenimiento;
    private LocalDate fechaProximoMantenimiento;
    private LocalDate vencimientoSoat;
    private String estado;
    private Boolean requiereMantenimiento;
    private Integer totalEntregasRealizadas;
}
```

### `RegistroMantenimientoRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroMantenimientoRequest {

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    private TipoMantenimiento tipo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private String taller;
    private Double kmAlServicio;
    private Double costo;
    private String tecnicoResponsable;
    private String observaciones;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;
}
```

---

## Configuración RabbitMQ

### `RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {

    // Exchange compartido del sistema
    public static final String EXCHANGE              = "madera.exchange";

    // Cola exclusiva para ms-mantenimiento
    public static final String COLA_ENTREGAS_COMP   = "cola.entregas.completadas";
    public static final String ROUTING_ENTREGA_COMP = "entrega.completada";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaEntregasCompletadas() {
        return new Queue(COLA_ENTREGAS_COMP, true);
    }

    @Bean
    public Binding bindingEntregaCompletada(
            Queue colaEntregasCompletadas,
            TopicExchange maderaExchange) {
        return BindingBuilder
            .bind(colaEntregasCompletadas)
            .to(maderaExchange)
            .with(ROUTING_ENTREGA_COMP);
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

> **Importante:** en ms-entrega debes agregar la publicación del evento `entrega.completada` cuando se confirma la recepción, igual que publicas otros eventos. ms-mantenimiento lo escucha automáticamente.

### Agregar en ms-entrega — `EntregaServiceImpl.java`

En el método `confirmarRecepcion`, después de guardar la entrega, agregar:

```java
// Publicar evento para ms-mantenimiento
EntregaCompletadaEvent eventoMant = new EntregaCompletadaEvent(
    entrega.getId(),
    entrega.getPedidoId(),
    entrega.getVehiculo(),           // placa del vehículo
    entrega.getTransportista(),
    entrega.getDistanciaKm(),
    entrega.getMinaDestino(),
    LocalDateTime.now()
);
amqpTemplate.convertAndSend(
    "madera.exchange",
    "entrega.completada",
    eventoMant
);
```

---

## Repositorios

### `VehiculoRepository.java`

```java
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<Vehiculo> findByEstado(EstadoVehiculo estado);
    List<Vehiculo> findByRequiereMantenimiento(Boolean requiere);
    List<Vehiculo> findByConductorNombreContainingIgnoreCase(String nombre);

    // Vehículos con SOAT próximo a vencer (30 días)
    @Query("SELECT v FROM Vehiculo v WHERE v.vencimientoSoat " +
           "BETWEEN :hoy AND :limite AND v.estado != 'INACTIVO'")
    List<Vehiculo> findVehiculosConSoatPorVencer(
        @Param("hoy") LocalDate hoy,
        @Param("limite") LocalDate limite);

    // Vehículos que superaron el límite de km
    @Query("SELECT v FROM Vehiculo v WHERE v.kmActual >= v.kmProximoMantenimiento " +
           "AND v.estado = 'OPERATIVO'")
    List<Vehiculo> findVehiculosKmExcedido();
}
```

### `AlertaMantenimientoRepository.java`

```java
@Repository
public interface AlertaMantenimientoRepository
        extends JpaRepository<AlertaMantenimiento, Long> {

    List<AlertaMantenimiento> findByResuelta(Boolean resuelta);
    List<AlertaMantenimiento> findByVehiculoId(Long vehiculoId);
    List<AlertaMantenimiento> findByPrioridad(String prioridad);
    long countByResuelta(Boolean resuelta);
}
```

### `RegistroMantenimientoRepository.java`

```java
@Repository
public interface RegistroMantenimientoRepository
        extends JpaRepository<RegistroMantenimiento, Long> {

    List<RegistroMantenimiento> findByVehiculoId(Long vehiculoId);
    List<RegistroMantenimiento> findByTipo(TipoMantenimiento tipo);
    List<RegistroMantenimiento> findByFechaIngresoBetween(
        LocalDate inicio, LocalDate fin);

    @Query("SELECT SUM(r.costo) FROM RegistroMantenimiento r " +
           "WHERE r.vehiculo.id = :vehiculoId")
    Double costoTotalPorVehiculo(@Param("vehiculoId") Long vehiculoId);
}
```

---

## Listener — corazón del microservicio

### `EntregaCompletadaListener.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EntregaCompletadaListener {

    private final MantenimientoService mantenimientoService;

    @RabbitListener(queues = RabbitMQConfig.COLA_ENTREGAS_COMP)
    public void procesarEntregaCompletada(EntregaCompletadaEvent evento) {

        log.info("Entrega completada recibida: #{} | Vehículo: {} | Km: {}",
            evento.getEntregaId(),
            evento.getPlacaVehiculo(),
            evento.getDistanciaKm());

        try {
            mantenimientoService.procesarEntregaCompletada(evento);
            log.info("Kilometraje actualizado para vehículo: {}",
                evento.getPlacaVehiculo());
        } catch (Exception e) {
            log.error("Error procesando entrega #{}: {}",
                evento.getEntregaId(), e.getMessage());
            throw e; // RabbitMQ reintentará
        }
    }
}
```

---

## Lógica de negocio

### `MantenimientoServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MantenimientoServiceImpl implements MantenimientoService {

    private final VehiculoRepository              vehiculoRepo;
    private final RegistroMantenimientoRepository registroRepo;
    private final AlertaMantenimientoRepository   alertaRepo;

    @Value("${mantenimiento.km-limite:5000}")
    private Double kmLimite;

    @Value("${mantenimiento.dias-limite:90}")
    private Integer diasLimite;

    // ─── REGISTRAR VEHÍCULO ──────────────────────────────────────────────────
    @Transactional
    public VehiculoResponse registrarVehiculo(VehiculoRequest request) {

        if (vehiculoRepo.existsByPlaca(request.getPlaca())) {
            throw new RuntimeException(
                "Ya existe un vehículo con la placa: " + request.getPlaca());
        }

        Vehiculo vehiculo = Vehiculo.builder()
            .placa(request.getPlaca().toUpperCase())
            .marca(request.getMarca())
            .modelo(request.getModelo())
            .anio(request.getAnio())
            .tipo(request.getTipo())
            .capacidadToneladasM3(request.getCapacidadToneladasM3())
            .conductorNombre(request.getConductorNombre())
            .conductorLicencia(request.getConductorLicencia())
            .conductorTelefono(request.getConductorTelefono())
            .kmActual(request.getKmActual() != null ? request.getKmActual() : 0.0)
            .kmUltimoMantenimiento(0.0)
            .kmProximoMantenimiento(
                (request.getKmActual() != null ? request.getKmActual() : 0.0)
                + kmLimite)
            .vencimientoSoat(request.getVencimientoSoat())
            .vencimientoRevisionTecnica(request.getVencimientoRevisionTecnica())
            .estado(EstadoVehiculo.OPERATIVO)
            .totalEntregasRealizadas(0)
            .requiereMantenimiento(false)
            .build();

        vehiculoRepo.save(vehiculo);
        return mapToResponse(vehiculo);
    }

    // ─── PROCESAR EVENTO DE ENTREGA COMPLETADA ───────────────────────────────
    @Transactional
    public void procesarEntregaCompletada(EntregaCompletadaEvent evento) {

        if (evento.getPlacaVehiculo() == null ||
            evento.getPlacaVehiculo().isBlank()) {
            log.warn("Evento sin placa de vehículo, ignorando.");
            return;
        }

        // Buscar vehículo por placa — si no existe, crearlo automáticamente
        Vehiculo vehiculo = vehiculoRepo
            .findByPlaca(evento.getPlacaVehiculo().toUpperCase())
            .orElseGet(() -> {
                log.info("Vehículo {} no registrado. Creando automáticamente.",
                    evento.getPlacaVehiculo());
                return crearVehiculoAutomatico(evento);
            });

        // Actualizar kilometraje
        double kmRecorridos = evento.getDistanciaKm() != null
            ? evento.getDistanciaKm() : 0.0;
        vehiculo.setKmActual(vehiculo.getKmActual() + kmRecorridos);
        vehiculo.setTotalEntregasRealizadas(
            vehiculo.getTotalEntregasRealizadas() + 1);

        // Verificar si necesita mantenimiento por km
        if (vehiculo.getKmActual() >= vehiculo.getKmProximoMantenimiento()) {
            vehiculo.setRequiereMantenimiento(true);
            generarAlerta(vehiculo, "KM_EXCEDIDO",
                String.format("Vehículo %s superó los %.0f km de mantenimiento. " +
                    "Km actual: %.0f",
                    vehiculo.getPlaca(), kmLimite, vehiculo.getKmActual()),
                "ALTA");
            log.warn("ALERTA: Vehículo {} requiere mantenimiento. Km: {}",
                vehiculo.getPlaca(), vehiculo.getKmActual());
        }

        // Verificar SOAT próximo a vencer
        if (vehiculo.getVencimientoSoat() != null) {
            long diasParaVencer = ChronoUnit.DAYS.between(
                LocalDate.now(), vehiculo.getVencimientoSoat());
            if (diasParaVencer <= 30 && diasParaVencer >= 0) {
                generarAlerta(vehiculo, "SOAT_POR_VENCER",
                    String.format("SOAT del vehículo %s vence en %d días (%s)",
                        vehiculo.getPlaca(), diasParaVencer,
                        vehiculo.getVencimientoSoat()),
                    diasParaVencer <= 7 ? "ALTA" : "MEDIA");
            }
        }

        vehiculoRepo.save(vehiculo);
    }

    // ─── REGISTRAR MANTENIMIENTO ─────────────────────────────────────────────
    @Transactional
    public RegistroMantenimientoResponse registrarMantenimiento(
            Long vehiculoId, RegistroMantenimientoRequest request) {

        Vehiculo vehiculo = vehiculoRepo.findById(vehiculoId)
            .orElseThrow(() -> new RuntimeException(
                "Vehículo no encontrado: " + vehiculoId));

        RegistroMantenimiento registro = RegistroMantenimiento.builder()
            .vehiculo(vehiculo)
            .tipo(request.getTipo())
            .descripcion(request.getDescripcion())
            .taller(request.getTaller())
            .kmAlServicio(vehiculo.getKmActual())
            .costo(request.getCosto())
            .tecnicoResponsable(request.getTecnicoResponsable())
            .observaciones(request.getObservaciones())
            .fechaIngreso(request.getFechaIngreso() != null
                ? request.getFechaIngreso() : LocalDate.now())
            .fechaSalida(request.getFechaSalida())
            .build();

        registroRepo.save(registro);

        // Actualizar vehículo post-mantenimiento
        vehiculo.setKmUltimoMantenimiento(vehiculo.getKmActual());
        vehiculo.setKmProximoMantenimiento(vehiculo.getKmActual() + kmLimite);
        vehiculo.setFechaUltimoMantenimiento(LocalDate.now());
        vehiculo.setFechaProximoMantenimiento(
            LocalDate.now().plusDays(diasLimite));
        vehiculo.setRequiereMantenimiento(false);
        vehiculo.setEstado(request.getFechaSalida() != null
            ? EstadoVehiculo.OPERATIVO
            : EstadoVehiculo.EN_MANTENIMIENTO);
        vehiculoRepo.save(vehiculo);

        // Resolver alertas pendientes del vehículo
        alertaRepo.findByVehiculoId(vehiculoId).stream()
            .filter(a -> !a.getResuelta())
            .forEach(a -> {
                a.setResuelta(true);
                a.setFechaResolucion(LocalDateTime.now());
                alertaRepo.save(a);
            });

        log.info("Mantenimiento {} registrado para vehículo {}",
            request.getTipo(), vehiculo.getPlaca());

        return mapRegistroToResponse(registro);
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    public List<VehiculoResponse> listarVehiculos() {
        return vehiculoRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public List<VehiculoResponse> vehiculosQueRequierenMantenimiento() {
        return vehiculoRepo.findByRequiereMantenimiento(true).stream()
            .map(this::mapToResponse).toList();
    }

    public List<AlertaMantenimiento> alertasPendientes() {
        return alertaRepo.findByResuelta(false);
    }

    public List<RegistroMantenimientoResponse> historialVehiculo(Long id) {
        return registroRepo.findByVehiculoId(id).stream()
            .map(this::mapRegistroToResponse).toList();
    }

    public VehiculoResponse obtenerPorPlaca(String placa) {
        return mapToResponse(vehiculoRepo.findByPlaca(placa.toUpperCase())
            .orElseThrow(() -> new RuntimeException(
                "Vehículo no encontrado: " + placa)));
    }

    // ─── HELPERS PRIVADOS ────────────────────────────────────────────────────
    private Vehiculo crearVehiculoAutomatico(EntregaCompletadaEvent evento) {
        Vehiculo v = Vehiculo.builder()
            .placa(evento.getPlacaVehiculo().toUpperCase())
            .marca("Por registrar")
            .conductorNombre(evento.getConductorNombre())
            .kmActual(0.0)
            .kmUltimoMantenimiento(0.0)
            .kmProximoMantenimiento(kmLimite)
            .estado(EstadoVehiculo.OPERATIVO)
            .totalEntregasRealizadas(0)
            .requiereMantenimiento(false)
            .build();
        return vehiculoRepo.save(v);
    }

    private void generarAlerta(Vehiculo vehiculo, String tipo,
                               String mensaje, String prioridad) {
        AlertaMantenimiento alerta = AlertaMantenimiento.builder()
            .vehiculo(vehiculo)
            .tipoAlerta(tipo)
            .mensaje(mensaje)
            .prioridad(prioridad)
            .resuelta(false)
            .build();
        alertaRepo.save(alerta);
    }

    // ─── MAPPERS ─────────────────────────────────────────────────────────────
    private VehiculoResponse mapToResponse(Vehiculo v) {
        double kmRestantes = v.getKmProximoMantenimiento() != null
            ? Math.max(0, v.getKmProximoMantenimiento() - v.getKmActual())
            : 0;
        return VehiculoResponse.builder()
            .id(v.getId())
            .placa(v.getPlaca())
            .marca(v.getMarca())
            .modelo(v.getModelo())
            .tipo(v.getTipo())
            .conductorNombre(v.getConductorNombre())
            .conductorTelefono(v.getConductorTelefono())
            .kmActual(v.getKmActual())
            .kmProximoMantenimiento(v.getKmProximoMantenimiento())
            .kmRestantesParaMantenimiento(kmRestantes)
            .fechaProximoMantenimiento(v.getFechaProximoMantenimiento())
            .vencimientoSoat(v.getVencimientoSoat())
            .estado(v.getEstado().name())
            .requiereMantenimiento(v.getRequiereMantenimiento())
            .totalEntregasRealizadas(v.getTotalEntregasRealizadas())
            .build();
    }

    private RegistroMantenimientoResponse mapRegistroToResponse(
            RegistroMantenimiento r) {
        RegistroMantenimientoResponse res = new RegistroMantenimientoResponse();
        res.setId(r.getId());
        res.setPlacaVehiculo(r.getVehiculo().getPlaca());
        res.setTipo(r.getTipo().name());
        res.setDescripcion(r.getDescripcion());
        res.setTaller(r.getTaller());
        res.setKmAlServicio(r.getKmAlServicio());
        res.setCosto(r.getCosto());
        res.setFechaIngreso(r.getFechaIngreso());
        res.setFechaSalida(r.getFechaSalida());
        return res;
    }
}
```

---

## Endpoints REST

### `MantenimientoController.java`

```java
@RestController
@RequestMapping("/api/mantenimiento")
@RequiredArgsConstructor
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    // ── Vehículos ─────────────────────────────────────────────────────────────
    @GetMapping("/vehiculos")
    public ResponseEntity<List<VehiculoResponse>> listarVehiculos() {
        return ResponseEntity.ok(mantenimientoService.listarVehiculos());
    }

    @GetMapping("/vehiculos/{placa}")
    public ResponseEntity<VehiculoResponse> obtenerPorPlaca(
            @PathVariable String placa) {
        return ResponseEntity.ok(mantenimientoService.obtenerPorPlaca(placa));
    }

    @PostMapping("/vehiculos")
    public ResponseEntity<VehiculoResponse> registrar(
            @RequestBody @Valid VehiculoRequest request) {
        return ResponseEntity.status(201)
            .body(mantenimientoService.registrarVehiculo(request));
    }

    @GetMapping("/vehiculos/requieren-mantenimiento")
    public ResponseEntity<List<VehiculoResponse>> requierenMantenimiento() {
        return ResponseEntity.ok(
            mantenimientoService.vehiculosQueRequierenMantenimiento());
    }

    // ── Mantenimientos ────────────────────────────────────────────────────────
    @PostMapping("/vehiculos/{id}/mantenimiento")
    public ResponseEntity<RegistroMantenimientoResponse> registrarMantenimiento(
            @PathVariable Long id,
            @RequestBody @Valid RegistroMantenimientoRequest request) {
        return ResponseEntity.status(201)
            .body(mantenimientoService.registrarMantenimiento(id, request));
    }

    @GetMapping("/vehiculos/{id}/historial")
    public ResponseEntity<List<RegistroMantenimientoResponse>> historial(
            @PathVariable Long id) {
        return ResponseEntity.ok(mantenimientoService.historialVehiculo(id));
    }

    // ── Alertas ───────────────────────────────────────────────────────────────
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaMantenimiento>> alertasPendientes() {
        return ResponseEntity.ok(mantenimientoService.alertasPendientes());
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/mantenimiento/vehiculos` | Lista todos los vehículos | Admin, Almacén |
| `GET` | `/api/mantenimiento/vehiculos/{placa}` | Detalle por placa | Admin |
| `POST` | `/api/mantenimiento/vehiculos` | Registrar vehículo | Admin |
| `GET` | `/api/mantenimiento/vehiculos/requieren-mantenimiento` | Alertas de km | Admin |
| `POST` | `/api/mantenimiento/vehiculos/{id}/mantenimiento` | Registrar servicio | Admin |
| `GET` | `/api/mantenimiento/vehiculos/{id}/historial` | Historial de servicios | Admin |
| `GET` | `/api/mantenimiento/alertas` | Todas las alertas pendientes | Admin |

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(
            RuntimeException ex) {
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
}
```

---

## Pasos de implementación

### Paso 1 — Configurar Spring Initializr ⏱ 10 min
Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation, DevTools, RabbitMQ, Eureka Client, Actuator.

**Verificación:** App arranca en puerto `8089`.

---

### Paso 2 — Crear los enums ⏱ 5 min
`EstadoVehiculo` y `TipoMantenimiento`.

---

### Paso 3 — Crear entidades ⏱ 30 min
En orden: `Vehiculo` → `RegistroMantenimiento` → `AlertaMantenimiento`.

**Verificación:** JPA crea las 3 tablas en `mantenimiento_db`.

---

### Paso 4 — Crear los DTOs ⏱ 20 min
`EntregaCompletadaEvent`, `VehiculoRequest`, `VehiculoResponse`, `RegistroMantenimientoRequest`, `RegistroMantenimientoResponse`, `AlertaMantenimientoResponse`.

---

### Paso 5 — Crear los 3 repositorios ⏱ 15 min
`VehiculoRepository` con las queries JPQL para SOAT y km excedido. `AlertaMantenimientoRepository` y `RegistroMantenimientoRepository`.

---

### Paso 6 — Crear `RabbitMQConfig.java` ⏱ 10 min
Cola `cola.entregas.completadas` con routing key `entrega.completada`.

**Verificación:** Cola visible en `http://localhost:15672`.

---

### Paso 7 — Crear `EntregaCompletadaListener.java` ⏱ 10 min
`@RabbitListener` escuchando la cola. Llama al service para procesar el evento.

---

### Paso 8 — Crear `MantenimientoService` y `MantenimientoServiceImpl` ⏱ 60 min
Lógica completa: registrar vehículo, procesar evento de entrega (actualizar km, generar alertas), registrar mantenimiento y resolver alertas.

**Verificación:** Compila sin errores.

---

### Paso 9 — Crear `MantenimientoController.java` ⏱ 15 min
Los 7 endpoints REST.

**Verificación:** `GET http://localhost:8089/api/mantenimiento/vehiculos` devuelve `[]`.

---

### Paso 10 — Actualizar ms-entrega ⏱ 15 min
Agregar publicación del evento `entrega.completada` en `confirmarRecepcion` de `EntregaServiceImpl`.

**Verificación:** Al confirmar una entrega, ms-mantenimiento actualiza el km del vehículo automáticamente.

---

### Paso 11 — Agregar al api-gateway ⏱ 5 min

```properties
spring.cloud.gateway.routes[7].id=ms-mantenimiento
spring.cloud.gateway.routes[7].uri=lb://ms-mantenimiento
spring.cloud.gateway.routes[7].predicates[0]=Path=/api/mantenimiento/**
```

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
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Paso 13 — Agregar al docker-compose.yml ⏱ 5 min

```yaml
  ms-mantenimiento:
    build: ./ms-mantenimiento
    container_name: ms-mantenimiento
    ports:
      - "8089:8089"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mantenimiento_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      SPRING_RABBITMQ_HOST: rabbitmq
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      MANTENIMIENTO_KM_LIMITE: 5000
      MANTENIMIENTO_DIAS_LIMITE: 90
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

Agregar al `init-db.sql`:

```sql
CREATE DATABASE mantenimiento_db;
```

---

## Pruebas con Postman

### Prueba 1 — Registrar vehículo

```
POST http://localhost:8080/api/mantenimiento/vehiculos
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "placa": "ABC-123",
  "marca": "Volvo",
  "modelo": "FH 460",
  "anio": 2020,
  "tipo": "camion",
  "capacidadToneladasM3": 25.0,
  "conductorNombre": "Pedro Quispe",
  "conductorLicencia": "Q15234567",
  "conductorTelefono": "987654321",
  "kmActual": 45000.0,
  "vencimientoSoat": "2026-12-31",
  "vencimientoRevisionTecnica": "2026-08-15"
}
```

**Respuesta esperada:** `201 Created` con el vehículo registrado y `kmProximoMantenimiento: 50000`.

---

### Prueba 2 — Verificar actualización automática por RabbitMQ

Confirmar una entrega en ms-entrega:

```
PUT http://localhost:8080/api/entregas/1/confirmar-recepcion
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "recibidoPor": "Juan Perez",
  "conformidad": true,
  "observaciones": "Madera en buen estado"
}
```

Luego verificar en ms-mantenimiento:

```
GET http://localhost:8080/api/mantenimiento/vehiculos/ABC-123
Authorization: Bearer <token>
```

**Respuesta esperada:** `kmActual` aumentó con los km de la entrega.

---

### Prueba 3 — Registrar mantenimiento realizado

```
POST http://localhost:8080/api/mantenimiento/vehiculos/1/mantenimiento
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "tipo": "PREVENTIVO",
  "descripcion": "Cambio de aceite, filtros y revisión de frenos",
  "taller": "Taller El Mecánico SAC",
  "costo": 850.00,
  "tecnicoResponsable": "Luis Mamani",
  "fechaIngreso": "2026-05-20",
  "fechaSalida": "2026-05-21"
}
```

**Respuesta esperada:** `201 Created`. El `kmProximoMantenimiento` se reinicia y `requiereMantenimiento` pasa a `false`.

---

### Prueba 4 — Ver vehículos que requieren mantenimiento

```
GET http://localhost:8080/api/mantenimiento/vehiculos/requieren-mantenimiento
Authorization: Bearer <token>
```

**Respuesta esperada:** Lista de vehículos con `requiereMantenimiento: true`.

---

### Prueba 5 — Ver alertas pendientes

```
GET http://localhost:8080/api/mantenimiento/alertas
Authorization: Bearer <token>
```

**Respuesta esperada:** Alertas de km excedido, SOAT por vencer, etc.

---

### Prueba 6 — Ver historial de un vehículo

```
GET http://localhost:8080/api/mantenimiento/vehiculos/1/historial
Authorization: Bearer <token>
```

**Respuesta esperada:** Lista de todos los servicios realizados al vehículo.

---

## Checklist final

- [ ] App arranca en puerto `8089`
- [ ] Tablas `vehiculo`, `registro_mantenimiento` y `alerta_mantenimiento` creadas
- [ ] `MS-MANTENIMIENTO` aparece en Eureka `http://localhost:8761`
- [ ] `POST /api/mantenimiento/vehiculos` registra vehículo correctamente
- [ ] Al confirmar entrega en ms-entrega, el km del vehículo se actualiza automáticamente
- [ ] Al superar el límite de km, se genera una alerta automática
- [ ] `GET /api/mantenimiento/vehiculos/requieren-mantenimiento` lista correctamente
- [ ] `POST /api/mantenimiento/vehiculos/{id}/mantenimiento` registra servicio y reinicia contadores
- [ ] Alertas se resuelven automáticamente al registrar mantenimiento
- [ ] Funciona a través del gateway con token JWT
- [ ] Cola `cola.entregas.completadas` visible en RabbitMQ `http://localhost:15672`

---

## Sistema completo — 7 microservicios de negocio

Con ms-mantenimiento implementado, el sistema cuenta con los **7 microservicios de negocio** requeridos:

| # | Microservicio | Puerto | BD | Comunicación |
|---|---|---|---|---|
| 1 | ms-inventario | 8082 | inventario_db | REST |
| 2 | ms-pedidos | 8083 | pedidos_db | Feign + RabbitMQ publisher |
| 3 | ms-notificaciones | 8084 | notificaciones_db | RabbitMQ listener |
| 4 | ms-entrega | 8086 | entrega_db | Feign + RabbitMQ publisher |
| 5 | ms-proveedores | 8087 | proveedores_db | Feign + RabbitMQ publisher |
| 6 | ms-reportes | 8088 | reportes_db | Feign (4 servicios) |
| 7 | ms-mantenimiento | 8089 | mantenimiento_db | RabbitMQ listener |

Más los servicios de infraestructura:

| Servicio | Puerto |
|---|---|
| eureka-server | 8761 |
| api-gateway | 8080 |
| ms-auth | 8085 |
| Frontend Vue.js | 80 |

---

## Cobertura total del sílabo

| Unidad | Tema | Implementado en |
|---|---|---|
| **1** | Feign Client sincrónico | ms-pedidos, ms-entrega, ms-proveedores, ms-reportes |
| **1** | RabbitMQ mensajería asíncrona | ms-pedidos, ms-entrega, ms-proveedores → ms-notificaciones, ms-mantenimiento |
| **1** | Spring Cloud · Eureka | eureka-server · todos los microservicios |
| **2** | JWT + OAuth2 + Spring Security | ms-auth + api-gateway |
| **2** | Resilience4J Circuit Breaker | ms-pedidos, ms-entrega, ms-proveedores, ms-reportes |
| **2** | Docker + docker-compose | Todos los servicios contenedorizados |
| **3** | Spring Boot Actuator | Todos los microservicios |
| **3** | Prometheus + Grafana | observabilidad/ |
| **3** | ELK Stack | observabilidad/logstash/ |

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
