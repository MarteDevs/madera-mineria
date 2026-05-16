package com.madera.entrega.service;

import com.madera.entrega.dto.AsignarTransportistaRequest;
import com.madera.entrega.dto.ConfirmarRecepcionRequest;
import com.madera.entrega.dto.EntregaResponse;
import com.madera.entrega.dto.PedidoAprobadoEvent;
import com.madera.entrega.model.EstadoEntrega;

import java.util.List;

public interface EntregaService {

    void crearEntregaDesdeEvento(PedidoAprobadoEvent evento);

    EntregaResponse asignarTransportista(Long id, AsignarTransportistaRequest request);

    EntregaResponse marcarEnRuta(Long id);

    EntregaResponse confirmarRecepcion(Long id, ConfirmarRecepcionRequest request);

    EntregaResponse marcarFallido(Long id, String motivo);

    List<EntregaResponse> listarTodas();

    EntregaResponse obtenerPorId(Long id);

    EntregaResponse obtenerPorPedidoId(Long pedidoId);

    List<EntregaResponse> listarPorEstado(EstadoEntrega estado);

    List<EntregaResponse> listarPorMina(String mina);

    List<EntregaResponse> listarEnTransito();
}
