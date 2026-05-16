package com.madera.pedidos.dto;

import com.madera.pedidos.model.EstadoPedido;
import lombok.Data;

@Data
public class CambioEstadoRequest {
    private EstadoPedido nuevoEstado;
    private String responsable;
    private String motivo;
}
