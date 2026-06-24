# Patrones de Comunicación en Microservicios

Dado que cada microservicio encapsula su propia base de datos y lógica, es inevitable que en ciertos flujos de negocio (como el registro de una entrega por parte de un proveedor), los microservicios deban hablar entre sí. 

En **Madera & Minería**, hemos implementado tres patrones principales para lidiar con esta complejidad.

## 1. Comunicación Síncrona (Spring Cloud OpenFeign)

Se utiliza cuando un microservicio **depende absolutamente** de la respuesta o de una acción en tiempo real de otro microservicio para poder continuar su flujo. 

**Ejemplo Implementado:** `ms-proveedores` comunicándose con `ms-inventario`.
- Cuando un proveedor registra una entrega en `ms-proveedores`, el stock físico de la madera debe aumentar.
- Se ha creado una interfaz `InventarioClient` anotada con `@FeignClient(name = "ms-inventario")`.
- **Ventaja:** Feign abstrae la lógica HTTP. Busca en Eureka la IP de `ms-inventario` y hace la petición REST `/api/inventario/{id}/entrada`. Para el desarrollador, es como llamar a un método de Java local.

## 2. Tolerancia a Fallos (Resilience4j Circuit Breaker)

Dado que las llamadas por red pueden fallar (latencia, servicio apagado), usamos el patrón de **Cortocircuito (Circuit Breaker)**. 

**Ejemplo Implementado:**
Si `ms-inventario` está caído o saturado, y `ms-proveedores` intenta actualizar el stock mediante Feign, la petición fallará. Sin un cortocircuito, `ms-proveedores` se quedaría colgado esperando o mostraría un error 500 feo al usuario, causando una falla en cascada.
- Usamos `@CircuitBreaker(name = "inventario", fallbackMethod = "fallbackRegistrarEntrega")`.
- **El Fallback:** Es un método alternativo. Si la llamada Feign falla, el código intercepta la excepción, guarda el registro de la entrega localmente (para no perder el trabajo del proveedor) y arroja una excepción controlada advirtiendo: *"La entrega fue registrada pero el stock no pudo actualizarse en el inventario"*.

## 3. Comunicación Asíncrona (RabbitMQ)

Se utiliza cuando un microservicio quiere avisar que "algo sucedió", pero **no le importa** quién lo escuche ni necesita una respuesta inmediata (patrón Publicador/Suscriptor y Coreografía de Eventos).

**Ejemplo Implementado:** Notificación de Mantenimientos Terminados.
- **Flujo:** Cuando se finaliza un mantenimiento en `ms-mantenimiento`, se utiliza un `AmqpTemplate` para enviar un mensaje (JSON) al *Exchange* (el buzón central) de RabbitMQ con la clave de ruta (Routing Key) `mantenimiento.completado`.
- `ms-mantenimiento` termina su trabajo ahí, responde HTTP 200 al frontend y se olvida del tema. Es altamente eficiente.
- Por otro lado, un servicio de notificaciones o logs puede tener un método con `@RabbitListener` que esté suscrito a esa cola. Cuando RabbitMQ recibe el mensaje, se lo empuja al Listener, quien procede a enviar un correo electrónico o actualizar estadísticas, en segundo plano, sin afectar el rendimiento del usuario final.
