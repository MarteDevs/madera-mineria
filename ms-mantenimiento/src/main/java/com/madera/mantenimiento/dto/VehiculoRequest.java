package com.madera.mantenimiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoRequest {

    @NotBlank(message = "La placa es obligatoria")
    private String placa;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    private String modelo;
    private Integer anio;
    private String tipo;
    private Double capacidadToneladasM3;
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Double kmActual;

    private LocalDate vencimientoSoat;
    private LocalDate vencimientoRevisionTecnica;
}
