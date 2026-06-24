INSERT INTO madera (tipo, uso, unidad, precio_por_unidad, stock_disponible, stock_minimo, mina, estado, ultima_actualizacion)
VALUES ('Pino', 'Cuadros', 'm3', 150.0, 500, 50, 'Cerro Verde', 'DISPONIBLE', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO madera (tipo, uso, unidad, precio_por_unidad, stock_disponible, stock_minimo, mina, estado, ultima_actualizacion)
VALUES ('Eucalipto', 'Soporte_Galeria', 'unidad', 85.5, 1200, 100, 'Antamina', 'DISPONIBLE', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
