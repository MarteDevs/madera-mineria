# Microservicio: ms-notificaciones

El microservicio `ms-notificaciones` es el corazón del sistema de avisos en tiempo real de **Madera & Minería**. Su objetivo es escuchar los eventos importantes que ocurren en el resto del sistema (por ejemplo, cuando el estado de un pedido cambia) y notificar a los usuarios en el frontend (Almacén, Compras, Admin) de forma instantánea sin saturar la red con peticiones continuas.

## 1. Arquitectura y Patrones Implementados

Este microservicio combina dos patrones fundamentales:
- **Event-Driven Architecture (RabbitMQ):** Para recibir los avisos del backend de manera asíncrona.
- **Server-Sent Events (SSE):** Para enviar las notificaciones al frontend (Navegador) en tiempo real, manteniendo una conexión HTTP abierta de una sola vía.

## 2. Recepción de Eventos (RabbitMQ)

El servicio actúa como un **Consumidor (Subscriber)**. 
- En el archivo `PedidoEventListener.java`, existe un método anotado con `@RabbitListener(queues = RabbitMQConfig.QUEUE)`.
- Esto significa que `ms-notificaciones` está permanentemente escuchando la cola `pedido.queue`. 
- Cada vez que el microservicio de **Pedidos** publica un mensaje diciendo "El pedido #10 ha sido EN RUTA", RabbitMQ empuja este mensaje JSON al Listener.
- El mensaje se deserializa automáticamente en un objeto Java llamado `PedidoEvent`.

## 3. Lógica de Procesamiento (`NotificacionServiceImpl.java`)

Una vez que el Listener recibe el `PedidoEvent`, lo envía al servicio `procesarEvento(PedidoEvent evento)` el cual hace lo siguiente:

1. **Generación del Mensaje:** Evalúa el estado del pedido (CREADO, APROBADO, RECHAZADO, PREPARANDO, EN_RUTA, ENTREGADO) y formatea un mensaje legible para el humano. Por ejemplo:
   - Si es `CREADO`: *"Nuevo pedido #10: 50 unidades de Eucalipto solicitadas por la mina M1. Requiere aprobación."*
   - Si es `EN_RUTA`: *"Pedido #10 en ruta: 50 unidades de Eucalipto en tránsito hacia la mina M1."*
2. **Determinación del Destinatario:** Define quién debe leer este mensaje.
   - Si es un rechazo, notifica al departamento de `COMPRAS`.
   - Si es una creación o entrega, notifica a `ALMACEN`.
3. **Persistencia:** Guarda la notificación en la base de datos PostgreSQL (`notificaciones_db`) con el estado `PENDIENTE`.

## 4. Emisión en Tiempo Real (Server-Sent Events - SSE)

Una vez guardada en BD, se invoca a `sseService.enviarNotificacion(guardada)`.
- **SseService** mantiene un registro en memoria (un `Map` o lista concurrente) de todos los clientes frontend que están conectados al endpoint GET `/api/notificaciones/stream`.
- Cuando entra una nueva notificación, el servicio recorre todos los clientes conectados y les envía un *push* del objeto JSON convertido en un string `data: { ... } \n\n`.
- **Ventaja de SSE:** A diferencia de WebSockets (que es bidireccional), SSE es unidireccional (Servidor a Cliente) y usa HTTP estándar. Es perfecto para notificaciones porque el cliente solo necesita *escuchar* y no enviar nada por ese canal.

## 5. Resumen de Flujo
`ms-pedidos` (Emisor) -> `RabbitMQ` (Exchange -> Queue) -> `ms-notificaciones` (Listener) -> Guardado en BD -> Emisión Push vía `SSE` -> `madera-frontend` (Alerta Visual).
