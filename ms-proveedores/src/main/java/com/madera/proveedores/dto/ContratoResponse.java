package com.madera.proveedores.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContratoResponse {
    private Long id;
    private String proveedorNombre;
    private String tipoMadera;
    private String unidad;
    private Double precioPactado;
    private Integer cantidadMinima;
    private Integer cantidadMaxima;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private String observaciones;
    private LocalDateTime fechaCreacion;
}
