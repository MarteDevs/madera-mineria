package com.madera.mantenimiento.dto;

import com.madera.mantenimiento.model.TipoMantenimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroMantenimientoRequest {

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    private TipoMantenimiento tipo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private String taller;
    private Double costo;
    private String tecnicoResponsable;
    private String observaciones;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;
}
