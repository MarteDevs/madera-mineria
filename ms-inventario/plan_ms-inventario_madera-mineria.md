# Plan Técnico — ms-inventario
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-inventario · Puerto `8082`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`inventario_db`)

---

## ¿Qué hace este microservicio?

Este es el **primer microservicio a implementar**. Gestiona el catálogo de madera disponible por mina y es la base del sistema: todos los demás microservicios dependerán de él.

| Responsabilidad | Descripción |
|---|---|
| Registrar tipos de madera | Eucalipto, pino, roble con su uso en minería (soporte de galería, entibado, cuadros) |
| Controlar stock por mina | Cantidad disponible por cada tipo de madera en cada mina (Cerro Verde, Antamina, etc.) |
| Exponer datos a otros servicios | `ms-pedidos` llamará a este servicio vía Feign Client para verificar stock |
| Registrar movimientos | Cada entrada o salida genera un historial con fecha, cantidad y motivo |

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Crear los endpoints REST (GET, POST, PUT) |
| Spring Data JPA | Conectarse a PostgreSQL sin escribir SQL manual |
| PostgreSQL Driver | Driver de conexión a la base de datos |
| Lombok | Genera getters, setters y constructores automáticamente |
| Validation | Valida que los datos de entrada sean correctos (`@NotNull`, `@Min`) |
| Spring Boot DevTools | Recarga automática al guardar cambios |

---

## Estructura de archivos

Crea los archivos en este orden exacto. Cada capa depende de la anterior.

```
ms-inventario/src/main/java/com/madera/inventario/
│
├── MsInventarioApplication.java          ← clase principal (ya existe)
│
├── model/
│   ├── Madera.java                        ← entidad principal (crear PRIMERO)
│   ├── MovimientoInventario.java          ← historial de movimientos
│   └── TipoMovimiento.java               ← enum: ENTRADA / SALIDA
│
├── dto/
│   ├── MaderaRequest.java                 ← datos que recibe la API
│   └── MaderaResponse.java               ← datos que devuelve la API
│
├── repository/
│   └── MaderaRepository.java             ← consultas a la base de datos
│
├── service/
│   ├── MaderaService.java                ← interfaz del servicio
│   └── MaderaServiceImpl.java            ← lógica de negocio
│
├── controller/
│   └── MaderaController.java             ← endpoints REST
│
└── exception/
    ├── StockInsuficienteException.java
    └── GlobalExceptionHandler.java       ← manejo de errores personalizado

src/main/resources/
└── application.properties               ← configuración (editar este archivo)
```

> **Regla:** nunca crees el controller antes del service, ni el service antes del repository. El orden importa.

---

## Entidades de la base de datos

### Tabla: `madera`

```java
@Entity
@Table(name = "madera")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Madera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String tipo;              // "eucalipto", "pino", "roble"

    private String uso;               // "soporte_galeria", "entibado", "cuadros"
    private String unidad;            // "m3", "metro_lineal", "unidad"

    @Min(0)
    private Double precioPorUnidad;   // precio en soles

    @Min(0)
    private Integer stockDisponible;

    private Integer stockMinimo;      // alerta cuando baje de este valor
    private String mina;              // "Cerro Verde", "Antamina", "Las Bambas"
    private String proveedor;         // empresa que suministra la madera
    private String estado;            // "DISPONIBLE", "RESERVADO", "AGOTADO"

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
```

### Tabla: `movimiento_inventario`

```java
@Entity
@Table(name = "movimiento_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "madera_id")
    private Madera madera;            // qué madera se movió

    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;      // ENTRADA o SALIDA

    private Integer cantidad;         // unidades que entraron o salieron
    private String motivo;            // "Pedido #23", "Compra a proveedor"
    private String responsable;       // usuario que registró el movimiento

    @CreationTimestamp
    private LocalDateTime fecha;
}
```

### Enum: `TipoMovimiento`

```java
public enum TipoMovimiento {
    ENTRADA,   // llegó madera al almacén
    SALIDA     // se despachó madera hacia la mina
}
```

---

## Endpoints REST

URL base: `http://localhost:8082/api/inventario`

### Madera

| Método | Endpoint | Descripción | Usado por |
|---|---|---|---|
| `GET` | `/api/inventario` | Lista toda la madera disponible | Frontend / Admin |
| `GET` | `/api/inventario/{id}` | Detalle de una madera | Frontend |
| `GET` | `/api/inventario/mina/{mina}` | Madera disponible en una mina | Frontend |
| `GET` | `/api/inventario/tipo/{tipo}` | Buscar por tipo de madera | Frontend |
| `GET` | `/api/inventario/{id}/stock` | Consultar stock actual | **Feign desde ms-pedidos** |
| `POST` | `/api/inventario` | Registrar nueva madera | Admin / Almacén |
| `PUT` | `/api/inventario/{id}` | Actualizar datos de madera | Admin |
| `PUT` | `/api/inventario/{id}/reducir?cantidad=5` | Descontar stock | **Feign desde ms-pedidos** |

### Movimientos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/inventario/{id}/movimientos` | Historial de una madera |
| `POST` | `/api/inventario/{id}/entrada` | Registrar llegada de madera |

> Los endpoints marcados como **Feign desde ms-pedidos** son los más críticos. Deben responder correctamente antes de avanzar al siguiente microservicio.

---

## Configuración

### `application.properties`

```properties
spring.application.name=ms-inventario
server.port=8082

# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/inventario_db
spring.datasource.username=postgres
spring.datasource.password=postgres123

# JPA — crea/actualiza tablas automáticamente
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

### Crear la base de datos en pgAdmin

```sql
CREATE DATABASE inventario_db;
```

---

## Pasos de implementación

Sigue este orden exacto. Cada paso tiene un resultado verificable.

### Paso 1 — Configurar `application.properties` ⏱ 10 min

Editar el archivo que ya existe en `src/main/resources/`. Cambiar nombre de app, puerto, URL de BD y credenciales.

**Verificación:** Correr la aplicación. Debe aparecer en consola:
```
Started MsInventarioApplication in X seconds
Tomcat started on port(s): 8082
```

---

### Paso 2 — Crear la base de datos ⏱ 5 min

En pgAdmin o DBeaver ejecutar:
```sql
CREATE DATABASE inventario_db;
```

**Verificación:** La aplicación corre sin errores de conexión.

---

### Paso 3 — Crear `Madera.java` ⏱ 20 min

Crear en `model/`. Agregar anotaciones `@Entity`, `@Table`, `@Id`, `@GeneratedValue`. Usar `@Data` de Lombok para evitar escribir getters y setters manualmente.

**Verificación:** Al correr la app, JPA crea automáticamente la tabla `madera` en PostgreSQL. Verificar en pgAdmin que la tabla existe con todas las columnas.

---

### Paso 4 — Crear `MovimientoInventario.java` y `TipoMovimiento.java` ⏱ 15 min

Crear en `model/`. El enum `TipoMovimiento` define los valores ENTRADA y SALIDA. La entidad `MovimientoInventario` tiene una relación `@ManyToOne` con `Madera`.

**Verificación:** JPA crea la tabla `movimiento_inventario` con la columna `madera_id` como clave foránea.

---

### Paso 5 — Crear los DTOs ⏱ 15 min

Crear en `dto/`:
- `MaderaRequest.java` — datos que recibe la API al crear/actualizar
- `MaderaResponse.java` — datos que devuelve la API al consultar

Agregar `@NotNull`, `@NotBlank` y `@Min(0)` en los campos obligatorios de `MaderaRequest`.

**Verificación:** Los DTOs compilan sin errores.

---

### Paso 6 — Crear `MaderaRepository.java` ⏱ 10 min

Crear en `repository/`. Extender `JpaRepository<Madera, Long>`. Agregar métodos de consulta:

```java
@Repository
public interface MaderaRepository extends JpaRepository<Madera, Long> {
    List<Madera> findByMina(String mina);
    List<Madera> findByEstado(String estado);
    Optional<Madera> findByTipoAndMina(String tipo, String mina);
    List<Madera> findByStockDisponibleGreaterThan(Integer minimo);
}
```

> Spring Data implementa estos métodos automáticamente por el nombre del método. No escribas SQL.

**Verificación:** La aplicación compila correctamente.

---

### Paso 7 — Crear `MaderaService` y `MaderaServiceImpl` ⏱ 30 min

Crear en `service/`. La interfaz define qué hace el servicio. La implementación contiene la lógica de negocio:

- Validar que hay stock suficiente antes de reducir
- Lanzar `StockInsuficienteException` si no hay stock
- Registrar un `MovimientoInventario` en cada entrada o salida
- Actualizar el estado a "AGOTADO" si el stock llega a 0

**Verificación:** La aplicación compila correctamente.

---

### Paso 8 — Crear `MaderaController.java` ⏱ 25 min

Crear en `controller/`. Mapear cada endpoint con `@GetMapping`, `@PostMapping`, `@PutMapping`. Retornar `ResponseEntity` con el código HTTP correcto:

| Situación | Código HTTP |
|---|---|
| Consulta exitosa | `200 OK` |
| Recurso creado | `201 Created` |
| Recurso no encontrado | `404 Not Found` |
| Stock insuficiente | `400 Bad Request` |
| Error del servidor | `500 Internal Server Error` |

**Verificación:** Probar `GET http://localhost:8082/api/inventario` en el navegador. Debe devolver `[]` (lista vacía).

---

### Paso 9 — Crear `GlobalExceptionHandler.java` ⏱ 15 min

Crear en `exception/`. Usar `@RestControllerAdvice` para capturar excepciones y devolver JSON con mensaje de error claro en vez de un error 500 genérico.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleStockInsuficiente(
            StockInsuficienteException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Stock insuficiente");
        error.put("mensaje", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Recurso no encontrado");
        error.put("mensaje", ex.getMessage());
        return ResponseEntity.status(404).body(error);
    }
}
```

**Verificación:** Al intentar reducir más stock del disponible, la API devuelve `400 Bad Request` con un JSON de error claro.

---

## Pruebas con Postman

### Prueba 1 — Crear una madera

```
POST http://localhost:8082/api/inventario
Content-Type: application/json
```

```json
{
  "tipo": "eucalipto",
  "uso": "soporte_galeria",
  "unidad": "m3",
  "precioPorUnidad": 280.50,
  "stockDisponible": 150,
  "stockMinimo": 20,
  "mina": "Cerro Verde",
  "proveedor": "Maderería del Sur SAC",
  "estado": "DISPONIBLE"
}
```

**Respuesta esperada:** `201 Created` con el objeto creado y un `id` asignado.

---

### Prueba 2 — Listar toda la madera

```
GET http://localhost:8082/api/inventario
```

**Respuesta esperada:** `200 OK` con array JSON de todas las maderas registradas.

---

### Prueba 3 — Consultar stock (el más importante)

```
GET http://localhost:8082/api/inventario/1/stock
```

**Respuesta esperada:** `200 OK` con el número `150`. Este endpoint es el que `ms-pedidos` llamará automáticamente vía Feign Client.

---

### Prueba 4 — Reducir stock

```
PUT http://localhost:8082/api/inventario/1/reducir?cantidad=10
```

**Respuesta esperada:** `200 OK`. Luego hacer GET al stock y verificar que pasó de `150` a `140`.

---

### Prueba 5 — Probar manejo de error

```
PUT http://localhost:8082/api/inventario/1/reducir?cantidad=9999
```

**Respuesta esperada:** `400 Bad Request` con el siguiente JSON:

```json
{
  "error": "Stock insuficiente",
  "mensaje": "Stock disponible: 140, solicitado: 9999"
}
```

Si devuelve este JSON, el `GlobalExceptionHandler` funciona correctamente.

---

## Checklist final antes de avanzar a ms-pedidos

Verifica que todas estas condiciones se cumplen antes de crear el siguiente microservicio:

- [ ] La aplicación arranca en el puerto `8082` sin errores
- [ ] Las tablas `madera` y `movimiento_inventario` existen en PostgreSQL
- [ ] `POST /api/inventario` devuelve `201` con el objeto creado
- [ ] `GET /api/inventario` devuelve lista de maderas
- [ ] `GET /api/inventario/{id}/stock` devuelve solo el número del stock
- [ ] `PUT /api/inventario/{id}/reducir?cantidad=10` descuenta correctamente
- [ ] `PUT /api/inventario/{id}/reducir?cantidad=9999` devuelve `400` con mensaje de error

---

## Qué viene después

Una vez completado `ms-inventario`, el siguiente microservicio es **ms-pedidos** (puerto `8083`). Este microservicio:

1. Llamará a `GET /api/inventario/{id}/stock` vía **Feign Client** antes de crear un pedido
2. Publicará un evento en **RabbitMQ** cuando se cree un pedido (`OrderCreated`)
3. Usará **Resilience4J** para manejar el caso en que `ms-inventario` no responda

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
