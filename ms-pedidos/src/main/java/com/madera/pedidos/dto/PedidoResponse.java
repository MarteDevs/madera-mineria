package com.madera.pedidos.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PedidoResponse {
    private Long id;
    private String tipoMadera;
    private Integer cantidadSolicitada;
    private String mina;
    private String estado;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaEntregaEstimada;
}
