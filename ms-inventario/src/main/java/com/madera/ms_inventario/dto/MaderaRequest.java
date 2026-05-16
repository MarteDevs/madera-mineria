package com.madera.ms_inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaderaRequest {

    @NotBlank(message = "El tipo de madera es obligatorio")
    private String tipo;

    private String uso;
    private String unidad;

    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Double precioPorUnidad;

    @NotNull(message = "El stock disponible es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockDisponible;

    private Integer stockMinimo;

    @NotBlank(message = "La mina es obligatoria")
    private String mina;

    private String proveedor;
    private String estado;
}
