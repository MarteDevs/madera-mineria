package com.madera.proveedores.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ContratoRequest {

    @NotBlank(message = "El tipo de madera es requerido")
    private String tipoMadera;

    @NotBlank(message = "La unidad de medida es requerida")
    private String unidad;

    @NotNull(message = "El precio pactado es requerido")
    @Min(value = 0, message = "El precio debe ser mayor o igual a 0")
    private Double precioPactado;

    private Integer cantidadMinima;
    private Integer cantidadMaxima;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;

    private String observaciones;
    private String estado; // VIGENTE, etc.
}
