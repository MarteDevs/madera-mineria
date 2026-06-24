# Microservicio: ms-proveedores

El microservicio `ms-proveedores` administra la relación con entidades externas (aserraderos, madereras) que suplen a la mina, asegurando que las compras respeten acuerdos comerciales (contratos).

## 1. Responsabilidad Principal
- Mantener un directorio maestro de Proveedores (RUC, razón social, contactos).
- Gestionar los Contratos de suministro (Precios pactados por cada tipo de madera, vigencia y rangos de entrega).
- Registrar la Entrega Física de madera en las instalaciones (con guías de remisión).

## 2. Base de Datos
Utiliza la base de datos PostgreSQL `proveedores_db`.
Entidades principales:
- `Proveedor`: Datos de contacto fiscales.
- `Contrato`: Atado a un Proveedor. Establece el precio (`precioPactado`) para un tipo de madera específico durante un periodo de tiempo.
- `EntregaProveedor`: El registro de que un camión del proveedor llegó. Calcula el costo total basándose en el precio pactado en el contrato.

## 3. Complejidad y Comunicación
Este es uno de los microservicios con la lógica más interconectada:
- **Validación Estricta:** No permite registrar una entrega si el proveedor no cuenta con un contrato *VIGENTE* para el tipo de madera que intenta entregar.
- **Circuit Breaker (Resilience4j) y Feign:** Al registrar la entrega, invoca sincrónicamente a `ms-inventario`. Si `ms-inventario` falla, usa un método *Fallback* que guarda la entrega pero marca en base de datos un flag de que "falló la sincronización de stock", informándole al usuario del problema.
- **Eventos:** Publica en RabbitMQ el evento de `EntregaEvent` para que otros servicios puedan llevar contabilidad de gastos o enviar notificaciones a Finanzas.
