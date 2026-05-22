package com.madera.entrega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String areaSolicitante;
    private String estado;
    private LocalDateTime fecha;
}
