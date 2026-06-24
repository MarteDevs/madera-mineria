INSERT INTO vehiculo (placa, marca, modelo, anio, tipo, capacidad_toneladas_m3, conductor_nombre, km_actual, km_ultimo_mantenimiento, km_proximo_mantenimiento, estado, requiere_mantenimiento, total_entregas_realizadas, fecha_registro, ultima_actualizacion)
VALUES ('ABC-123', 'Volvo', 'FMX', 2022, 'camion', 20.0, 'Carlos Ramirez', 15000.0, 10000.0, 20000.0, 'OPERATIVO', false, 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO vehiculo (placa, marca, modelo, anio, tipo, capacidad_toneladas_m3, conductor_nombre, km_actual, km_ultimo_mantenimiento, km_proximo_mantenimiento, estado, requiere_mantenimiento, total_entregas_realizadas, fecha_registro, ultima_actualizacion)
VALUES ('XYZ-987', 'Scania', 'G410', 2020, 'furgon', 15.0, 'Luis Torres', 49500.0, 40000.0, 50000.0, 'OPERATIVO', false, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
