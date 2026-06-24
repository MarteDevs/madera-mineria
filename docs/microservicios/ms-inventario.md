# Microservicio: ms-inventario

El microservicio `ms-inventario` tiene la tarea crítica de administrar el stock físico de la madera que ingresa y sale de los almacenes y minas. 

## 1. Responsabilidad Principal
- Controlar el stock de los diferentes tipos de madera (Ej: Pino, Eucalipto) asignados a distintas sedes/minas.
- Registrar un historial inmutable de movimientos (kardex) que detalle si hubo ingresos (Entrada) o reducciones (Salida), la cantidad, el motivo y el responsable.
- Actuar como fuente de la verdad para el stock disponible; otros servicios deben consultarle antes de despachar madera.

## 2. Base de Datos
Utiliza la base de datos PostgreSQL `inventario_db`.
Entidades principales:
- `Madera`: Mantiene el registro consolidado (tipo de madera, unidad, precio por unidad, mina asignada, stock actual y stock mínimo).
- `MovimientoInventario`: Guarda un registro de auditoría con la fecha, tipo (ENTRADA/SALIDA) y motivo por cada cambio en el stock de una `Madera`.

## 3. Puntos Críticos de Integración
- Es altamente consumido por **`ms-proveedores`** vía Feign. Cuando un proveedor deja madera, el microservicio de proveedores llama al endpoint `POST /api/inventario/{id}/entrada` para que el stock se incremente.
- **Seguridad en la Concurrencia:** Los métodos de modificación de stock (`reducirStock` y `registrarEntrada`) están marcados con `@Transactional`. Esto evita condiciones de carrera a nivel de base de datos si múltiples servicios intentan actualizar el inventario de la misma madera de manera simultánea.
