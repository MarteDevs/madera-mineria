# Plan Técnico — ms-reportes
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-reportes · Puerto `8088`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`reportes_db`)  
> **Depende de:** ms-inventario · ms-pedidos · ms-entrega · ms-proveedores (todos vía Feign)

---

## ¿Qué hace este microservicio?

Es el microservicio de **inteligencia del sistema**. Consulta datos de todos los demás microservicios vía Feign y genera reportes consolidados. Es el más complejo en cuanto a comunicación porque habla con 4 servicios distintos al mismo tiempo.

| Responsabilidad | Descripción |
|---|---|
| Reporte de stock | Stock actual por mina y tipo de madera desde ms-inventario |
| Reporte de pedidos | Pedidos por período, estado y mina desde ms-pedidos |
| Reporte de entregas | Entregas completadas y en tránsito desde ms-entrega |
| Reporte de proveedores | Entregas por proveedor y montos desde ms-proveedores |
| Dashboard general | Resumen ejecutivo del sistema completo |
| Historial | Guarda cada reporte generado con fecha y parámetros |

---

## Flujo principal

```
Admin solicita reporte general
            │
            ▼
GET /api/reportes/dashboard
            │
            ├──── Feign ────▶ ms-inventario
            │                 GET /api/inventario
            │                 (stock por tipo y mina)
            │
            ├──── Feign ────▶ ms-pedidos
            │                 GET /api/pedidos
            │                 (pedidos del período)
            │
            ├──── Feign ────▶ ms-entrega
            │                 GET /api/entregas
            │                 (estado de entregas)
            │
            └──── Feign ────▶ ms-proveedores
                              GET /api/proveedores
                              (resumen de proveedores)
                                    │
                                    ▼
                         ms-reportes consolida todo
                                    │
                                    ▼
                         Devuelve DashboardResponse
                         (un solo objeto con todo)
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints REST de reportes |
| Spring Data JPA | Guardar historial de reportes generados |
| PostgreSQL Driver | Conexión a `reportes_db` |
| Lombok | Getters, setters automáticos |
| Validation | Validar parámetros de entrada |
| DevTools | Recarga automática |
| **OpenFeign** | Consultar datos de 4 microservicios distintos |
| **Eureka Client** | Descubrimiento de servicios |
| **Actuator** | Health checks y métricas |
| **Resilience4J** | Circuit Breaker para cada cliente Feign |

---

## Estructura de archivos

```
ms-reportes/src/main/java/com/madera/reportes/
│
├── MsReportesApplication.java             ← @EnableFeignClients aquí
│
├── model/
│   ├── ReporteGenerado.java               ← historial de reportes (crear PRIMERO)
│   └── TipoReporte.java                   ← enum de tipos de reporte
│
├── dto/
│   ── externos/                           ← DTOs que vienen de otros servicios
│   │   ├── MaderaDto.java                 ← de ms-inventario
│   │   ├── PedidoDto.java                 ← de ms-pedidos
│   │   ├── EntregaDto.java                ← de ms-entrega
│   │   └── ProveedorDto.java              ← de ms-proveedores
│   │
│   └── response/                          ← DTOs que devuelve ms-reportes
│       ├── DashboardResponse.java         ← resumen ejecutivo completo
│       ├── ReporteStockResponse.java      ← stock por mina
│       ├── ReportePedidosResponse.java    ← pedidos por período
│       ├── ReporteEntregasResponse.java   ← entregas y tiempos
│       ├── ReporteProveedoresResponse.java← resumen de proveedores
│       └── ReporteGeneradoResponse.java  ← historial de reportes
│
├── repository/
│   └── ReporteGeneradoRepository.java
│
├── client/
│   ├── InventarioClient.java
│   ├── InventarioClientFallback.java
│   ├── PedidosClient.java
│   ├── PedidosClientFallback.java
│   ├── EntregaClient.java
│   ├── EntregaClientFallback.java
│   ├── ProveedoresClient.java
│   └── ProveedoresClientFallback.java
│
├── service/
│   ├── ReporteService.java
│   └── ReporteServiceImpl.java
│
├── controller/
│   └── ReporteController.java
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → clients → service → controller → exception

---

## Configuración

### `application.properties`

```properties
spring.application.name=ms-reportes
server.port=8088

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/reportes_db
spring.datasource.username=postgres
spring.datasource.password=postgres123

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Feign
feign.client.config.default.connectTimeout=8000
feign.client.config.default.readTimeout=8000

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true
eureka.instance.lease-renewal-interval-in-seconds=10

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# Resilience4J — un circuit breaker por cada cliente
resilience4j.circuitbreaker.instances.inventario.sliding-window-size=5
resilience4j.circuitbreaker.instances.inventario.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.inventario.wait-duration-in-open-state=10s

resilience4j.circuitbreaker.instances.pedidos.sliding-window-size=5
resilience4j.circuitbreaker.instances.pedidos.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.pedidos.wait-duration-in-open-state=10s

resilience4j.circuitbreaker.instances.entrega.sliding-window-size=5
resilience4j.circuitbreaker.instances.entrega.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.entrega.wait-duration-in-open-state=10s

resilience4j.circuitbreaker.instances.proveedores.sliding-window-size=5
resilience4j.circuitbreaker.instances.proveedores.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.proveedores.wait-duration-in-open-state=10s

resilience4j.retry.instances.inventario.max-attempts=2
resilience4j.retry.instances.pedidos.max-attempts=2
resilience4j.retry.instances.entrega.max-attempts=2
resilience4j.retry.instances.proveedores.max-attempts=2
```

### Crear base de datos

```sql
CREATE DATABASE reportes_db;
```

### Clase principal

```java
@SpringBootApplication
@EnableFeignClients
public class MsReportesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsReportesApplication.class, args);
    }
}
```

---

## Entidades de la base de datos

### Enum: `TipoReporte`

```java
public enum TipoReporte {
    DASHBOARD,       // resumen ejecutivo completo
    STOCK,           // inventario de madera
    PEDIDOS,         // pedidos por período
    ENTREGAS,        // entregas y tiempos
    PROVEEDORES      // resumen de proveedores
}
```

### Entidad: `ReporteGenerado`

```java
@Entity
@Table(name = "reporte_generado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoReporte tipo;

    private String solicitadoPor;          // usuario que pidió el reporte
    private String parametros;             // JSON con los filtros usados
    private Long tiempoGeneracionMs;       // milisegundos que tardó

    private Boolean exitoso;               // si todos los servicios respondieron
    private String serviciosConsultados;   // "inventario,pedidos,entrega"
    private String serviciosFallidos;      // si alguno no respondió

    @CreationTimestamp
    private LocalDateTime fechaGeneracion;
}
```

---

## DTOs externos (datos que vienen de otros servicios)

### `MaderaDto.java` — de ms-inventario

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaderaDto {
    private Long id;
    private String tipo;
    private String uso;
    private String unidad;
    private Double precioPorUnidad;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private String mina;
    private String estado;
}
```

### `PedidoDto.java` — de ms-pedidos

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDto {
    private Long id;
    private String tipoMadera;
    private Integer cantidadSolicitada;
    private String unidad;
    private String mina;
    private String solicitadoPor;
    private String estado;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
}
```

### `EntregaDto.java` — de ms-entrega

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDto {
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
    private Boolean conformidad;
}
```

### `ProveedorDto.java` — de ms-proveedores

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDto {
    private Long id;
    private String ruc;
    private String razonSocial;
    private String ciudad;
    private Boolean activo;
}
```

---

## DTOs de respuesta

### `DashboardResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // Resumen de inventario
    private Integer totalTiposMadera;
    private Integer stockTotalUnidades;
    private List<String> tiposConStockBajo;     // stock < stockMinimo
    private Map<String, Integer> stockPorMina;  // mina → total unidades

    // Resumen de pedidos
    private Integer totalPedidos;
    private Integer pedidosPendientes;
    private Integer pedidosAprobados;
    private Integer pedidosEntregados;
    private Integer pedidosRechazados;
    private Map<String, Integer> pedidosPorMina;

    // Resumen de entregas
    private Integer totalEntregas;
    private Integer entregasEnTransito;
    private Integer entregasCompletadas;
    private Double tiempoPromedioEntregaHoras;

    // Resumen de proveedores
    private Integer totalProveedores;
    private Integer proveedoresActivos;
    private Integer contratosProximosAVencer;

    // Metadata del reporte
    private LocalDateTime fechaGeneracion;
    private Long tiempoGeneracionMs;
    private List<String> serviciosNoDisponibles;
}
```

### `ReporteStockResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteStockResponse {
    private String mina;
    private List<ItemStockDto> items;
    private Integer totalUnidades;
    private Integer itemsConStockBajo;
    private LocalDateTime fechaGeneracion;

    @Data
    @AllArgsConstructor
    public static class ItemStockDto {
        private String tipo;
        private String uso;
        private Integer stock;
        private Integer stockMinimo;
        private Boolean stockBajo;
        private String unidad;
        private Double precioPorUnidad;
    }
}
```

### `ReportePedidosResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportePedidosResponse {
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private String mina;
    private Integer totalPedidos;
    private Map<String, Integer> porEstado;
    private Map<String, Integer> porTipoMadera;
    private Integer totalUnidadesSolicitadas;
    private List<PedidoDto> detalle;
    private LocalDateTime fechaGeneracion;
}
```

### `ReporteEntregasResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteEntregasResponse {
    private Integer totalEntregas;
    private Integer entregasCompletadas;
    private Integer entregasEnTransito;
    private Integer entregasFallidas;
    private Double tiempoPromedioHoras;
    private Double porcentajeConformidad;
    private Map<String, Integer> porMina;
    private Map<String, Integer> porTransportista;
    private List<EntregaDto> detalle;
    private LocalDateTime fechaGeneracion;
}
```

---

## Feign Clients

### `InventarioClient.java`

```java
@FeignClient(
    name = "ms-inventario",
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {

    @GetMapping("/api/inventario")
    List<MaderaDto> listarTodo();

    @GetMapping("/api/inventario/mina/{mina}")
    List<MaderaDto> porMina(@PathVariable String mina);
}
```

### `InventarioClientFallback.java`

```java
@Component
@Slf4j
public class InventarioClientFallback implements InventarioClient {

    @Override
    public List<MaderaDto> listarTodo() {
        log.warn("ms-inventario no disponible en reporte");
        return Collections.emptyList();
    }

    @Override
    public List<MaderaDto> porMina(String mina) {
        log.warn("ms-inventario no disponible para mina: {}", mina);
        return Collections.emptyList();
    }
}
```

### `PedidosClient.java`

```java
@FeignClient(
    name = "ms-pedidos",
    fallback = PedidosClientFallback.class
)
public interface PedidosClient {

    @GetMapping("/api/pedidos")
    List<PedidoDto> listarTodos();

    @GetMapping("/api/pedidos/mina/{mina}")
    List<PedidoDto> porMina(@PathVariable String mina);

    @GetMapping("/api/pedidos/estado/{estado}")
    List<PedidoDto> porEstado(@PathVariable String estado);
}
```

### `PedidosClientFallback.java`

```java
@Component
@Slf4j
public class PedidosClientFallback implements PedidosClient {

    @Override
    public List<PedidoDto> listarTodos() {
        log.warn("ms-pedidos no disponible en reporte");
        return Collections.emptyList();
    }

    @Override
    public List<PedidoDto> porMina(String mina) {
        return Collections.emptyList();
    }

    @Override
    public List<PedidoDto> porEstado(String estado) {
        return Collections.emptyList();
    }
}
```

### `EntregaClient.java`

```java
@FeignClient(
    name = "ms-entrega",
    fallback = EntregaClientFallback.class
)
public interface EntregaClient {

    @GetMapping("/api/entregas")
    List<EntregaDto> listarTodas();

    @GetMapping("/api/entregas/en-transito")
    List<EntregaDto> enTransito();

    @GetMapping("/api/entregas/mina/{mina}")
    List<EntregaDto> porMina(@PathVariable String mina);
}
```

### `ProveedoresClient.java`

```java
@FeignClient(
    name = "ms-proveedores",
    fallback = ProveedoresClientFallback.class
)
public interface ProveedoresClient {

    @GetMapping("/api/proveedores")
    List<ProveedorDto> listarTodos();

    @GetMapping("/api/proveedores/activos")
    List<ProveedorDto> listarActivos();

    @GetMapping("/api/proveedores/contratos/proximos-a-vencer")
    List<Object> contratosProximosAVencer();
}
```

> Los fallbacks de EntregaClient y ProveedoresClient siguen el mismo patrón — devuelven listas vacías y loguean el warning.

---

## Repositorio

### `ReporteGeneradoRepository.java`

```java
@Repository
public interface ReporteGeneradoRepository
        extends JpaRepository<ReporteGenerado, Long> {

    List<ReporteGenerado> findByTipo(TipoReporte tipo);
    List<ReporteGenerado> findBySolicitadoPor(String usuario);
    List<ReporteGenerado> findByExitoso(Boolean exitoso);
    List<ReporteGenerado> findByFechaGeneracionBetween(
        LocalDateTime inicio, LocalDateTime fin);

    // Tiempo promedio de generación por tipo
    @Query("SELECT AVG(r.tiempoGeneracionMs) FROM ReporteGenerado r " +
           "WHERE r.tipo = :tipo AND r.exitoso = true")
    Double promedioTiempoGeneracion(@Param("tipo") TipoReporte tipo);
}
```

---

## Lógica de negocio

### `ReporteServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    private final ReporteGeneradoRepository reporteRepo;
    private final InventarioClient  inventarioClient;
    private final PedidosClient     pedidosClient;
    private final EntregaClient     entregaClient;
    private final ProveedoresClient proveedoresClient;

    // ─── DASHBOARD GENERAL ───────────────────────────────────────────────────
    public DashboardResponse generarDashboard(String solicitadoPor) {
        long inicio = System.currentTimeMillis();
        List<String> serviciosFallidos = new ArrayList<>();

        // 1. Consultar todos los servicios en paralelo (cada uno con fallback)
        List<MaderaDto>    maderas    = fetchInventario(serviciosFallidos);
        List<PedidoDto>    pedidos    = fetchPedidos(serviciosFallidos);
        List<EntregaDto>   entregas   = fetchEntregas(serviciosFallidos);
        List<ProveedorDto> proveedores = fetchProveedores(serviciosFallidos);

        // 2. Calcular métricas de inventario
        Integer totalTipos = maderas.size();
        Integer stockTotal = maderas.stream()
            .mapToInt(m -> m.getStockDisponible() != null ? m.getStockDisponible() : 0)
            .sum();
        List<String> stockBajo = maderas.stream()
            .filter(m -> m.getStockDisponible() != null &&
                         m.getStockMinimo()     != null &&
                         m.getStockDisponible() <= m.getStockMinimo())
            .map(m -> m.getTipo() + " (" + m.getMina() + ")")
            .toList();
        Map<String, Integer> stockPorMina = maderas.stream()
            .collect(Collectors.groupingBy(
                m -> m.getMina() != null ? m.getMina() : "Sin mina",
                Collectors.summingInt(m -> m.getStockDisponible() != null
                    ? m.getStockDisponible() : 0)));

        // 3. Calcular métricas de pedidos
        Map<String, Integer> porEstado = pedidos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstado() != null ? p.getEstado() : "DESCONOCIDO",
                Collectors.summingInt(p -> 1)));
        Map<String, Integer> pedidosPorMina = pedidos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getMina() != null ? p.getMina() : "Sin mina",
                Collectors.summingInt(p -> 1)));

        // 4. Calcular métricas de entregas
        List<EntregaDto> enTransito   = entregas.stream()
            .filter(e -> "EN_RUTA".equals(e.getEstado()) ||
                         "PREPARANDO".equals(e.getEstado()))
            .toList();
        List<EntregaDto> completadas  = entregas.stream()
            .filter(e -> "ENTREGADO".equals(e.getEstado()))
            .toList();

        double tiempoPromedio = completadas.stream()
            .filter(e -> e.getFechaSalida() != null &&
                         e.getFechaEntregaReal() != null)
            .mapToLong(e -> Duration.between(
                e.getFechaSalida(),
                e.getFechaEntregaReal()).toHours())
            .average().orElse(0.0);

        // 5. Métricas de proveedores
        long provActivos = proveedores.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .count();

        // 6. Guardar historial
        long tiempoTotal = System.currentTimeMillis() - inicio;
        ReporteGenerado historial = ReporteGenerado.builder()
            .tipo(TipoReporte.DASHBOARD)
            .solicitadoPor(solicitadoPor)
            .tiempoGeneracionMs(tiempoTotal)
            .exitoso(serviciosFallidos.isEmpty())
            .serviciosConsultados("inventario,pedidos,entrega,proveedores")
            .serviciosFallidos(String.join(",", serviciosFallidos))
            .build();
        reporteRepo.save(historial);

        // 7. Construir respuesta
        return DashboardResponse.builder()
            .totalTiposMadera(totalTipos)
            .stockTotalUnidades(stockTotal)
            .tiposConStockBajo(stockBajo)
            .stockPorMina(stockPorMina)
            .totalPedidos(pedidos.size())
            .pedidosPendientes(porEstado.getOrDefault("PENDIENTE", 0))
            .pedidosAprobados(porEstado.getOrDefault("APROBADO", 0))
            .pedidosEntregados(porEstado.getOrDefault("ENTREGADO", 0))
            .pedidosRechazados(porEstado.getOrDefault("RECHAZADO", 0))
            .pedidosPorMina(pedidosPorMina)
            .totalEntregas(entregas.size())
            .entregasEnTransito(enTransito.size())
            .entregasCompletadas(completadas.size())
            .tiempoPromedioEntregaHoras(tiempoPromedio)
            .totalProveedores(proveedores.size())
            .proveedoresActivos((int) provActivos)
            .fechaGeneracion(LocalDateTime.now())
            .tiempoGeneracionMs(tiempoTotal)
            .serviciosNoDisponibles(serviciosFallidos)
            .build();
    }

    // ─── REPORTE DE STOCK ────────────────────────────────────────────────────
    public ReporteStockResponse reporteStock(String mina) {
        long inicio = System.currentTimeMillis();

        List<MaderaDto> maderas = mina != null
            ? inventarioClient.porMina(mina)
            : inventarioClient.listarTodo();

        List<ReporteStockResponse.ItemStockDto> items = maderas.stream()
            .map(m -> new ReporteStockResponse.ItemStockDto(
                m.getTipo(),
                m.getUso(),
                m.getStockDisponible(),
                m.getStockMinimo(),
                m.getStockDisponible() != null &&
                m.getStockMinimo()     != null &&
                m.getStockDisponible() <= m.getStockMinimo(),
                m.getUnidad(),
                m.getPrecioPorUnidad()
            ))
            .toList();

        int total = items.stream()
            .mapToInt(i -> i.getStock() != null ? i.getStock() : 0)
            .sum();
        long stockBajo = items.stream()
            .filter(ReporteStockResponse.ItemStockDto::getStockBajo)
            .count();

        guardarHistorial(TipoReporte.STOCK, "sistema",
            System.currentTimeMillis() - inicio, true, "inventario", "");

        return ReporteStockResponse.builder()
            .mina(mina != null ? mina : "TODAS")
            .items(items)
            .totalUnidades(total)
            .itemsConStockBajo((int) stockBajo)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── REPORTE DE PEDIDOS ──────────────────────────────────────────────────
    public ReportePedidosResponse reportePedidos(
            String mina, LocalDateTime desde, LocalDateTime hasta) {

        long inicio = System.currentTimeMillis();

        List<PedidoDto> todos = mina != null
            ? pedidosClient.porMina(mina)
            : pedidosClient.listarTodos();

        // Filtrar por fechas si se proporcionan
        List<PedidoDto> filtrados = todos.stream()
            .filter(p -> {
                if (desde == null && hasta == null) return true;
                if (p.getFechaPedido() == null) return false;
                boolean despueDe = desde == null ||
                    !p.getFechaPedido().isBefore(desde);
                boolean anteDe   = hasta == null ||
                    !p.getFechaPedido().isAfter(hasta);
                return despueDe && anteDe;
            })
            .toList();

        Map<String, Integer> porEstado = filtrados.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstado() != null ? p.getEstado() : "DESCONOCIDO",
                Collectors.summingInt(p -> 1)));

        Map<String, Integer> porTipo = filtrados.stream()
            .collect(Collectors.groupingBy(
                p -> p.getTipoMadera() != null ? p.getTipoMadera() : "Sin tipo",
                Collectors.summingInt(p -> 1)));

        int totalUnidades = filtrados.stream()
            .mapToInt(p -> p.getCantidadSolicitada() != null
                ? p.getCantidadSolicitada() : 0)
            .sum();

        guardarHistorial(TipoReporte.PEDIDOS, "sistema",
            System.currentTimeMillis() - inicio, true, "pedidos", "");

        return ReportePedidosResponse.builder()
            .desde(desde)
            .hasta(hasta)
            .mina(mina != null ? mina : "TODAS")
            .totalPedidos(filtrados.size())
            .porEstado(porEstado)
            .porTipoMadera(porTipo)
            .totalUnidadesSolicitadas(totalUnidades)
            .detalle(filtrados)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── REPORTE DE ENTREGAS ─────────────────────────────────────────────────
    public ReporteEntregasResponse reporteEntregas(String mina) {
        long inicio = System.currentTimeMillis();

        List<EntregaDto> entregas = mina != null
            ? entregaClient.porMina(mina)
            : entregaClient.listarTodas();

        List<EntregaDto> completadas = entregas.stream()
            .filter(e -> "ENTREGADO".equals(e.getEstado())).toList();
        List<EntregaDto> enTransito  = entregas.stream()
            .filter(e -> "EN_RUTA".equals(e.getEstado()) ||
                         "PREPARANDO".equals(e.getEstado())).toList();
        List<EntregaDto> fallidas    = entregas.stream()
            .filter(e -> "FALLIDO".equals(e.getEstado())).toList();

        double tiempoPromedio = completadas.stream()
            .filter(e -> e.getFechaSalida() != null &&
                         e.getFechaEntregaReal() != null)
            .mapToLong(e -> Duration.between(
                e.getFechaSalida(),
                e.getFechaEntregaReal()).toHours())
            .average().orElse(0.0);

        long conConformidad = completadas.stream()
            .filter(e -> Boolean.TRUE.equals(e.getConformidad()))
            .count();
        double pctConformidad = completadas.isEmpty() ? 0.0
            : (double) conConformidad / completadas.size() * 100;

        Map<String, Integer> porMina = entregas.stream()
            .collect(Collectors.groupingBy(
                e -> e.getMinaDestino() != null ? e.getMinaDestino() : "Sin mina",
                Collectors.summingInt(e -> 1)));

        Map<String, Integer> porTransportista = completadas.stream()
            .filter(e -> e.getTransportista() != null)
            .collect(Collectors.groupingBy(
                EntregaDto::getTransportista,
                Collectors.summingInt(e -> 1)));

        guardarHistorial(TipoReporte.ENTREGAS, "sistema",
            System.currentTimeMillis() - inicio, true, "entrega", "");

        return ReporteEntregasResponse.builder()
            .totalEntregas(entregas.size())
            .entregasCompletadas(completadas.size())
            .entregasEnTransito(enTransito.size())
            .entregasFallidas(fallidas.size())
            .tiempoPromedioHoras(tiempoPromedio)
            .porcentajeConformidad(pctConformidad)
            .porMina(porMina)
            .porTransportista(porTransportista)
            .detalle(entregas)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────
    private List<MaderaDto> fetchInventario(List<String> fallidos) {
        try {
            List<MaderaDto> result = inventarioClient.listarTodo();
            if (result.isEmpty()) fallidos.add("inventario");
            return result;
        } catch (Exception e) {
            fallidos.add("inventario");
            return Collections.emptyList();
        }
    }

    private List<PedidoDto> fetchPedidos(List<String> fallidos) {
        try {
            List<PedidoDto> result = pedidosClient.listarTodos();
            if (result.isEmpty()) fallidos.add("pedidos");
            return result;
        } catch (Exception e) {
            fallidos.add("pedidos");
            return Collections.emptyList();
        }
    }

    private List<EntregaDto> fetchEntregas(List<String> fallidos) {
        try {
            return entregaClient.listarTodas();
        } catch (Exception e) {
            fallidos.add("entrega");
            return Collections.emptyList();
        }
    }

    private List<ProveedorDto> fetchProveedores(List<String> fallidos) {
        try {
            return proveedoresClient.listarTodos();
        } catch (Exception e) {
            fallidos.add("proveedores");
            return Collections.emptyList();
        }
    }

    private void guardarHistorial(TipoReporte tipo, String usuario,
            long tiempo, boolean exitoso,
            String consultados, String fallidos) {
        reporteRepo.save(ReporteGenerado.builder()
            .tipo(tipo)
            .solicitadoPor(usuario)
            .tiempoGeneracionMs(tiempo)
            .exitoso(exitoso)
            .serviciosConsultados(consultados)
            .serviciosFallidos(fallidos)
            .build());
    }

    public List<ReporteGenerado> historial() {
        return reporteRepo.findAll();
    }
}
```

---

## Endpoints REST

### `ReporteController.java`

```java
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    // Dashboard general — consulta LOS 4 microservicios
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(defaultValue = "sistema") String solicitadoPor) {
        return ResponseEntity.ok(
            reporteService.generarDashboard(solicitadoPor));
    }

    // Reporte de stock por mina (o todo si no se especifica)
    @GetMapping("/stock")
    public ResponseEntity<ReporteStockResponse> stock(
            @RequestParam(required = false) String mina) {
        return ResponseEntity.ok(reporteService.reporteStock(mina));
    }

    // Reporte de pedidos con filtros opcionales
    @GetMapping("/pedidos")
    public ResponseEntity<ReportePedidosResponse> pedidos(
            @RequestParam(required = false) String mina,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime hasta) {
        return ResponseEntity.ok(
            reporteService.reportePedidos(mina, desde, hasta));
    }

    // Reporte de entregas
    @GetMapping("/entregas")
    public ResponseEntity<ReporteEntregasResponse> entregas(
            @RequestParam(required = false) String mina) {
        return ResponseEntity.ok(reporteService.reporteEntregas(mina));
    }

    // Historial de reportes generados
    @GetMapping("/historial")
    public ResponseEntity<List<ReporteGenerado>> historial() {
        return ResponseEntity.ok(reporteService.historial());
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Descripción | Consulta a |
|---|---|---|---|
| `GET` | `/api/reportes/dashboard` | Resumen ejecutivo completo | inventario + pedidos + entrega + proveedores |
| `GET` | `/api/reportes/stock?mina=Cerro Verde` | Stock por mina | ms-inventario |
| `GET` | `/api/reportes/pedidos?mina=X&desde=Y&hasta=Z` | Pedidos por período | ms-pedidos |
| `GET` | `/api/reportes/entregas?mina=X` | Reporte de entregas | ms-entrega |
| `GET` | `/api/reportes/historial` | Reportes generados | reportes_db |

---

## Pasos de implementación

### Paso 1 — Configurar Spring Initializr ⏱ 10 min
Dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation, DevTools, OpenFeign, Eureka Client, Actuator.

**Verificación:** App arranca en puerto `8088`.

---

### Paso 2 — Crear `TipoReporte.java` ⏱ 5 min
Enum con los 5 tipos de reporte.

---

### Paso 3 — Crear `ReporteGenerado.java` ⏱ 15 min
Entidad para el historial.

**Verificación:** JPA crea tabla `reporte_generado`.

---

### Paso 4 — Crear DTOs externos ⏱ 20 min
Los 4 DTOs que replican la estructura de los otros microservicios: `MaderaDto`, `PedidoDto`, `EntregaDto`, `ProveedorDto`.

> Deben tener exactamente los mismos campos que devuelven los otros servicios.

---

### Paso 5 — Crear DTOs de respuesta ⏱ 25 min
`DashboardResponse`, `ReporteStockResponse`, `ReportePedidosResponse`, `ReporteEntregasResponse`.

---

### Paso 6 — Crear repositorio ⏱ 10 min
`ReporteGeneradoRepository` con la query de tiempo promedio.

---

### Paso 7 — Crear los 4 Feign Clients con sus fallbacks ⏱ 30 min
`InventarioClient`, `PedidosClient`, `EntregaClient`, `ProveedoresClient`. Cada uno con su fallback que devuelve lista vacía.

**Verificación:** App arranca aunque ningún servicio esté corriendo.

---

### Paso 8 — Crear `ReporteService` y `ReporteServiceImpl` ⏱ 60 min
La implementación más larga. Contiene toda la lógica de cálculo de métricas para cada reporte.

**Verificación:** Compila sin errores.

---

### Paso 9 — Crear `ReporteController` ⏱ 15 min
Los 5 endpoints REST.

**Verificación:** `GET http://localhost:8088/api/reportes/dashboard` devuelve JSON con datos reales.

---

### Paso 10 — Agregar al api-gateway ⏱ 5 min

```properties
spring.cloud.gateway.routes[6].id=ms-reportes
spring.cloud.gateway.routes[6].uri=lb://ms-reportes
spring.cloud.gateway.routes[6].predicates[0]=Path=/api/reportes/**
```

---

### Paso 11 — Agregar Dockerfile ⏱ 5 min

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Paso 12 — Agregar al docker-compose.yml ⏱ 5 min

```yaml
  ms-reportes:
    build: ./ms-reportes
    container_name: ms-reportes
    ports:
      - "8088:8088"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/reportes_db
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
```

Agregar al `init-db.sql`:

```sql
CREATE DATABASE reportes_db;
```

---

## Pruebas con Postman

> Todos los servicios deben estar corriendo para obtener datos reales.

### Prueba 1 — Dashboard general (la más importante)

```
GET http://localhost:8080/api/reportes/dashboard
Authorization: Bearer <token>
```

**Respuesta esperada:** JSON con todas las métricas del sistema consolidadas, incluyendo `serviciosNoDisponibles: []` si todo está bien.

---

### Prueba 2 — Reporte de stock de una mina

```
GET http://localhost:8080/api/reportes/stock?mina=Cerro Verde
Authorization: Bearer <token>
```

**Respuesta esperada:** Stock detallado de Cerro Verde con alertas de stock bajo.

---

### Prueba 3 — Reporte de pedidos por período

```
GET http://localhost:8080/api/reportes/pedidos?desde=2026-01-01T00:00:00&hasta=2026-12-31T23:59:59
Authorization: Bearer <token>
```

**Respuesta esperada:** Pedidos del año 2026 agrupados por estado y tipo de madera.

---

### Prueba 4 — Reporte de entregas

```
GET http://localhost:8080/api/reportes/entregas
Authorization: Bearer <token>
```

**Respuesta esperada:** Métricas de entregas: tiempo promedio, porcentaje de conformidad, totales por mina y transportista.

---

### Prueba 5 — Dashboard con servicio caído (Circuit Breaker)

Apaga ms-inventario y ejecuta:

```
GET http://localhost:8080/api/reportes/dashboard
Authorization: Bearer <token>
```

**Respuesta esperada:** Dashboard con datos parciales y `serviciosNoDisponibles: ["inventario"]`. El sistema no falla, simplemente informa qué no pudo consultar.

---

### Prueba 6 — Historial de reportes

```
GET http://localhost:8080/api/reportes/historial
Authorization: Bearer <token>
```

**Respuesta esperada:** Lista de todos los reportes generados con tiempo de generación y servicios consultados.

---

## Checklist final

- [ ] App arranca en puerto `8088`
- [ ] Tabla `reporte_generado` creada en `reportes_db`
- [ ] `MS-REPORTES` aparece en Eureka `http://localhost:8761`
- [ ] `GET /api/reportes/dashboard` devuelve métricas de los 4 servicios
- [ ] `GET /api/reportes/stock` devuelve inventario real
- [ ] `GET /api/reportes/pedidos` filtra correctamente por fechas
- [ ] `GET /api/reportes/entregas` calcula tiempo promedio y conformidad
- [ ] Si un servicio está caído, el dashboard devuelve datos parciales (no falla)
- [ ] `serviciosNoDisponibles` lista los servicios que no respondieron
- [ ] Cada reporte se guarda en el historial
- [ ] Funciona a través del gateway con token JWT

---

## Qué viene después

El último microservicio nuevo es **ms-mantenimiento** (puerto `8089`). Gestiona los vehículos de transporte y recibe eventos de ms-entrega vía RabbitMQ para actualizar el kilometraje automáticamente cuando se completa una entrega.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
