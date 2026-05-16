# Plan Técnico — ms-auth
## Sistema de Gestión de Pedidos y Entregas de Madera para Minería

> **Curso:** Desarrollo de Aplicaciones Web II · CIBERTEC  
> **Microservicio:** ms-auth · Puerto `8085`  
> **Spring Boot:** 3.5.14 · Java 21 · Maven  
> **Base de datos:** PostgreSQL (`auth_db`)  
> **Unidad del sílabo:** Unidad 2 — Seguridad en Microservicios

---

## ¿Qué hace este microservicio?

Es el **guardián del sistema**. Gestiona usuarios, roles y tokens JWT. Cualquier request a ms-inventario, ms-pedidos o ms-notificaciones debe incluir un token válido generado por este servicio. Sin token, acceso denegado.

| Responsabilidad | Descripción |
|---|---|
| Registrar usuarios | Crear cuentas con rol asignado (ADMIN, ALMACEN, COMPRAS, TRANSPORTE) |
| Login | Validar credenciales y generar token JWT |
| Validar tokens | Verificar que el token es válido y no ha expirado |
| Control de roles | Cada rol tiene permisos diferentes en el sistema |
| Cambiar contraseña | El usuario puede actualizar su propia contraseña |

---

## Roles del sistema

| Rol | Permisos |
|---|---|
| `ROLE_ADMIN` | Todo: usuarios, inventario, pedidos, reportes |
| `ROLE_ALMACEN` | Gestionar inventario, aprobar/rechazar pedidos, ver notificaciones |
| `ROLE_COMPRAS` | Crear pedidos, ver sus propios pedidos, ver inventario |
| `ROLE_TRANSPORTE` | Ver pedidos aprobados, actualizar estado de entrega |

---

## Flujo de autenticación

```
Usuario hace POST /api/auth/login
        │
        ▼
ms-auth valida usuario y contraseña en auth_db
        │
   ┌────┴────┐
   NO        SÍ
   │         │
   ▼         ▼
401      Genera token JWT
Unauthorized    │
                ▼
        Devuelve token al cliente
                │
                ▼
        Cliente incluye token en cada request
        Authorization: Bearer <token>
                │
                ▼
        Cada microservicio valida el token
        (en la Fase del api-gateway)
```

---

## Tecnologías utilizadas

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | Endpoints de registro, login y gestión de usuarios |
| Spring Data JPA | Persistencia de usuarios en PostgreSQL |
| PostgreSQL Driver | Conexión a `auth_db` |
| Lombok | Getters, setters y constructores automáticos |
| Validation | Validar datos de entrada (`@NotBlank`, `@Email`) |
| DevTools | Recarga automática |
| **Spring Security** | Encriptar contraseñas, proteger endpoints |
| **OAuth2 Resource Server** | Validar tokens JWT en cada request |
| **jjwt 0.11.5** | Generar y validar tokens JWT |

---

## Estructura de archivos

```
ms-auth/src/main/java/com/madera/auth/
│
├── MsAuthApplication.java
│
├── model/
│   ├── Usuario.java                      ← entidad principal (crear PRIMERO)
│   └── Rol.java                          ← enum de roles
│
├── dto/
│   ├── RegisterRequest.java              ← datos para registrar usuario
│   ├── LoginRequest.java                 ← email + password
│   ├── AuthResponse.java                 ← token JWT + datos del usuario
│   └── UsuarioResponse.java             ← datos públicos del usuario
│
├── repository/
│   └── UsuarioRepository.java
│
├── config/
│   ├── SecurityConfig.java              ← configuración de Spring Security
│   └── ApplicationConfig.java           ← beans de seguridad
│
├── security/
│   ├── JwtService.java                  ← generar y validar tokens JWT
│   └── JwtAuthenticationFilter.java     ← filtro que valida el token en cada request
│
├── service/
│   ├── AuthService.java                 ← interfaz
│   └── AuthServiceImpl.java             ← lógica de registro y login
│
├── controller/
│   └── AuthController.java              ← endpoints REST
│
└── exception/
    └── GlobalExceptionHandler.java
```

> **Orden de creación:** model → dto → repository → security/JwtService → config → service → controller → exception

---

## Entidades de la base de datos

### Enum: `Rol`

```java
public enum Rol {
    ROLE_ADMIN,
    ROLE_ALMACEN,
    ROLE_COMPRAS,
    ROLE_TRANSPORTE
}
```

### Entidad: `Usuario`

```java
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;             // se guarda encriptada con BCrypt

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private String mina;                 // a qué mina pertenece (para COMPRAS)
    private boolean activo;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    // ── Métodos de UserDetails (requeridos por Spring Security) ──────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol.name()));
    }

    @Override
    public String getUsername() {
        return email;                    // el email es el username
    }

    @Override
    public boolean isAccountNonExpired()    { return true; }

    @Override
    public boolean isAccountNonLocked()     { return true; }

    @Override
    public boolean isCredentialsNonExpired(){ return true; }

    @Override
    public boolean isEnabled()              { return activo; }
}
```

---

## DTOs

### `RegisterRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String password;

    private Rol rol;                     // si no se envía, asignar ROLE_COMPRAS
    private String mina;                 // obligatorio si rol es COMPRAS
}
```

### `LoginRequest.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
```

### `AuthResponse.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String tipo;             // "Bearer"
    private String email;
    private String nombre;
    private String rol;
    private Long expiracion;         // timestamp de expiración
}
```

---

## Seguridad JWT

### `JwtService.java`

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    // ─── GENERAR TOKEN ───────────────────────────────────────────────────────
    public String generarToken(UserDetails userDetails) {
        return generarToken(new HashMap<>(), userDetails);
    }

    public String generarToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {

        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // ─── VALIDAR TOKEN ───────────────────────────────────────────────────────
    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return username.equals(userDetails.getUsername())
            && !esTokenExpirado(token);
    }

    // ─── EXTRAER DATOS ───────────────────────────────────────────────────────
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public <T> T extraerClaim(String token,
                               Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosLosClaims(token);
        return claimsResolver.apply(claims);
    }

    // ─── HELPERS PRIVADOS ────────────────────────────────────────────────────
    private boolean esTokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSignKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(secretKey.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### `JwtAuthenticationFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header o no empieza con "Bearer ", pasar al siguiente filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extraerUsername(jwt);

        // Si hay email y no hay autenticación previa en el contexto
        if (userEmail != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.esTokenValido(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                        .buildDetails(request));
                SecurityContextHolder.getContext()
                    .setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## Configuración de Spring Security

### `ApplicationConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UsuarioRepository usuarioRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByEmail(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos — no requieren token
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register"
                ).permitAll()
                // Solo ADMIN puede listar y gestionar usuarios
                .requestMatchers("/api/auth/usuarios/**")
                    .hasAuthority("ROLE_ADMIN")
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
```

---

## Repositorio

### `UsuarioRepository.java`

```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findByRol(Rol rol);
    List<Usuario> findByMina(String mina);
    List<Usuario> findByActivo(boolean activo);
}
```

---

## Lógica de negocio

### `AuthServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository     usuarioRepo;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    // ─── REGISTRO ────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (usuarioRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                "El email ya está registrado: " + request.getEmail());
        }

        Rol rol = request.getRol() != null
            ? request.getRol()
            : Rol.ROLE_COMPRAS;        // rol por defecto

        Usuario usuario = Usuario.builder()
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .rol(rol)
            .mina(request.getMina())
            .activo(true)
            .build();

        usuarioRepo.save(usuario);

        String token = jwtService.generarToken(usuario);

        return AuthResponse.builder()
            .token(token)
            .tipo("Bearer")
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .rol(usuario.getRol().name())
            .expiracion(System.currentTimeMillis() + 86400000L)
            .build();
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {

        // Spring Security valida email y password automáticamente
        // Si son incorrectos lanza BadCredentialsException
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        Usuario usuario = usuarioRepo.findByEmail(request.getEmail())
            .orElseThrow(() ->
                new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generarToken(usuario);

        return AuthResponse.builder()
            .token(token)
            .tipo("Bearer")
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .rol(usuario.getRol().name())
            .expiracion(System.currentTimeMillis() + 86400000L)
            .build();
    }

    // ─── LISTAR USUARIOS ─────────────────────────────────────────────────────
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    public List<UsuarioResponse> listarPorRol(Rol rol) {
        return usuarioRepo.findByRol(rol).stream()
            .map(this::mapToResponse).toList();
    }

    // ─── DESACTIVAR USUARIO ──────────────────────────────────────────────────
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepo.save(usuario);
    }

    // ─── MAPPER ──────────────────────────────────────────────────────────────
    private UsuarioResponse mapToResponse(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setApellido(u.getApellido());
        r.setEmail(u.getEmail());
        r.setRol(u.getRol().name());
        r.setMina(u.getMina());
        r.setActivo(u.isActivo());
        r.setFechaCreacion(u.getFechaCreacion());
        return r;
    }
}
```

---

## Endpoints REST

URL base: `http://localhost:8085/api/auth`

### `AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Públicos — no requieren token
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Protegidos — requieren token
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<List<UsuarioResponse>> porRol(
            @PathVariable Rol rol) {
        return ResponseEntity.ok(authService.listarPorRol(rol));
    }

    @PutMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        authService.desactivarUsuario(id);
        return ResponseEntity.ok().build();
    }
}
```

### Tabla de endpoints

| Método | Endpoint | Auth requerida | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ Pública | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | ❌ Pública | Login y obtener token JWT |
| `GET` | `/api/auth/usuarios` | ✅ ROLE_ADMIN | Listar todos los usuarios |
| `GET` | `/api/auth/usuarios/rol/{rol}` | ✅ ROLE_ADMIN | Filtrar por rol |
| `PUT` | `/api/auth/usuarios/{id}/desactivar` | ✅ ROLE_ADMIN | Desactivar usuario |

---

## Manejo de errores

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity.status(401).body(Map.of(
            "error", "Credenciales inválidas",
            "mensaje", "Email o contraseña incorrectos"
        ));
    }

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
        String mensaje = ex.getBindingResult()
            .getFieldErrors().stream()
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

### Paso 1 — Crear `Rol.java` ⏱ 5 min

Enum con los 4 roles del sistema.

**Verificación:** Compila sin errores.

---

### Paso 2 — Crear `Usuario.java` ⏱ 25 min

Entidad que implementa `UserDetails`. Este es el paso más delicado — asegúrate de implementar todos los métodos de la interfaz.

**Verificación:** JPA crea la tabla `usuario` en `auth_db`. Verificar en pgAdmin.

---

### Paso 3 — Crear los DTOs ⏱ 20 min

Crear `RegisterRequest`, `LoginRequest`, `AuthResponse` y `UsuarioResponse` en `dto/`.

**Verificación:** Compilan sin errores.

---

### Paso 4 — Crear `UsuarioRepository.java` ⏱ 10 min

Extender `JpaRepository`. El método `findByEmail` es el más importante — Spring Security lo usará para cargar el usuario.

**Verificación:** Compila sin errores.

---

### Paso 5 — Crear `JwtService.java` ⏱ 25 min

Clase que genera y valida tokens JWT. Usa la clave secreta del `application.properties`.

**Verificación:** Compila sin errores. No hay forma de probarla sola todavía.

---

### Paso 6 — Crear `JwtAuthenticationFilter.java` ⏱ 20 min

Filtro que intercepta cada request, extrae el token del header `Authorization` y valida que sea correcto.

**Verificación:** Compila sin errores.

---

### Paso 7 — Crear `ApplicationConfig.java` ⏱ 15 min

Define los beans de Spring Security: `UserDetailsService`, `AuthenticationProvider`, `PasswordEncoder` y `AuthenticationManager`.

**Verificación:** La app arranca sin errores de contexto de Spring.

---

### Paso 8 — Crear `SecurityConfig.java` ⏱ 20 min

Define qué endpoints son públicos y cuáles requieren token. Conecta el filtro JWT con Spring Security.

**Verificación:** Al intentar `GET http://localhost:8085/api/auth/usuarios` sin token devuelve `403 Forbidden`.

---

### Paso 9 — Crear `AuthService` y `AuthServiceImpl` ⏱ 35 min

Lógica de registro (encripta contraseña, genera token) y login (valida credenciales, genera token).

**Verificación:** Compila sin errores.

---

### Paso 10 — Crear `AuthController.java` ⏱ 15 min

Los 5 endpoints. Los dos primeros son públicos, los demás protegidos.

**Verificación:** `POST http://localhost:8085/api/auth/register` registra un usuario y devuelve un token.

---

### Paso 11 — Crear `GlobalExceptionHandler.java` ⏱ 15 min

Capturar credenciales inválidas, errores de validación y errores generales.

**Verificación:** Login con contraseña incorrecta devuelve `401` con mensaje claro.

---

## Pruebas con Postman

### Prueba 1 — Registrar un usuario ADMIN

```
POST http://localhost:8085/api/auth/register
Content-Type: application/json
```

```json
{
  "nombre": "Carlos",
  "apellido": "Mamani",
  "email": "admin@madera.com",
  "password": "admin123",
  "rol": "ROLE_ADMIN"
}
```

**Respuesta esperada:** `201 Created` con el token JWT.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "admin@madera.com",
  "nombre": "Carlos",
  "rol": "ROLE_ADMIN",
  "expiracion": 1234567890000
}
```

---

### Prueba 2 — Registrar usuario de COMPRAS (para Cerro Verde)

```
POST http://localhost:8085/api/auth/register
Content-Type: application/json
```

```json
{
  "nombre": "Juan",
  "apellido": "Perez",
  "email": "compras@cerroverde.com",
  "password": "compras123",
  "rol": "ROLE_COMPRAS",
  "mina": "Cerro Verde"
}
```

**Respuesta esperada:** `201 Created` con token JWT.

---

### Prueba 3 — Login correcto

```
POST http://localhost:8085/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@madera.com",
  "password": "admin123"
}
```

**Respuesta esperada:** `200 OK` con un nuevo token JWT.

> Copia este token — lo usarás en las siguientes pruebas como `Bearer <token>`.

---

### Prueba 4 — Login con contraseña incorrecta

```
POST http://localhost:8085/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@madera.com",
  "password": "wrongpassword"
}
```

**Respuesta esperada:** `401 Unauthorized` con mensaje "Email o contraseña incorrectos".

---

### Prueba 5 — Listar usuarios CON token

```
GET http://localhost:8085/api/auth/usuarios
Authorization: Bearer <token-del-admin>
```

**Respuesta esperada:** `200 OK` con la lista de usuarios registrados.

---

### Prueba 6 — Listar usuarios SIN token

```
GET http://localhost:8085/api/auth/usuarios
```

**Respuesta esperada:** `403 Forbidden`. El endpoint está protegido.

---

### Prueba 7 — Filtrar por rol

```
GET http://localhost:8085/api/auth/usuarios/rol/ROLE_COMPRAS
Authorization: Bearer <token-del-admin>
```

**Respuesta esperada:** `200 OK` con los usuarios que tienen rol COMPRAS.

---

### Prueba 8 — Registrar usuario de ALMACEN

```
POST http://localhost:8085/api/auth/register
Content-Type: application/json
```

```json
{
  "nombre": "Rosa",
  "apellido": "Garcia",
  "email": "almacen@madera.com",
  "password": "almacen123",
  "rol": "ROLE_ALMACEN"
}
```

**Respuesta esperada:** `201 Created`. Este usuario podrá aprobar pedidos.

---

## Checklist final antes de avanzar a ms-entrega

- [ ] La aplicación arranca en el puerto `8085` sin errores
- [ ] La tabla `usuario` existe en `auth_db`
- [ ] `POST /api/auth/register` crea usuario y devuelve token JWT
- [ ] `POST /api/auth/login` con credenciales correctas devuelve token JWT
- [ ] `POST /api/auth/login` con credenciales incorrectas devuelve `401`
- [ ] `GET /api/auth/usuarios` sin token devuelve `403`
- [ ] `GET /api/auth/usuarios` con token de ADMIN devuelve lista de usuarios
- [ ] La contraseña se guarda encriptada en la BD (verificar en pgAdmin — no debe ser texto plano)
- [ ] Token JWT tiene formato `eyJ...` (tres partes separadas por puntos)

---

## Usuarios de prueba recomendados para el proyecto

Registra estos 4 usuarios para tener un juego de pruebas completo:

| Nombre | Email | Password | Rol | Mina |
|---|---|---|---|---|
| Carlos Mamani | admin@madera.com | admin123 | ROLE_ADMIN | — |
| Rosa Garcia | almacen@madera.com | almacen123 | ROLE_ALMACEN | — |
| Juan Perez | compras@cerroverde.com | compras123 | ROLE_COMPRAS | Cerro Verde |
| Pedro Quispe | transporte@madera.com | trans123 | ROLE_TRANSPORTE | — |

---

## Qué viene después

El siguiente microservicio es **ms-entrega** (puerto `8086`). Gestiona el despacho físico de madera hacia la mina:

1. Asignar transportista y vehículo al pedido aprobado
2. Registrar la ruta: almacén → mina
3. Actualizar estado: `PREPARANDO` → `EN_RUTA` → `ENTREGADO`
4. Confirmar recepción en la mina

Con ms-auth + ms-entrega completados, más el **api-gateway** final, tendrás el sistema completo listo para la **Evaluación Final (EF)**.

---

*Plan elaborado para el proyecto del curso Desarrollo de Aplicaciones Web II — CIBERTEC 2026*
