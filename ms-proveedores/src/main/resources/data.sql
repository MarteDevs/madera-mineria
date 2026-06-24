INSERT INTO proveedor (ruc, razon_social, nombre_comercial, contacto_nombre, contacto_email, contacto_telefono, direccion, ciudad, activo, fecha_registro, ultima_actualizacion)
VALUES ('20100200300', 'Maderas del Sur S.A.C.', 'MaderaSur', 'Juan Perez', 'juan@maderasur.pe', '987654321', 'Av. Industrial 123', 'Arequipa', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO proveedor (ruc, razon_social, nombre_comercial, contacto_nombre, contacto_email, contacto_telefono, direccion, ciudad, activo, fecha_registro, ultima_actualizacion)
VALUES ('20500600700', 'Forestal Andina S.A.', 'ForestAndina', 'Maria Gomez', 'ventas@forestandina.pe', '912345678', 'Av. Los Pinos 456', 'Lima', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
