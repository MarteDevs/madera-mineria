package com.madera.mantenimiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMantenimientoResponse {
    private Long id;
    private String placaVehiculo;
    private String tipo;
    private String descripcion;
    private String taller;
    private Double kmAlServicio;
    private Double costo;
    private String tecnicoResponsable;
    private String observaciones;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;
    private LocalDateTime fechaRegistro;
}
