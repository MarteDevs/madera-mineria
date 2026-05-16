package com.madera.entrega.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarRecepcionRequest {

    @NotBlank(message = "El nombre del receptor es obligatorio")
    private String recibidoPor;

    private Boolean conformidad;
    private String observaciones;
}
