package com.madera.pedidos.dto;

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
    private String estado;
    private LocalDateTime fecha;

    public PedidoAprobadoEvent(Long pedidoId, String tipoMadera, Integer cantidad, String unidad, String mina, String solicitadoPor, LocalDateTime fecha) {
        this.pedidoId = pedidoId;
        this.tipoMadera = tipoMadera;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.mina = mina;
        this.solicitadoPor = solicitadoPor;
        this.estado = "APROBADO";
        this.fecha = fecha;
    }
}
