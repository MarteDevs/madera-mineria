package com.madera.entrega.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarTransportistaRequest {

    @NotBlank(message = "El transportista es obligatorio")
    private String transportista;

    @NotBlank(message = "La placa del vehiculo es obligatoria")
    private String vehiculo;

    private String telefTransportista;
    private String almacenOrigen;
    private String direccionMina;
    private Double distanciaKm;
    private LocalDateTime fechaEntregaEstimada;
}
