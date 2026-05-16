package com.madera.notificaciones.service;

import java.util.List;

import com.madera.notificaciones.dto.NotificacionResponse;
import com.madera.notificaciones.dto.PedidoEvent;

public interface NotificacionService {

    void procesarEvento(PedidoEvent evento);

    NotificacionResponse marcarLeida(Long id);

    List<NotificacionResponse> listarTodas();

    List<NotificacionResponse> listarPendientes();

    List<NotificacionResponse> listarPorMina(String mina);

    List<NotificacionResponse> listarPorDestinatario(String destinatario);

    long contarPendientesAlmacen();
}
