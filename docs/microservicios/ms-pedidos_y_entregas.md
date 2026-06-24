# Microservicios: ms-pedidos y ms-entregas

Estos dos microservicios trabajan fuertemente acoplados (a través de eventos y llamadas REST) para manejar la etapa final del proceso: Recibir la necesidad de madera desde la mina y llevarla físicamente.

## ms-pedidos (Puerto: 8085)
- **Responsabilidad:** Recepción de los requerimientos. Las distintas áreas de la mina solicitan "X" cantidad de un tipo de madera.
- **Base de Datos:** `pedidos_db`.
- **Características:**
  - Posee una máquina de estados para el pedido: `CREADO` -> `APROBADO` -> `PREPARANDO` -> `EN_RUTA` -> `ENTREGADO`.
  - Cuando un pedido es `APROBADO` (por un Admin), `ms-pedidos` se comunica con `ms-inventario` (vía OpenFeign) para consultar si hay suficiente stock disponible antes de continuar.
  - Genera eventos a RabbitMQ con cada cambio de estado (que son escuchados por `ms-notificaciones` para alertar a los usuarios).

## ms-entregas (Puerto: 8086)
- **Responsabilidad:** La logística del despacho. Traduce un Pedido (solicitud) en un viaje físico en camión.
- **Base de Datos:** `entregas_db`.
- **Características:**
  - **Asignación de Vehículo:** Se conecta con `ms-mantenimiento` para buscar camiones `OPERATIVOS` (Feign Client) y le asigna un chofer y un vehículo a la entrega.
  - Al completar el despacho (`ENTREGADO`), notifica de vuelta a `ms-pedidos` para cerrar el ciclo de vida de la solicitud.
  - Esta separación evita que el código de reglas comerciales de la mina (pedidos) se mezcle con el código de administración de fletes y logística vehicular (entregas).
