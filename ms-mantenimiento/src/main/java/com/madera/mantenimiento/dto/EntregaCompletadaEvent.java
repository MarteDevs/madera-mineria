package com.madera.mantenimiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaCompletadaEvent {
    private Long entregaId;
    private Long pedidoId;
    private String placaVehiculo;          // Placa del camión que realizó la entrega
    private String conductorNombre;
    private Double distanciaKm;            // Kilómetros recorridos en el viaje
    private String minaDestino;
    private LocalDateTime fechaEntrega;
}
