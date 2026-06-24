# Microservicio: ms-mantenimiento

El microservicio `ms-mantenimiento` gestiona los activos logísticos (vehículos) asegurando su disponibilidad y óptimo funcionamiento para las entregas de madera.

## 1. Responsabilidad Principal
- Llevar un inventario de los vehículos propios (camiones, furgones, camionetas) incluyendo datos operativos (placa, carga útil en m3, conductor).
- Monitorear el uso (kilometraje actual) y generar alertas automáticas cuando un vehículo requiera pasar por el taller.
- Deshabilitar temporalmente vehículos en reparación para evitar que `ms-entregas` los asigne a rutas activas.

## 2. Base de Datos
Utiliza la base de datos PostgreSQL `mantenimiento_db`.
Entidades principales:
- `Vehiculo`: Posee los datos base (placa, marca), datos de desgaste (`kmActual`, `kmUltimoMantenimiento`) y estado operativo (`OPERATIVO`, `MANTENIMIENTO`, `INACTIVO`).

## 3. Lógica de Negocio Destacada
- Configurable por Propiedades: Los límites de tolerancia para las alertas preventivas (ej: `mantenimiento.km-limite=5000`) se inyectan dinámicamente mediante `@Value` desde el archivo `application.properties`.
- Actualización Inteligente: Cuando un vehículo realiza una entrega, el kilometraje sube. Si el nuevo kilometraje supera el umbral de `kmProximoMantenimiento`, el sistema automáticamente activa el flag `requiereMantenimiento = true`.
- Escucha Eventos (RabbitMQ): Puede integrarse como oyente de eventos de llegada (`ms-entregas`) para sumar kilometrajes de manera asíncrona sin intervención humana.
