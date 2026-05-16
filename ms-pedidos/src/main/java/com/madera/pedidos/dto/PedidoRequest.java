package com.madera.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PedidoRequest {

    @NotNull(message = "El ID de la madera es obligatorio")
    private Long maderaId;

    @NotBlank(message = "El tipo de madera es obligatorio")
    private String tipoMadera;

    private String usoMadera;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidadSolicitada;

    private String unidad;

    @NotBlank(message = "La mina es obligatoria")
    private String mina;

    private String areaSolicitante;

    @NotBlank(message = "El solicitante es obligatorio")
    private String solicitadoPor;
}
