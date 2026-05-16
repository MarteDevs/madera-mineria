package com.madera.entrega.listener;

import com.madera.entrega.config.RabbitMQConfig;
import com.madera.entrega.dto.PedidoAprobadoEvent;
import com.madera.entrega.service.EntregaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoAprobadoListener {

    private final EntregaService entregaService;

    @RabbitListener(queues = RabbitMQConfig.COLA_APROBADOS)
    public void procesarPedidoAprobado(PedidoAprobadoEvent evento) {
        log.info("Pedido aprobado recibido: #{} - {} unidades de {} para {}",
            evento.getPedidoId(),
            evento.getCantidad(),
            evento.getTipoMadera(),
            evento.getMina());

        entregaService.crearEntregaDesdeEvento(evento);
    }
}
