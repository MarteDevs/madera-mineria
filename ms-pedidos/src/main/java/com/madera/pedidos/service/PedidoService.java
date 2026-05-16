package com.madera.pedidos.service;

import com.madera.pedidos.dto.PedidoRequest;
import com.madera.pedidos.dto.PedidoResponse;
import com.madera.pedidos.model.EstadoPedido;

import java.util.List;

public interface PedidoService {
    PedidoResponse crearPedido(PedidoRequest request);
    PedidoResponse aprobarPedido(Long id, String aprobadoPor);
    PedidoResponse rechazarPedido(Long id, String motivo);
    PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado);
    List<PedidoResponse> listarTodos();
    List<PedidoResponse> listarPorMina(String mina);
    List<PedidoResponse> listarPorEstado(EstadoPedido estado);
    PedidoResponse obtenerPorId(Long id);
}
