# Detalle de Microservicios

Cada microservicio del sistema está diseñado bajo los principios de **Single Responsibility** y operan de forma autónoma. Todos comparten el stack de **Spring Boot 3.2.x**, **Java 17**, **PostgreSQL** y exponen métricas vía **Spring Boot Actuator**.

## 1. ms-auth (Puerto: 8081)
- **Responsabilidad:** Gestionar la autenticación de usuarios y la generación de Tokens JWT (JSON Web Tokens).
- **Base de Datos:** `auth_db`.
- **Características:** 
  - Contiene las entidades `Usuario` y `Role`.
  - Configurado con Spring Security para manejar contraseñas encriptadas (BCrypt).
  - Devuelve un token JWT válido (y el rol del usuario) cuando se hace un POST exitoso a `/api/auth/login`. Este token se usa luego en el Gateway.

## 2. ms-inventario (Puerto: 8083)
- **Responsabilidad:** Control del stock físico de madera extraída de las minas y de los ingresos de proveedores.
- **Base de Datos:** `inventario_db`.
- **Características:**
  - Control de unidades, tipo de madera y sede de mina.
  - Guarda un historial inmutable de `MovimientoInventario` (Entrada/Salida) para auditorías.
  - Proporciona endpoints para que otros servicios (como `ms-proveedores` o `ms-pedidos`) verifiquen y actualicen el stock de madera.

## 3. ms-proveedores (Puerto: 8084)
- **Responsabilidad:** Gestión del directorio de proveedores, contratos de suministro y registro de entregas.
- **Base de Datos:** `proveedores_db`.
- **Características:**
  - Valida contratos vigentes antes de permitir el registro de una entrega (para respetar los precios pactados).
  - Mantiene comunicación síncrona (Feign Client) con `ms-inventario` para sincronizar las cantidades físicas reales tras una entrega.
  - Emite eventos a RabbitMQ (`proveedor.entrega.registrada`).

## 4. ms-mantenimiento (Puerto: 8089)
- **Responsabilidad:** Gestión de la flota de vehículos, kilometraje y alertas de mantenimiento preventivo.
- **Base de Datos:** `mantenimiento_db`.
- **Características:**
  - Registra el kilometraje de los camiones, furgones, etc.
  - Genera notificaciones y cambia estados cuando un vehículo requiere mantenimiento basado en límites configurados.
  - Escucha eventos asíncronos en RabbitMQ (ej: un vehículo regresó de un viaje largo y se actualiza su kilometraje).

## 5. ms-pedidos (Puerto: 8085) & ms-entregas (Puerto: 8086)
*(Microservicios secundarios para la gestión final de venta/distribución de la madera)*
- **ms-pedidos:** Gestiona las solicitudes de los clientes finales.
- **ms-entregas:** Gestiona la asignación de un Vehículo (del `ms-mantenimiento`) para llevar el pedido al destino.
- Utilizan RabbitMQ para comunicarse: Un pedido cambia de estado de "CREADO" a "EN RUTA" cuando `ms-entregas` confirma la asignación del transporte.

## Datos de Semilla (Data Seeding)
Se ha implementado una carga inicial de datos en las bases de datos vacías para agilizar las pruebas y el desarrollo.
- **Estrategia:** A nivel de Spring Boot, se usa `data.sql` sumado a `spring.jpa.defer-datasource-initialization=true`. Esto permite que Hibernate cree primero las tablas automáticamente (`ddl-auto=update`), y luego Spring inserte la data por defecto (vehículos de prueba, maderas por defecto, y proveedores semilla) utilizando `ON CONFLICT DO NOTHING` para evitar duplicados en reinicios.
