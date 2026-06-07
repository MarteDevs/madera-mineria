package com.madera.mantenimiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaMantenimientoResponse {
    private Long id;
    private Long vehiculoId;
    private String placaVehiculo;
    private String tipoAlerta;
    private String mensaje;
    private String prioridad;
    private Boolean resuelta;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaResolucion;
}
