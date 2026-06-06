package com.madera.proveedores.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntregaProveedorRequest {

    @NotNull(message = "El ID de madera es requerido")
    private Long maderaId;

    @NotBlank(message = "El tipo de madera es requerido")
    private String tipoMadera;

    @NotNull(message = "La cantidad entregada es requerida")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidadEntregada;

    @NotBlank(message = "La unidad es requerida")
    private String unidad;

    @NotBlank(message = "La guía de remisión es requerida")
    private String guiaRemision;

    @NotBlank(message = "El responsable de recepción es requerido")
    private String responsableRecepcion;
}
