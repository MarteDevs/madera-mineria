package com.madera.entrega.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoAprobadoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String unidad;
    private String mina;
    private String solicitadoPor;
    private LocalDateTime fecha;
}
