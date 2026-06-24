# Microservicio: ms-auth

El microservicio `ms-auth` es el responsable de la seguridad perimetral de las identidades en el sistema **Madera & Minería**.

## 1. Responsabilidad Principal
- Autenticación de usuarios contra la base de datos (Login).
- Generación y firmado de tokens JWT (JSON Web Tokens).
- Gestión de Usuarios y Roles (RBAC - Role Based Access Control).

## 2. Base de Datos
Utiliza la base de datos PostgreSQL `auth_db`.
Entidades principales:
- `Usuario`: Almacena el correo, contraseña encriptada (BCrypt) y nombre completo.
- El rol se almacena internamente para definir los permisos (Ej: `ROLE_ADMIN`, `ROLE_ALMACEN`).

## 3. Flujo de Autenticación
1. El usuario en el frontend envía un POST a `/api/auth/login` con sus credenciales.
2. La petición pasa por el API Gateway, que la enruta hacia `ms-auth`.
3. `ms-auth` verifica el hash de la contraseña usando Spring Security.
4. Si es correcto, genera un JWT firmado con un secreto (Secret Key) que incluye el email y el rol del usuario como *claims* (cargas útiles) del token.
5. El token es retornado al frontend, el cual lo guardará en el `localStorage` (Pinia) y lo enviará en la cabecera `Authorization: Bearer <token>` en todas las futuras peticiones.

## 4. Importancia en la Arquitectura
Dado que el **API Gateway** valida la firma del token de todos los *requests* entrantes, `ms-auth` es vital para el arranque del sistema. Si este servicio cae, nadie podrá iniciar sesión, aunque los tokens ya emitidos seguirán siendo validados por el Gateway de forma descentralizada.
