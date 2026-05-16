package com.madera.notificaciones.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.madera.notificaciones.config.RabbitMQConfig;
import com.madera.notificaciones.dto.PedidoEvent;
import com.madera.notificaciones.service.NotificacionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoEventListener {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void procesarEvento(PedidoEvent evento) {
        log.info("Evento recibido desde RabbitMQ: Pedido #{} - {} - {}",
                evento.getPedidoId(),
                evento.getEstado(),
                evento.getMina());

        try {
            notificacionService.procesarEvento(evento);
            log.info("Notificación creada correctamente para pedido #{}", evento.getPedidoId());
        } catch (Exception exception) {
            log.error("Error procesando evento del pedido #{}: {}",
                    evento.getPedidoId(),
                    exception.getMessage(),
                    exception);
            throw exception;
        }
    }
}
