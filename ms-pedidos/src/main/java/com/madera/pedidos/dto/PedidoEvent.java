package com.madera.pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEvent {
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String estado;              // "CREADO", "APROBADO", "RECHAZADO"
    private LocalDateTime fecha;
}
